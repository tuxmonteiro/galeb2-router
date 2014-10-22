/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
 * PARTICULAR PURPOSE.
 */
package com.globo.galeb.handlers;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.streams.Pump;

import com.globo.galeb.core.Backend;
import com.globo.galeb.core.Farm;
import com.globo.galeb.core.RemoteUser;
import com.globo.galeb.core.RequestData;
import com.globo.galeb.core.ServerResponse;
import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.exceptions.GatewayTimeoutException;
import com.globo.galeb.exceptions.NotFoundException;
import com.globo.galeb.exceptions.ServiceUnavailableException;
import com.globo.galeb.metrics.ICounter;

public class RouterRequestHandler implements Handler<HttpServerRequest> {

    private final Vertx vertx;
    private final Farm farm;
    private final ICounter counter;
    private final Logger log;

    private final String httpHeaderHost = HttpHeaders.HOST.toString();
    private final String httpHeaderConnection = HttpHeaders.CONNECTION.toString();
    private final IQueueService queueService;

    private RemoteUser lastRemoteUser = null;
    private String lastHeaderHost = "";
    private Backend lastBackend = null;

    private class GatewayTimeoutTaskHandler implements Handler<Long> {

        private final ServerResponse sResponse;
        private final String headerHost;
        private final String backendId;

        public GatewayTimeoutTaskHandler(final ServerResponse sResponse, String headerHost, String backendId) {
            this.sResponse = sResponse;
            this.headerHost = headerHost;
            this.backendId = backendId;
        }

        @Override
        public void handle(Long event) {
            sResponse.setHeaderHost(headerHost)
                .setHeaderHost(headerHost)
                .setBackendId(backendId)
                .showErrorAndClose(new GatewayTimeoutException());
        }

    }

    private class ClientRequestExceptionHandler implements Handler<Throwable> {

        private final ServerResponse sResponse;
        private final Long requestTimeoutTimer;
        private final String headerHost;
        private final String backendId;

        public ClientRequestExceptionHandler(final ServerResponse sResponse,
                Long requestTimeoutTimer, String headerHost, String backendId) {
            this.sResponse = sResponse;
            this.requestTimeoutTimer = requestTimeoutTimer;
            this.headerHost = headerHost;
            this.backendId = backendId;
        }

        @Override
        public void handle(Throwable event) {
            vertx.cancelTimer(requestTimeoutTimer);
            queueService.publishBackendFail(backendId.toString());
            sResponse.setHeaderHost(headerHost).setBackendId(backendId)
                .showErrorAndClose(event);
        }

    }

    @Override
    public void handle(final HttpServerRequest sRequest) throws RuntimeException {

        String headerHost = "UNDEF";
        String backendId = "UNDEF";

        if (sRequest.headers().contains(httpHeaderHost)) {
            headerHost = sRequest.headers().get(httpHeaderHost).split(":")[0];
        } else {
            log.warn("HTTP Header Host UNDEF");
            return;
        }

        log.debug(String.format("Received request for host %s '%s %s'",
                sRequest.headers().get(httpHeaderHost), sRequest.method(), sRequest.absoluteURI().toString()));

        final Virtualhost virtualhost = farm.getVirtualhost(headerHost);

        if (virtualhost==null) {
            log.warn("Host UNDEF");
            new ServerResponse(sRequest, log, counter, false).showErrorAndClose(new NotFoundException());
            return;
        }
        virtualhost.setQueue(queueService);
        Long requestTimeOut = virtualhost.getRequestTimeOut();
        Boolean enableChunked = virtualhost.isChunked();
        Boolean enableAccessLog = virtualhost.hasAccessLog();

        final ServerResponse sResponse = new ServerResponse(sRequest, log, counter, enableAccessLog);
        sRequest.response().setChunked(enableChunked);

        final Long requestTimeoutTimer = vertx.setTimer(requestTimeOut,
                                            new GatewayTimeoutTaskHandler(sResponse, headerHost, backendId));

        if (!virtualhost.hasBackends()) {
            vertx.cancelTimer(requestTimeoutTimer);
            log.warn(String.format("Host %s without backends", headerHost));
            sResponse.showErrorAndClose(new ServiceUnavailableException());
            return;
        }

        RemoteUser remoteUser = new RemoteUser(sRequest.remoteAddress());
        final HttpClient httpClient;
        final Backend backend;

        if (lastBackend!=null && remoteUser.equals(lastRemoteUser) && headerHost.equals(lastHeaderHost)) {

            backend = lastBackend;
            httpClient = backend.connect();
            backendId = backend.toString();

        } else {

            lastRemoteUser = remoteUser;
            lastHeaderHost = headerHost;

            backend = virtualhost.getChoice(new RequestData(sRequest));
            backendId = backend.toString();
            lastBackend = backend;

            backend.setRemoteUser(remoteUser);
            httpClient = backend.connect();
        }

        final boolean connectionKeepalive = isHttpKeepAlive(sRequest.headers(), sRequest.version());

        final Handler<HttpClientResponse> handlerHttpClientResponse =
                new RouterResponseHandler(vertx,
                                          log,
                                          requestTimeoutTimer,
                                          sRequest.response(),
                                          sResponse,
                                          backend,
                                          counter)
                        .setConnectionKeepalive(connectionKeepalive)
                        .setHeaderHost(headerHost)
                        .setInitialRequestTime(System.currentTimeMillis());

        final HttpClientRequest cRequest = httpClient!=null ?
                httpClient.request(sRequest.method(), sRequest.uri(), handlerHttpClientResponse) : null;

        if (cRequest==null) {
            sResponse.showErrorAndClose(new ServiceUnavailableException());
            return;
        }

        cRequest.setChunked(enableChunked);

        updateHeadersXFF(sRequest.headers(), remoteUser);

        cRequest.headers().set(sRequest.headers());
        cRequest.headers().set(httpHeaderConnection, "keep-alive");

        if (enableChunked) {
            // Pump sRequest => cRequest
            try {
                Pump.createPump(sRequest, cRequest).start();
            } catch (RuntimeException e) {
                log.debug(e);
            }
        } else {
            sRequest.bodyHandler(new Handler<Buffer>() {
                @Override
                public void handle(Buffer buffer) {
                    cRequest.headers().set("Content-Length", String.format("%d", buffer.length()));
                    cRequest.write(buffer);
                }
            });
        }

        cRequest.exceptionHandler(
                new ClientRequestExceptionHandler(sResponse, requestTimeoutTimer, headerHost, backendId));

        sRequest.endHandler(new VoidHandler() {
            @Override
            public void handle() {
                cRequest.end();
            }
         });
    }

    public RouterRequestHandler(
            final Vertx vertx,
            final Farm farm,
            final ICounter counter,
            final IQueueService queueService,
            final Logger log) {
        this.vertx = vertx;
        this.farm = farm;
        this.counter = counter;
        this.queueService = queueService;
        this.log = log;
    }

    private void updateHeadersXFF(final MultiMap headers, RemoteUser remoteUser) {

        final String httpHeaderXRealIp         = "X-Real-IP";
        final String httpHeaderXForwardedFor   = "X-Forwarded-For";
        final String httpHeaderforwardedFor    = "Forwarded-For";
        final String httpHeaderXForwardedHost  = "X-Forwarded-Host";
        final String httpHeaderXForwardedProto = "X-Forwarded-Proto";

        String remote = remoteUser.getRemoteIP();
        String headerHost = headers.get(httpHeaderHost).split(":")[0];

        if (!headers.contains(httpHeaderXRealIp)) {
            headers.set(httpHeaderXRealIp, remote);
        }

        String xff;
        if (headers.contains(httpHeaderXForwardedFor)) {
            xff = String.format("%s, %s", headers.get(httpHeaderXForwardedFor),remote);
            headers.remove(httpHeaderXForwardedFor);
        } else {
            xff = remote;
        }
        headers.set(httpHeaderXForwardedFor, xff);

        if (headers.contains(httpHeaderforwardedFor)) {
            xff = String.format("%s, %s" , headers.get(httpHeaderforwardedFor), remote);
            headers.remove(httpHeaderforwardedFor);
        } else {
            xff = remote;
        }
        headers.set(httpHeaderforwardedFor, xff);

        if (!headers.contains(httpHeaderXForwardedHost)) {
            headers.set(httpHeaderXForwardedHost, headerHost);
        }

        if (!headers.contains(httpHeaderXForwardedProto)) {
            headers.set(httpHeaderXForwardedProto, "http");
        }
    }

    public boolean isHttpKeepAlive(MultiMap headers, HttpVersion httpVersion) {
        return headers.contains(httpHeaderConnection) ?
                !"close".equalsIgnoreCase(headers.get(httpHeaderConnection)) :
                httpVersion.equals(HttpVersion.HTTP_1_1);
    }

}
