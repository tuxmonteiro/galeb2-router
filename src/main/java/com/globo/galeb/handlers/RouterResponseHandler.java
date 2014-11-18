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

import com.globo.galeb.core.BackendSession;
import com.globo.galeb.core.IBackend;
import com.globo.galeb.core.NullBackend;
import com.globo.galeb.core.RemoteUser;
import com.globo.galeb.core.ServerResponse;
import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.core.bus.NullQueueService;
import com.globo.galeb.core.entity.EntitiesMap;
import com.globo.galeb.exceptions.ServiceUnavailableException;
import com.globo.galeb.logger.SafeLogger;
import com.globo.galeb.metrics.CounterConsoleOut;
import com.globo.galeb.metrics.ICounter;
import com.globo.galeb.scheduler.IScheduler;
import com.globo.galeb.scheduler.impl.NullScheduler;
import com.globo.galeb.streams.Pump;

import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

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
    private IBackend backend = new NullBackend(new JsonObject());

    /** The counter. */
    private ICounter counter = new CounterConsoleOut();

    /** The safelog. */
    private SafeLogger safelog = new SafeLogger();

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

    /* (non-Javadoc)
     * @see org.vertx.java.core.Handler#handle(java.lang.Object)
     */
    @Override
    public void handle(final HttpClientResponse cResponse) throws RuntimeException {
        if (sResponse==null||httpServerResponse==null) {
            getLog().error("Response is NULL");
            return;
        }
        getLog().debug(String.format("Received response from backend %d %s", cResponse.statusCode(), cResponse.statusMessage()));

        scheduler.cancel();

        adjustStatusAndHeaderResponse(cResponse.statusCode(), cResponse);

        pumpStream(cResponse);

    }

    private void adjustStatusAndHeaderResponse(int statusCode, final HttpClientResponse httpClientResponse) {
        sResponse.setStatusCode(statusCode);
        sResponse.setHeaders(httpClientResponse.headers());

        if (!connectionKeepalive) {
            httpServerResponse.headers().set("Connection", "close");
        }

    }

    private void pumpStream(final HttpClientResponse httpClientResponse) {

        final Pump pump = new Pump(httpClientResponse, httpServerResponse);

        pump.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable throwable) {
                scheduler.cancel();
                getLog().error(String.format("FAIL: RouterResponse.pump with %s", throwable.getMessage()));
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
                getLog().debug(String.format("Completed backend response. %d bytes", pump.bytesPumped()));
            }
        });

        httpClientResponse.exceptionHandler(new Handler<Throwable>() {
            @SuppressWarnings("unchecked")
            @Override
            public void handle(Throwable event) {
                String backendId = backend.toString();

                getLog().error(String.format("host: %s , backend: %s , message: %s", headerHost, backendId, event.getMessage()));
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
     * Gets the initial request time.
     *
     * @return the initial request time
     */
    public Long getInitialRequestTime() {
        return initialRequestTime;
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
     * Checks if is connection keepalive.
     *
     * @return true, if is connection keepalive
     */
    public boolean isConnectionKeepalive() {
        return connectionKeepalive;
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
     * Gets the scheduler.
     *
     * @return the scheduler
     */
    public IScheduler getScheduler() {
        return scheduler;
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
     * Gets the http server response.
     *
     * @return the http server response
     */
    public HttpServerResponse getHttpServerResponse() {
        return httpServerResponse;
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
     * Gets the s response.
     *
     * @return the s response
     */
    public ServerResponse getsResponse() {
        return sResponse;
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
     * Gets the backend.
     *
     * @return the backend
     */
    public IBackend getBackend() {
        return backend;
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
     * Gets the counter.
     *
     * @return the counter
     */
    public ICounter getCounter() {
        return counter;
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
     * Gets the log.
     *
     * @return the log
     */
    public Logger getLog() {
        return safelog.getLogger();
    }

    /**
     * Sets the log.
     *
     * @param log the log
     * @return this
     */
    public RouterResponseHandler setLog(final Logger log) {
        this.safelog.setLogger(log);
        return this;
    }

    /**
     * Gets the queue service.
     *
     * @return the queue service
     */
    public IQueueService getQueueService() {
        return queueService;
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
     * Gets the remote user.
     *
     * @return the remote user
     */
    public RemoteUser getRemoteUser() {
        return remoteUser;
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
