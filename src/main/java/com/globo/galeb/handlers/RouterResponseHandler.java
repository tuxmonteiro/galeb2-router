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

import com.globo.galeb.core.Backend;
import com.globo.galeb.core.ServerResponse;
import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.metrics.ICounter;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.streams.Pump;

public class RouterResponseHandler implements Handler<HttpClientResponse> {

    private final Vertx vertx;
    private final Long requestTimeoutTimer;
    private final HttpServerResponse httpServerResponse;
    private final ServerResponse sResponse;
    private final Backend backend;
    private final ICounter counter;
    private final Logger log;

    private String headerHost = "UNDEF";
    private Long initialRequestTime = null;
    private boolean connectionKeepalive = true;
//    private boolean backendForceKeepAlive = true;

    @Override
    public void handle(final HttpClientResponse cResponse) {
        log.debug(String.format("Received response from backend %d %s", cResponse.statusCode(), cResponse.statusMessage()));

        vertx.cancelTimer(requestTimeoutTimer);

        // Define statusCode and Headers
        final int statusCode = cResponse.statusCode();
        sResponse.setStatusCode(statusCode);
        sResponse.setHeaders(cResponse.headers());
        if (!connectionKeepalive) {
            httpServerResponse.headers().set("Connection", "close");
        }

        // Pump cResponse => sResponse
        Pump.createPump(cResponse, httpServerResponse).start();

        cResponse.endHandler(new VoidHandler() {
            @Override
            public void handle() {

                if (headerHost!=null) {
                    if (initialRequestTime!=null) {
                        counter.requestTime(getKey(), initialRequestTime);
                    }
                }

                sResponse.setStatusCode(statusCode)
                    .setId(getKey())
                    .end();

                if (connectionKeepalive) {
                    if (backend.checkKeepAliveLimit()) {
                        sResponse.closeResponse();
                    }
                } else {
                    sResponse.closeResponse();
                }

//                else {
//                    if (!backendForceKeepAlive) {
//                        backend.close();
//                    }
//                    sResponse.closeResponse();
//                }
            }
        });

        cResponse.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                log.error(String.format("host+backend: %s, message: %s", getKey(), event.getMessage()));
                vertx.eventBus().publish(IQueueService.QUEUE_HEALTHCHECK_FAIL, backend.toString() );
                sResponse.setHeaderHost(getHeaderHost())
                    .setId(getKey())
                    .showErrorAndClose(event);
                backend.close();
            }
        });

    }

    public String getHeaderHost() {
        return headerHost;
    }

    public RouterResponseHandler setHeaderHost(String headerHost) {
        this.headerHost = headerHost;
        return this;
    }

    public Long getInitialRequestTime() {
        return initialRequestTime;
    }

    public RouterResponseHandler setInitialRequestTime(Long initialRequestTime) {
        this.initialRequestTime = initialRequestTime;
        return this;
    }

    public boolean isConnectionKeepalive() {
        return connectionKeepalive;
    }

    public RouterResponseHandler setConnectionKeepalive(boolean connectionKeepalive) {
        this.connectionKeepalive = connectionKeepalive;
        return this;
    }

//    public boolean isBackendForceKeepAlive() {
//        return backendForceKeepAlive;
//    }

//    public RouterResponseHandler setBackendForceKeepAlive(boolean backendForceKeepAlive) {
//        this.backendForceKeepAlive = backendForceKeepAlive;
//        return this;
//    }

    private String getKey() {
        return String.format("%s.%s",
                headerHost!=null?headerHost.replaceAll("[^\\w]", "_"):"UNDEF",
                backend!=null?backend.toString().replaceAll("[^\\w]", "_"):"UNDEF");
    }

    public RouterResponseHandler(
            final Vertx vertx,
            final Logger log,
            final Long requestTimeoutTimer,
            final HttpServerResponse httpServerResponse,
            final ServerResponse sResponse,
            final Backend backend) {
        this(vertx, log, requestTimeoutTimer, httpServerResponse, sResponse, backend, null);
    }

    public RouterResponseHandler(
            final Vertx vertx,
            final Logger log,
            final Long requestTimeoutTimer,
            final HttpServerResponse httpServerResponse,
            final ServerResponse sResponse,
            final Backend backend,
            final ICounter counter) {
        this.vertx = vertx;
        this.requestTimeoutTimer = requestTimeoutTimer;
        this.httpServerResponse = httpServerResponse;
        this.sResponse = sResponse;
        this.backend = backend;
        this.log = log;
        this.counter = counter;

        this.httpServerResponse.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                String message = String.format("[%s] %s", this, event.getMessage());
                log.error(message);
            }
        });

    }

}
