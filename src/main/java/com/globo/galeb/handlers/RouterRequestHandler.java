/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.globo.galeb.handlers;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.logging.Logger;

import com.globo.galeb.core.Backend;
import com.globo.galeb.core.BackendPool;
import com.globo.galeb.core.Farm;
import com.globo.galeb.core.HttpCode;
import com.globo.galeb.core.RemoteUser;
import com.globo.galeb.core.RequestData;
import com.globo.galeb.core.ServerResponse;
import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.exceptions.GatewayTimeoutException;
import com.globo.galeb.exceptions.NotFoundException;
import com.globo.galeb.exceptions.ServiceUnavailableException;
import com.globo.galeb.metrics.ICounter;
import com.globo.galeb.rules.IRuleReturn;
import com.globo.galeb.rules.Rule;
import com.globo.galeb.scheduler.IScheduler;
import com.globo.galeb.scheduler.ISchedulerHandler;
import com.globo.galeb.scheduler.impl.VertxDelayScheduler;
import com.globo.galeb.scheduler.impl.VertxPeriodicScheduler;
import com.globo.galeb.streams.Pump;

/**
 * Class RouterRequestHandler.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class RouterRequestHandler implements Handler<HttpServerRequest> {

    /** The vertx. */
    private final Vertx vertx;

    /** The farm. */
    private final Farm farm;

    /** The counter. */
    private final ICounter counter;

    /** The log. */
    private final Logger log;

    /** The http header host. */
    private final String httpHeaderHost = HttpHeaders.HOST.toString();

    /** The http header connection. */
    private final String httpHeaderConnection = HttpHeaders.CONNECTION.toString();

    /** The queueService instance. */
    private final IQueueService queueService;

    /** The scheduler clean up dead sessions. */
    private final IScheduler schedulerCleanUpDeadSessions;

    /** The enable chuncked. */
    private Boolean enableChuncked = true;

    /**
     * Class GatewayTimeoutTaskHandler.
     *
     * @author: See AUTHORS file.
     * @version: 1.0.0, Oct 23, 2014.
     */
    private class GatewayTimeoutTaskHandler implements ISchedulerHandler {

        /** The serverResponse. */
        private final ServerResponse sResponse;

        /** The header host of request. */
        private final String headerHost;

        /** The backend id. */
        private final String backendId;

        /**
         * Instantiates a new gateway timeout task handler.
         *
         * @param sResponse the serverResponse instance
         * @param headerHost the header host
         * @param backendId the backend id
         */
        public GatewayTimeoutTaskHandler(final ServerResponse sResponse, String headerHost, String backendId) {
            this.sResponse = sResponse;
            this.headerHost = headerHost;
            this.backendId = backendId;
        }

        /* (non-Javadoc)
         * @see org.vertx.java.core.Handler#handle(java.lang.Object)
         */
        @Override
        public void handle() {
            sResponse.setHeaderHost(headerHost).setBackendId(backendId)
                .showErrorAndClose(new GatewayTimeoutException());
        }

    }

    /**
     * Class ClientRequestExceptionHandler.
     *
     * @author: See AUTHORS file.
     * @version: 1.0.0, Oct 23, 2014.
     */
    private class ClientRequestExceptionHandler implements Handler<Throwable> {

        /** The serverResponse instance. */
        private final ServerResponse sResponse;

        /** the scheduler instance */
        private final IScheduler scheduler;

        /** The header host of request. */
        private final String headerHost;

        /** The backend id. */
        private final String backendId;

        /**
         * Instantiates a new client request exception handler.
         *
         * @param sResponse the serverResponse instance
         * @param requestTimeoutTimer the request timeout timer
         * @param headerHost the header host
         * @param backendId the backend id
         */
        public ClientRequestExceptionHandler(final ServerResponse sResponse,
                IScheduler scheduler, String headerHost, String backendId) {
            this.sResponse = sResponse;
            this.scheduler = scheduler;
            this.headerHost = headerHost;
            this.backendId = backendId;
        }

        /* (non-Javadoc)
         * @see org.vertx.java.core.Handler#handle(java.lang.Object)
         */
        @Override
        public void handle(Throwable event) {
            scheduler.cancel();
            queueService.publishBackendFail(backendId.toString());
            log.error(String.format("ClientRequestExceptionHandler: %s", event.getMessage()));
            sResponse.setHeaderHost(headerHost).setBackendId(backendId)
                .showErrorAndClose(event);
        }

    }

    /* (non-Javadoc)
     * @see org.vertx.java.core.Handler#handle(java.lang.Object)
     */
    @Override
    public void handle(final HttpServerRequest sRequest) throws RuntimeException {

        String headerHost = "UNDEF";
        String backendId = "UNDEF";

        Virtualhost virtualhost = farm.getCriterion().when(sRequest).thenGetResult();

        if (virtualhost==null) {
            new ServerResponse(sRequest, log, counter, false).showErrorAndClose(new NotFoundException());
            return;
        }

        log.debug(String.format("Received request for host %s '%s %s'",
                sRequest.headers().get(httpHeaderHost), sRequest.method(), sRequest.absoluteURI().toString()));

        boolean enableAccessLog = (boolean) virtualhost.getOrCreateProperty(Virtualhost.ENABLE_ACCESSLOG_FIELDNAME, false);

        final ServerResponse sResponse = new ServerResponse(sRequest, log, counter, enableAccessLog);
        sRequest.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                log.error("HttpServerRequest fail");
                sResponse.showErrorAndClose(event);
            }
        });

        Rule ruleChosen = virtualhost.getCriterion().when(sRequest).thenGetResult();

        IRuleReturn ruleReturn = null;

        if (ruleChosen!=null) {
            ruleReturn = ruleChosen.getRuleReturn();
        } else {
            sResponse.showErrorAndClose(new ServiceUnavailableException());
        }

        if (ruleReturn instanceof HttpCode) {
            sResponse.setStatusCode(Integer.parseInt(ruleReturn.getReturnId()));
            sResponse.setMessage(((HttpCode)ruleReturn).getMessage());
            sResponse.endResponse();
            return;
        }

        BackendPool backendPool = null;
        if (ruleReturn instanceof BackendPool) {
            backendPool = (BackendPool)ruleReturn;
        } else {
            sResponse.showErrorAndClose(new ServiceUnavailableException());
            return;
        }

        if (backendPool.getEntities().isEmpty()) {
            log.warn(String.format("Pool '%s' without backends", backendPool));
            sResponse.showErrorAndClose(new ServiceUnavailableException());
            return;
        }

        sResponse.setChunked(enableChuncked);

        final Backend backend = backendPool.getChoice(new RequestData(sRequest));
//                                                .setKeepAlive(connectionKeepalive); // TODO?

        RemoteUser remoteUser = new RemoteUser(sRequest.remoteAddress());

        final boolean connectionKeepalive = isHttpKeepAlive(sRequest.headers(), sRequest.version());
//        requestTimeOut = (Long) farm.getOrCreateProperty(Farm.REQUEST_TIMEOUT_FIELDNAME, requestTimeOut);

        final IScheduler schedulerTimeOut = new VertxDelayScheduler(vertx)
                        .setPeriod((Long) backendPool.getOrCreateProperty(BackendPool.REQUEST_TIMEOUT_FIELDNAME, 10000L))
                        .setHandler(new GatewayTimeoutTaskHandler(sResponse, headerHost, backendId))
                        .cancelHandler(new Handler<Void>() {
                            @Override
                            public void handle(Void event) {
                                log.debug("scheduler canceled");
                            }
                        })
                        .cancelFailedHandler(new Handler<Void>() {
                            @Override
                            public void handle(Void event) {
                                log.debug("FAIL: scheduler NOT canceled");
                            }
                        })
                        .start();
        log.debug("Scheduler started");

        log.debug(String.format("GetChoice >> Virtualhost: %s, Backend: %s", virtualhost, backend));

        backendId = backend.toString();

        final HttpClient httpClient = backend.connect(remoteUser);

        final Handler<HttpClientResponse> handlerHttpClientResponse =
                new RouterResponseHandler(schedulerTimeOut,
                                          queueService,
                                          log,
                                          sRequest.response(),
                                          sResponse,
                                          backend,
                                          remoteUser,
                                          counter)
                        .setConnectionKeepalive(connectionKeepalive)
                        .setHeaderHost(headerHost)
                        .setInitialRequestTime(System.currentTimeMillis());

        final HttpClientRequest cRequest = httpClient!=null ?
                httpClient.request(sRequest.method(), sRequest.uri(), handlerHttpClientResponse) : null;

        if (cRequest==null) {
            schedulerTimeOut.cancel();
            log.error("FAIL: HttpClientRequest is null");
            sResponse.showErrorAndClose(new ServiceUnavailableException());
            return;
        }

        cRequest.setChunked(enableChuncked);

        updateHeadersXFF(sRequest.headers(), remoteUser);

        cRequest.headers().set(sRequest.headers());
        cRequest.headers().set(httpHeaderConnection, "keep-alive");

        // Pump sRequest => cRequest

        final Pump pump = new Pump(sRequest, cRequest);
        pump.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                schedulerTimeOut.cancel();
                log.error(String.format("FAIL: RouterRequest.pump with %s", event.getMessage()));
                sResponse.showErrorAndClose(new ServiceUnavailableException());
            }
        });
        pump.writeHandler(new Handler<Void>() {
            @Override
            public void handle(Void v) {
                schedulerTimeOut.cancel();
                pump.writeHandler(null);
            }
        });
        pump.start();
        log.debug(String.format("PUMP Virtualhost: %s, Backend: %s >> pump started", virtualhost, backend));

        cRequest.exceptionHandler(
                new ClientRequestExceptionHandler(sResponse, schedulerTimeOut, headerHost, backendId));

        sRequest.endHandler(new VoidHandler() {
            @Override
            public void handle() {
                log.debug("sRequest endHandler");
                cRequest.end();
            }
         });
    }

    /**
     * Instantiates a new router request handler.
     *
     * @param vertx the vertx
     * @param farm the farm
     * @param counter the counter
     * @param queueService the queue service
     * @param log the logger
     */
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
        this.schedulerCleanUpDeadSessions = new VertxPeriodicScheduler(vertx);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        schedulerCleanUpDeadSessions.cancel();
    }

    /**
     * Update headers xff.
     *
     * @param headers the headers
     * @param remoteUser the remote user
     */
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

    /**
     * Checks if is http keep alive.
     *
     * @param headers the headers
     * @param httpVersion the http version
     * @return true, if is http keep alive
     */
    public boolean isHttpKeepAlive(MultiMap headers, HttpVersion httpVersion) {
        return headers.contains(httpHeaderConnection) ?
                !"close".equalsIgnoreCase(headers.get(httpHeaderConnection)) :
                httpVersion.equals(HttpVersion.HTTP_1_1);
    }

}
