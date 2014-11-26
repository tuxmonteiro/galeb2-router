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

import com.globo.galeb.bus.IQueueService;
import com.globo.galeb.bus.NullQueueService;
import com.globo.galeb.entity.EntitiesMap;
import com.globo.galeb.entity.impl.backend.BackendSession;
import com.globo.galeb.entity.impl.backend.IBackend;
import com.globo.galeb.entity.impl.backend.NullBackend;
import com.globo.galeb.exceptions.ServiceUnavailableException;
import com.globo.galeb.logger.SafeLogger;
import com.globo.galeb.metrics.CounterConsoleOut;
import com.globo.galeb.metrics.ICounter;
import com.globo.galeb.request.RemoteUser;
import com.globo.galeb.scheduler.IScheduler;
import com.globo.galeb.scheduler.impl.NullScheduler;
import com.globo.galeb.server.ServerResponse;
import com.globo.galeb.streams.Pump;

import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerResponse;

/**
 * Class RouterResponseHandler.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class RouterResponseHandler implements Handler<HttpClientResponse> {

    /** the scheduler instance */
    private IScheduler scheduler = new NullScheduler();

    /** The http server response. */
    private HttpServerResponse httpServerResponse = null;

    /** The backend. */
    private IBackend backend = new NullBackend();

    /** The counter. */
    private ICounter counter = new CounterConsoleOut();

    /** The safelog. */
    private SafeLogger log   = null;

    /** The queue service. */
    private IQueueService queueService = new NullQueueService();

    /** The header host. */
    private String headerHost = "UNDEF";

    /** The initial request time. */
    private Long initialRequestTime = null;

    /** The connection keepalive. */
    private boolean connectionKeepalive = true;

    /** The remote user. */
    private RemoteUser remoteUser = new RemoteUser();

    /** The server response instance. */
    private ServerResponse sResponse = null;

    /**
     * Define logger if necessary.
     */
    private void defineLoggerIfNecessary() {
        if (log==null) {
            log = new SafeLogger();
        }
    }

    /* (non-Javadoc)
     * @see org.vertx.java.core.Handler#handle(java.lang.Object)
     */
    @Override
    public void handle(final HttpClientResponse cResponse) throws RuntimeException {
        defineLoggerIfNecessary();
        if (sResponse==null||httpServerResponse==null) {
            log.error("Response is NULL");
            return;
        }
        log.debug(String.format("Received response from backend %d %s", cResponse.statusCode(), cResponse.statusMessage()));

        scheduler.cancel();

        updateResponseHeadersAndStatus(cResponse.statusCode(), cResponse);

        pumpStream(cResponse);

    }

    /**
     * Adjust status and header response.
     *
     * @param statusCode the status code
     * @param httpClientResponse the http client response
     */
    private void updateResponseHeadersAndStatus(int statusCode, final HttpClientResponse httpClientResponse) {
        sResponse.setStatusCode(statusCode);
        sResponse.setHeaders(httpClientResponse.headers());

        if (!connectionKeepalive) {
            httpServerResponse.headers().set("Connection", "close");
        }

    }

    /**
     * Pump stream.
     *
     * @param httpClientResponse the http client response
     */
    private void pumpStream(final HttpClientResponse httpClientResponse) {

        final Pump pump = new Pump(httpClientResponse, httpServerResponse);

        pump.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable throwable) {
                defineLoggerIfNecessary();
                scheduler.cancel();
                log.error(String.format("FAIL: RouterResponse.pump with %s", throwable.getMessage()));
                sResponse.showErrorAndClose(new ServiceUnavailableException());
            }
        });

        pump.writeHandler(new Handler<Void>() {
            @Override
            public void handle(Void v) {
                scheduler.cancel();
                pump.writeHandler(null);
            }
        });
        pump.start();

        httpClientResponse.endHandler(new VoidHandler() {
            @Override
            public void handle() {

                String backendId = backend.toString();
                if (!"UNDEF".equals(headerHost) && initialRequestTime!=null) {
                    counter.requestTime(headerHost, backendId, initialRequestTime);
                }

                sResponse.setHeaderHost(headerHost)
                    .setBackendId(backendId)
                    .endResponse();

                if (!connectionKeepalive) {
                    sResponse.closeResponse();
                    backend.close(remoteUser.toString());
                }
                defineLoggerIfNecessary();
                log.debug(String.format("Completed backend response. %d bytes", pump.bytesPumped()));
            }
        });

        httpClientResponse.exceptionHandler(new Handler<Throwable>() {
            @SuppressWarnings("unchecked")
            @Override
            public void handle(Throwable event) {
                String backendId = backend.toString();

                defineLoggerIfNecessary();
                log.error(String.format("host: %s , backend: %s , message: %s", headerHost, backendId, event.getMessage()));
                queueService.publishBackendFail(backendId);
                sResponse.setHeaderHost(headerHost).setBackendId(backendId)
                    .showErrorAndClose(event);

                ((EntitiesMap<BackendSession>) backend).removeEntity(remoteUser.toString());
            }
        });
    }

    /**
     * Sets the header host.
     *
     * @param headerHost the header host
     * @return this
     */
    public RouterResponseHandler setHeaderHost(String headerHost) {
        this.headerHost = headerHost;
        return this;
    }

    /**
     * Sets the initial request time.
     *
     * @param initialRequestTime the initial request time
     * @return this
     */
    public RouterResponseHandler setInitialRequestTime(Long initialRequestTime) {
        this.initialRequestTime = initialRequestTime;
        return this;
    }

    /**
     * Sets the connection keepalive.
     *
     * @param connectionKeepalive the connection keepalive
     * @return this
     */
    public RouterResponseHandler setConnectionKeepalive(boolean connectionKeepalive) {
        this.connectionKeepalive = connectionKeepalive;
        return this;
    }

    /**
     * Sets the scheduler.
     *
     * @param scheduler the scheduler
     * @return this
     */
    public RouterResponseHandler setScheduler(final IScheduler scheduler) {
        this.scheduler = scheduler;
        return this;
    }

    /**
     * Sets the http server response.
     *
     * @param httpServerResponse the http server response
     * @return this
     */
    public RouterResponseHandler setHttpServerResponse(final HttpServerResponse httpServerResponse) {
        this.httpServerResponse = httpServerResponse;
        return this;
    }

    /**
     * Sets response.
     *
     * @param sResponse the s response
     * @return this
     */
    public RouterResponseHandler setsResponse(final ServerResponse sResponse) {
        this.sResponse = sResponse;
        return this;
    }

    /**
     * Sets the backend.
     *
     * @param backend the backend
     * @return this
     */
    public RouterResponseHandler setBackend(final IBackend backend) {
        this.backend = backend;
        return this;
    }

    /**
     * Sets the counter.
     *
     * @param counter the counter
     * @return this
     */
    public RouterResponseHandler setCounter(final ICounter counter) {
        this.counter = counter;
        return this;
    }

    /**
     * Sets the log.
     *
     * @param log the log
     * @return this
     */
    public RouterResponseHandler setLog(final SafeLogger alog) {
        this.log = alog;
        return this;
    }

    /**
     * Sets the queue service.
     *
     * @param queueService the queue service
     * @return this
     */
    public RouterResponseHandler setQueueService(final IQueueService queueService) {
        this.queueService = queueService;
        return this;
    }

    /**
     * Sets the remote user.
     *
     * @param remoteUser the remote user
     * @return this
     */
    public RouterResponseHandler setRemoteUser(final RemoteUser remoteUser) {
        this.remoteUser = remoteUser;
        return this;
    }

}
