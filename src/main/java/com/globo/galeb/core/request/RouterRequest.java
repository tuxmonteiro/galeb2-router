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
package com.globo.galeb.core.request;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpVersion;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.core.bus.NullQueueService;
import com.globo.galeb.core.entity.impl.Farm;
import com.globo.galeb.core.entity.impl.backend.BackendPool;
import com.globo.galeb.core.entity.impl.backend.IBackend;
import com.globo.galeb.core.entity.impl.backend.NullBackend;
import com.globo.galeb.core.entity.impl.frontend.Rule;
import com.globo.galeb.core.entity.impl.frontend.Virtualhost;
import com.globo.galeb.core.rulereturn.HttpCode;
import com.globo.galeb.core.rulereturn.IRuleReturn;
import com.globo.galeb.core.server.ServerResponse;
import com.globo.galeb.exceptions.NotFoundException;
import com.globo.galeb.exceptions.ServiceUnavailableException;
import com.globo.galeb.handlers.ClientRequestExceptionHandler;
import com.globo.galeb.handlers.GatewayTimeoutTaskHandler;
import com.globo.galeb.handlers.RouterResponseHandler;
import com.globo.galeb.logger.SafeLogger;
import com.globo.galeb.metrics.CounterConsoleOut;
import com.globo.galeb.metrics.ICounter;
import com.globo.galeb.scheduler.IScheduler;
import com.globo.galeb.scheduler.impl.NullScheduler;
import com.globo.galeb.scheduler.impl.VertxDelayScheduler;
import com.globo.galeb.streams.Pump;

/**
 * Class RouterRequest.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 20, 2014.
 */
public class RouterRequest {

    /** The http header connection. */
    public static final String HTTP_HEADER_CONNECTION = HttpHeaders.CONNECTION.toString();

    /** The http header host. */
    public static final String HTTP_HEADER_HOST = HttpHeaders.HOST.toString();


    /** The http server request. */
    private final HttpServerRequest httpServerRequest;

    /** The httpClientRequest request. */
    private HttpClientRequest httpClientRequest = null;

    /** The serverResponse response. */
    private ServerResponse serverResponse = null;

    /** The schedulerTimeout. */
    private IScheduler schedulerTimeOut = new NullScheduler();

    /** The json conf. */
    private JsonObject conf = new JsonObject();

    /** The farm. */
    private Farm farm = null;

    /** The queue service. */
    private IQueueService queueService = new NullQueueService();

    /** The counter. */
    private ICounter counter = new CounterConsoleOut();

    /** The log. */
    private SafeLogger log = new SafeLogger();

    /** The plataform. */
    private Object plataform = null;

    /** The http headers. */
    private MultiMap headers = new CaseInsensitiveMultiMap();

    /** The enable chuncked. */
    private boolean enableChuncked = true;

    /** The enable access log. */
    private boolean enableAccessLog = false;

    /** The virtualhost. */
    private Virtualhost virtualhost = null;

    /** The remote user. */
    private RemoteUser remoteUser = new RemoteUser();

    /** The connection keepalive. */
    private boolean connectionKeepalive = true;

    /** The http version. */
    private HttpVersion httpVersion = HttpVersion.HTTP_1_1;

    /** The backend. */
    private IBackend backend = new NullBackend();

    /**
     * Instantiates a new router request.
     *
     * @param httpServerRequest the http server request
     */
    public RouterRequest(final HttpServerRequest httpServerRequest) {
        this.httpServerRequest = httpServerRequest;
    }

    /**
     * Sets the conf.
     *
     * @param conf the conf
     * @return this
     */
    public RouterRequest setConf(final JsonObject conf) {
        this.conf = conf;
        return this;
    }

    /**
     * Sets the farm.
     *
     * @param farm the farm
     * @return this
     */
    public RouterRequest setFarm(final Farm farm) {
        this.farm = farm;
        return this;
    }

    /**
     * Sets the queue service.
     *
     * @param queueService the queue service
     * @return this
     */
    public RouterRequest setQueueService(final IQueueService queueService) {
        this.queueService = queueService;
        return this;
    }

    /**
     * Sets the counter.
     *
     * @param counter the counter
     * @return this
     */
    public RouterRequest setCounter(final ICounter counter) {
        this.counter = counter;
        return this;
    }

    /**
     * Sets the log.
     *
     * @param log the log
     * @return this
     */
    public RouterRequest setLog(final Logger log) {
        this.log.setLogger(log);
        return this;
    }

    /**
     * Sets the plataform.
     *
     * @param plataform the plataform
     * @return this
     */
    public RouterRequest setPlataform(final Object plataform) {
        this.plataform = plataform;
        return this;
    }

    /**
     * Start.
     */
    public void start() {

        setUpHeadersAndVersion();

        remoteUser = new RemoteUser(httpServerRequest.remoteAddress());
        connectionKeepalive = isHttpKeepAlive();

        httpServerRequest.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                log.error("HttpServerRequest fail");
                serverResponse.showErrorAndClose(event);
            }
        });

        log.debug(String.format("Received request for host %s '%s %s'", headers.get(RouterRequest.HTTP_HEADER_HOST),
                                                                        httpServerRequest.method(),
                                                                        httpServerRequest.absoluteURI().toString()));

        virtualhost = farm.getCriterion().when(httpServerRequest).thenGetResult();
        serverResponse = new ServerResponse(httpServerRequest).setLog(log.getLogger());

        if (virtualhost==null) {
            serverResponse.showErrorAndClose(new NotFoundException());
            return;
        }

        enableChuncked = virtualhost.getProperties().getBoolean(Virtualhost.ENABLE_CHUNKED_FIELDNAME, true);
        enableAccessLog = virtualhost.getProperties().getBoolean(Virtualhost.ENABLE_ACCESSLOG_FIELDNAME, false);

        serverResponse.setLog(log.getLogger())
                      .setEnableAccessLog(enableAccessLog)
                      .setChunked(enableChuncked);

        choiceBackend();

        if (backend==null || backend instanceof NullBackend) {
            log.error("Backend is null");
            serverResponse.showErrorAndClose(new ServiceUnavailableException());
            return;
        }

        final HttpClient httpClient = backend.connect(remoteUser);

        if (httpClient==null) {
            log.error("HttpClient is null");
            serverResponse.showErrorAndClose(new ServiceUnavailableException());
            return;
        }

        schedulerTimeOut = startSchedulerTimeout(conf.getLong(Farm.REQUEST_TIMEOUT_FIELDNAME, 5000L));

        final RouterResponseHandler handlerHttpClientResponse = new RouterResponseHandler();
        handlerHttpClientResponse.setScheduler(schedulerTimeOut)
                                 .setQueueService(queueService)
                                 .setLog(log.getLogger())
                                 .setHttpServerResponse(httpServerRequest.response())
                                 .setsResponse(serverResponse)
                                 .setBackend(backend)
                                 .setRemoteUser(remoteUser)
                                 .setCounter(counter)
                                 .setConnectionKeepalive(connectionKeepalive)
                                 .setHeaderHost(httpServerRequest.headers().get(RouterRequest.HTTP_HEADER_HOST))
                                 .setInitialRequestTime(System.currentTimeMillis());

        httpClientRequest = httpClient.request(httpServerRequest.method(), httpServerRequest.uri(), handlerHttpClientResponse);

        if (httpClientRequest==null) {
            schedulerTimeOut.cancel();
            log.error("FAIL: HttpClientRequest is null");
            serverResponse.showErrorAndClose(new ServiceUnavailableException());
            return;
        }

        httpClientRequest.setChunked(enableChuncked);

        updateRequestHeaders();

        pumpStream();

    }

    public RouterRequest setUpHeadersAndVersion() {
        headers = httpServerRequest.headers();
        httpVersion = httpServerRequest.version();
        return this;
    }

    /**
     * Pump stream.
     */
    private void pumpStream() {

        final Pump pump = new Pump(httpServerRequest, httpClientRequest);

        pump.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                schedulerTimeOut.cancel();
                log.error(String.format("FAIL: RouterRequest.pump with %s", event.getMessage()));
                serverResponse.showErrorAndClose(new ServiceUnavailableException());
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

        httpClientRequest.exceptionHandler(new ClientRequestExceptionHandler()
                                        .setsResponse(serverResponse)
                                        .setHeaderHost(httpServerRequest.headers().get(RouterRequest.HTTP_HEADER_HOST))
                                        .setBackendId(backend.toString())
                                        .setScheduler(schedulerTimeOut));

        httpServerRequest.endHandler(new VoidHandler() {
            @Override
            public void handle() {
                log.debug("sRequest endHandler");
                httpClientRequest.end();
            }
         });
    }

    /**
     * Start scheduler timeout.
     *
     * @param requestTimeout the request timeout
     * @return the i scheduler
     */
    private IScheduler startSchedulerTimeout(Long requestTimeout) {

        IScheduler schedulerTimeOut = new VertxDelayScheduler((Vertx) plataform);
        schedulerTimeOut.setPeriod(requestTimeout)
                        .setHandler(new GatewayTimeoutTaskHandler(serverResponse,
                                                                  httpServerRequest.headers().get(RouterRequest.HTTP_HEADER_HOST),
                                                                  backend.toString()))
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

    /**
     * Choice backend.
     */
    private void choiceBackend() {

        Rule ruleChosen = virtualhost.getCriterion().when(httpServerRequest).thenGetResult();

        IRuleReturn ruleReturn = null;

        if (ruleChosen!=null) {
            ruleReturn = ruleChosen.getRuleReturn();
        } else {
            serverResponse.showErrorAndClose(new ServiceUnavailableException());
        }

        if (ruleReturn instanceof HttpCode) {
            serverResponse.setStatusCode(Integer.parseInt(ruleReturn.getReturnId()));
            serverResponse.setMessage(((HttpCode)ruleReturn).getMessage());
            serverResponse.endResponse();
            return;
        }

        BackendPool backendPool = null;
        if (ruleReturn instanceof BackendPool) {
            backendPool = (BackendPool)ruleReturn;
        } else {
            serverResponse.showErrorAndClose(new ServiceUnavailableException());
            return;
        }

        if (backendPool.getEntities().isEmpty()) {
            log.warn(String.format("Pool '%s' without backends", backendPool));
            serverResponse.showErrorAndClose(new ServiceUnavailableException());
            return;
        }

        backend = backendPool.getChoice(new RequestData(httpServerRequest));

        log.debug(String.format("GetChoice >> Virtualhost: %s, Backend: %s", virtualhost, backend));
    }

    /**
     * Update headers xff.
     */
    private void updateRequestHeaders() {

        final String httpHeaderXRealIp         = "X-Real-IP";
        final String httpHeaderXForwardedFor   = "X-Forwarded-For";
        final String httpHeaderforwardedFor    = "Forwarded-For";
        final String httpHeaderXForwardedHost  = "X-Forwarded-Host";
        final String httpHeaderXForwardedProto = "X-Forwarded-Proto";

        String remote = remoteUser.getRemoteIP();
        String headerHost = headers.get(RouterRequest.HTTP_HEADER_HOST).split(":")[0];

        httpClientRequest.headers().set(headers);
        httpClientRequest.headers().set(RouterRequest.HTTP_HEADER_CONNECTION, "keep-alive");

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
     * @return true, if is http keep alive
     */
    public boolean isHttpKeepAlive() {

        return headers.contains(RouterRequest.HTTP_HEADER_CONNECTION) ?
                !"close".equalsIgnoreCase(headers.get(RouterRequest.HTTP_HEADER_CONNECTION)) :
                httpVersion.equals(HttpVersion.HTTP_1_1);
    }

}
