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
package com.globo.galeb.core;

import java.util.concurrent.atomic.AtomicBoolean;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.scheduler.IScheduler;
import com.globo.galeb.scheduler.ISchedulerHandler;
import com.globo.galeb.scheduler.impl.VertxPeriodicScheduler;

/**
 * Class BackendSession.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class BackendSession {

    /** The vertx. */
    private final Vertx vertx;

    /** The backend id. */
    private final String backendId;

    /** The http client instance. */
    private HttpClient         client             = null;

    /** The queue service. */
    private IQueueService      queueService       = null;

    /** The backend properties. */
    private JsonObject backendProperties    = new JsonObject();

    /** The keep alive. */
    private boolean    keepAlive            = true;

    /** The max pool size. */
    private int        maxPoolSize          = 1;

    /** The keep alive limit scheduler. */
    private IScheduler keepAliveLimitScheduler = null;

    /** The request count. */
    private long requestCount = 0L;

    /** The keep alive time mark. */
    private long keepAliveTimeMark = System.currentTimeMillis();

    /** The keep alive max request. */
    private long keepAliveMaxRequest = Long.MAX_VALUE;

    /** The keep alive time out. */
    private long keepAliveTimeOut = 86400000L; // One day

    /** The is locked. */
    private AtomicBoolean isLocked = new AtomicBoolean(false);

    /**
     * Class KeepAliveCheckLimitHandler.
     *
     * @author See AUTHORS file.
     * @version 1.0.0, Nov 4, 2014.
     */
    class KeepAliveCheckLimitHandler implements ISchedulerHandler {

        /** The backend session. */
        private BackendSession backendSession;

        /**
         * Instantiates a new keep alive check limit handler.
         *
         * @param backendSession the backend session
         */
        public KeepAliveCheckLimitHandler(final BackendSession backendSession) {
            this.backendSession = backendSession;
        }

        /* (non-Javadoc)
         * @see com.globo.galeb.scheduler.ISchedulerHandler#handle()
         */
        @Override
        public void handle() {
            if (isLocked.get()) {
                return;
            }
            isLocked.set(true);
            if (backendSession.isKeepAliveLimit() && !backendSession.isClosed()) {
                backendSession.close();
            }
            isLocked.compareAndSet(true, false);
        }
    }

    /**
     * Instantiates a new backend session.
     *
     * @param vertx the vertx
     * @param serverHost the server host
     * @param backendId the backend id
     */
    public BackendSession(final Vertx vertx, String backendId) {
        this.vertx = vertx;
        this.backendId = backendId;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        keepAliveLimitScheduler.cancel();
    }

    /**
     * Sets the keep alive max request.
     *
     * @param keepAliveMaxRequest the keep alive max request
     * @return the backend session
     */
    public BackendSession setKeepAliveMaxRequest(long keepAliveMaxRequest) {
        this.keepAliveMaxRequest = keepAliveMaxRequest;
        return this;
    }

    /**
     * Sets the keep alive time out.
     *
     * @param keepAliveTimeOut the keep alive time out
     * @return the backend session
     */
    public BackendSession setKeepAliveTimeOut(long keepAliveTimeOut) {
        this.keepAliveTimeOut = keepAliveTimeOut;
        return this;
    }

    /**
     * Checks if is keep alive limit.
     *
     * @return true, if is keep alive limit
     */
    public boolean isKeepAliveLimit() {
        Long now = System.currentTimeMillis();
        if (requestCount<keepAliveMaxRequest) {
            requestCount++;
        }
        if ((requestCount==Long.MAX_VALUE) || (requestCount>=keepAliveMaxRequest) ||
                (now-keepAliveTimeMark)>keepAliveTimeOut) {
            keepAliveTimeMark = now;
            requestCount = 0L;
            return true;
        }
        return false;
    }

    /**
     * Sets the backend properties.
     *
     * @param backendProperties the backend properties
     * @return the backend session
     */
    public BackendSession setBackendProperties(JsonObject backendProperties) {
        this.backendProperties = backendProperties;
        return this;
    }

    // Lazy initialization
    /**
     * Connect and gets http client instance.
     *
     * @return the http client
     */
    public HttpClient connect() {

        if (keepAlive && keepAliveLimitScheduler==null) {
            keepAliveLimitScheduler = new VertxPeriodicScheduler(vertx)
                                                .setHandler(new KeepAliveCheckLimitHandler(this))
                                                .setPeriod(1000L)
                                                .start();
        }

        setKeepAliveFromProperties();

        String[] hostWithPortArray = backendId!=null ? backendId.split(":") : null;
        String host = "";
        int port = 80;
        if (hostWithPortArray != null && hostWithPortArray.length>1) {
            host = hostWithPortArray[0];
            try {
                port = Integer.parseInt(hostWithPortArray[1]);
            } catch (NumberFormatException e) {
                port = 80;
            }
        } else {
            host = backendId;
            port = 80;
        }

        if (isKeepAliveLimit() && !isClosed()) {
            close();
        }

        if (client==null && vertx!=null) {
            client = vertx.createHttpClient();
            client.setKeepAlive(keepAlive);
            client.setTCPKeepAlive(keepAlive);
            client.setMaxPoolSize(maxPoolSize);

            if (!"".equals(host) && port!=-1 && !client.toString().startsWith("Mock")) {
                client.setHost(host)
                      .setPort(port);
            }
            client.exceptionHandler(new Handler<Throwable>() {
                @Override
                public void handle(Throwable e) {
                    if (queueService!=null) {
                        queueService.publishBackendFail(backendId);
                    }
                }
            });
        }

        return client;
    }

    /**
     * Sets keepalive attribute from properties.
     */
    private void setKeepAliveFromProperties() {
        keepAlive = backendProperties.getBoolean(Backend.KEEPALIVE_FIELDNAME, true);
    }

    /**
     * Close connection and destroy http client instance.
     */
    public void close() {

        if (keepAliveLimitScheduler!=null) {
            keepAliveLimitScheduler.cancel();
        }

        if (client!=null) {
            try {
                client.close();
            } catch (IllegalStateException ignore) {
                // Already closed. Ignore exception.
            } finally {
                client=null;
            }
        }
    }

    /**
     * Checks if is closed.
     *
     * @return true, if is closed
     */
    public boolean isClosed() {
        if (client==null) {
            return true;
        }
        boolean httpClientClosed = false;
        try {
            client.getReceiveBufferSize();
        } catch (IllegalStateException e) {
            httpClientClosed = true;
        }
        return httpClientClosed;
    }

    /**
     * Sets the queue service.
     *
     * @param queueService the new queue service
     * @return this
     *
     */
    public BackendSession setQueueService(IQueueService queueService) {
        this.queueService = queueService;
        return this;
    }

    /**
     * Sets the max pool size.
     *
     * @param maxPoolSize the new max pool size
     * @return this
     *
     */
    public BackendSession setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
        return this;
    }

}
