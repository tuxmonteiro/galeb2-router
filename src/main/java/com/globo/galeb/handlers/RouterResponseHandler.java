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

import com.globo.galeb.core.Backend;
import com.globo.galeb.core.RemoteUser;
import com.globo.galeb.core.ServerResponse;
import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.exceptions.ServiceUnavailableException;
import com.globo.galeb.metrics.ICounter;
import com.globo.galeb.scheduler.IScheduler;
import com.globo.galeb.streams.Pump;

import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.logging.Logger;

/**
 * Class RouterResponseHandler.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class RouterResponseHandler implements Handler<HttpClientResponse> {

    /** the scheduler instance */
    private final IScheduler scheduler;

    /** The http server response. */
    private final HttpServerResponse httpServerResponse;

    /** The server response instance. */
    private final ServerResponse sResponse;

    /** The backend id. */
    private final Backend backend;

    /** The counter. */
    private final ICounter counter;

    /** The log. */
    private final Logger log;

    /** The queue service. */
    private final IQueueService queueService;

    /** The header host. */
    private String headerHost = "UNDEF";

    /** The initial request time. */
    private Long initialRequestTime = null;

    /** The connection keepalive. */
    private boolean connectionKeepalive = true;

    /** The remote user. */
    private  final RemoteUser remoteUser;

    /* (non-Javadoc)
     * @see org.vertx.java.core.Handler#handle(java.lang.Object)
     */
    @Override
    public void handle(final HttpClientResponse cResponse) throws RuntimeException {
        log.debug(String.format("Received response from backend %d %s", cResponse.statusCode(), cResponse.statusMessage()));

        scheduler.cancel();

        // Define statusCode and Headers
        final int statusCode = cResponse.statusCode();
        sResponse.setStatusCode(statusCode);
        sResponse.setHeaders(cResponse.headers());
        if (!connectionKeepalive) {
            httpServerResponse.headers().set("Connection", "close");
        }

        // Pump cResponse => sResponse
        final Pump pump = new Pump(cResponse, httpServerResponse);
        pump.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable throwable) {
                scheduler.cancel();
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


        cResponse.endHandler(new VoidHandler() {
            @Override
            public void handle() {
                String backendId = backend.toString();
                if (!"UNDEF".equals(headerHost) && initialRequestTime!=null) {
                    counter.requestTime(headerHost, backendId, initialRequestTime);
                }

                sResponse.setStatusCode(statusCode)
                    .setHeaderHost(headerHost)
                    .setBackendId(backendId)
                    .endResponse();

                backend.removeSession(remoteUser);

                if (!connectionKeepalive) {
                    sResponse.closeResponse();
                }
            }
        });

        cResponse.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                String backendId = backend.toString();

                log.error(String.format("host: %s , backend: %s , message: %s", headerHost, backendId, event.getMessage()));
                queueService.publishBackendFail(backendId);
                sResponse.setHeaderHost(headerHost).setBackendId(backendId)
                    .showErrorAndClose(event);

                backend.removeSession(remoteUser);
            }
        });

    }

    /**
     * Sets the header host.
     *
     * @param headerHost the header host
     * @return the router response handler
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
     * @return the router response handler
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
     * @return the router response handler
     */
    public RouterResponseHandler setConnectionKeepalive(boolean connectionKeepalive) {
        this.connectionKeepalive = connectionKeepalive;
        return this;
    }

    /**
     * Instantiates a new router response handler.
     *
     * @param vertx the vertx
     * @param log the log
     * @param requestTimeoutTimer the request timeout timer
     * @param httpServerResponse the http server response
     * @param sResponse the s response
     * @param backend the backend
     * @param remoteUser the remote user
     */
    public RouterResponseHandler(
            final IScheduler scheduler,
            final IQueueService queueService,
            final Logger log,
            final HttpServerResponse httpServerResponse,
            final ServerResponse sResponse,
            final Backend backend,
            final RemoteUser remoteUser) {
        this(scheduler, queueService, log, httpServerResponse, sResponse, backend, remoteUser, null);
    }

    /**
     * Instantiates a new router response handler.
     *
     * @param vertx the vertx
     * @param log the log
     * @param requestTimeoutTimer the request timeout timer
     * @param httpServerResponse the http server response
     * @param sResponse the s response
     * @param backend the backend
     * @param remoteUser the remote user
     * @param counter the counter
     */
    public RouterResponseHandler(
            final IScheduler scheduler,
            final IQueueService queueService,
            final Logger log,
            final HttpServerResponse httpServerResponse,
            final ServerResponse sResponse,
            final Backend backend,
            final RemoteUser remoteUser,
            final ICounter counter) {
        this.scheduler = scheduler;
        this.queueService = queueService;
        this.httpServerResponse = httpServerResponse;
        this.sResponse = sResponse;
        this.backend = backend;
        this.remoteUser = remoteUser;
        this.log = log;
        this.counter = counter;
    }

}
