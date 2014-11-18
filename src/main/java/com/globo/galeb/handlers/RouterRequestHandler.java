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
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.logging.Logger;

import com.globo.galeb.core.BackendPool;
import com.globo.galeb.core.Farm;
import com.globo.galeb.core.GatewayTimeoutTaskHandler;
import com.globo.galeb.core.HttpCode;
import com.globo.galeb.core.IBackend;
import com.globo.galeb.core.RemoteUser;
import com.globo.galeb.core.RequestData;
import com.globo.galeb.core.ServerResponse;
import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.exceptions.NotFoundException;
import com.globo.galeb.exceptions.ServiceUnavailableException;
import com.globo.galeb.metrics.ICounter;
import com.globo.galeb.rules.IRuleReturn;
import com.globo.galeb.rules.Rule;
import com.globo.galeb.scheduler.IScheduler;
import com.globo.galeb.scheduler.impl.VertxDelayScheduler;
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

    /** The enable chuncked. */
    private Boolean enableChuncked = true;

    /* (non-Javadoc)
     * @see org.vertx.java.core.Handler#handle(java.lang.Object)
     */
    @Override
    public void handle(final HttpServerRequest sRequest) throws RuntimeException {

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
        sResponse.setChunked(enableChuncked);

        IBackend backend = getBackend(virtualhost, sRequest, sResponse);

        if (backend==null) {
            return;
        }
        String backendId = backend.toString();

        RemoteUser remoteUser = new RemoteUser(sRequest.remoteAddress());

        final boolean connectionKeepalive = isHttpKeepAlive(sRequest.headers(), sRequest.version());

        Long requestTimeout = 1L * ((Integer)(farm.getOrCreateProperty(Farm.REQUEST_TIMEOUT_FIELDNAME, 5000L)));
        final IScheduler schedulerTimeOut = startSchedulerTimeout(requestTimeout, sRequest, sResponse, backendId);

        final HttpClient httpClient = backend.connect(remoteUser);

        final RouterResponseHandler handlerHttpClientResponse = new RouterResponseHandler();
        handlerHttpClientResponse.setScheduler(schedulerTimeOut)
                                 .setQueueService(queueService)
                                 .setLog(log)
                                 .setHttpServerResponse(sRequest.response())
                                 .setsResponse(sResponse)
                                 .setBackend(backend)
                                 .setRemoteUser(remoteUser)
                                 .setCounter(counter)
                                 .setConnectionKeepalive(connectionKeepalive)
                                 .setHeaderHost(sRequest.headers().get(httpHeaderHost))
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

        pumpStream(sRequest, cRequest, sResponse, backendId, schedulerTimeOut);

    }

    private void pumpStream(final HttpServerRequest sRequest,
                            final HttpClientRequest cRequest,
                            final ServerResponse sResponse,
                            String backendId,
                            final IScheduler schedulerTimeOut) {

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

        cRequest.exceptionHandler(new ClientRequestExceptionHandler()
                                        .setsResponse(sResponse)
                                        .setHeaderHost(sRequest.headers().get(httpHeaderHost))
                                        .setBackendId(backendId)
                                        .setScheduler(schedulerTimeOut));

        sRequest.endHandler(new VoidHandler() {
            @Override
            public void handle() {
                log.debug("sRequest endHandler");
                cRequest.end();
            }
         });
    }

    private IScheduler startSchedulerTimeout(Long requestTimeout,
                                             final HttpServerRequest sRequest,
                                             final ServerResponse sResponse,
                                             String backendId) {

        IScheduler schedulerTimeOut = new VertxDelayScheduler(vertx);
        schedulerTimeOut.setPeriod(requestTimeout)
                        .setHandler(new GatewayTimeoutTaskHandler(sResponse,
                                                                  sRequest.headers().get(httpHeaderHost),
                                                                  backendId))
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

        return schedulerTimeOut;
    }

    private IBackend getBackend(final Virtualhost virtualhost,
                                final HttpServerRequest sRequest,
                                final ServerResponse sResponse) {

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
            return null;
        }

        BackendPool backendPool = null;
        if (ruleReturn instanceof BackendPool) {
            backendPool = (BackendPool)ruleReturn;
        } else {
            sResponse.showErrorAndClose(new ServiceUnavailableException());
            return null;
        }

        if (backendPool.getEntities().isEmpty()) {
            log.warn(String.format("Pool '%s' without backends", backendPool));
            sResponse.showErrorAndClose(new ServiceUnavailableException());
            return null;
        }

        IBackend backend = backendPool.getChoice(new RequestData(sRequest));

        log.debug(String.format("GetChoice >> Virtualhost: %s, Backend: %s", virtualhost, backend));

        return backend;
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
