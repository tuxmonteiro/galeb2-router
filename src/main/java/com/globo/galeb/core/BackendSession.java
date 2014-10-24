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

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;

import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.metrics.ICounter;

/**
 * Class BackendSession.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class BackendSession {

    /** The vertx. */
    private final Vertx vertx;

    /** The server host. */
    private final String serverHost;

    /** The backend id. */
    private final String backendId;

    /** The http client instance. */
    private HttpClient         client             = null;

    /** The connections counter. */
    private ConnectionsCounter connectionsCounter = null;

    /** The queue service. */
    private IQueueService      queueService       = null;

    /** The backend properties. */
    private SafeJsonObject backendProperties = new SafeJsonObject();

    /** The counter. */
    private ICounter   counter              = null;

    /** The remote user. */
    private RemoteUser remoteUser           = null;

    /** The keep alive. */
    private Boolean    keepAlive            = true;

    /** The max pool size. */
    private int        maxPoolSize          = 1;

    /**
     * Instantiates a new backend session.
     *
     * @param vertx the vertx
     * @param serverHost the server host
     * @param backendId the backend id
     */
    public BackendSession(final Vertx vertx, String serverHost, String backendId) {
        this.vertx = vertx;
        this.serverHost = serverHost;
        this.backendId = backendId;
    }

    /**
     * Sets the counter.
     *
     * @param counter the counter
     * @return the backend session
     */
    public BackendSession setCounter(ICounter counter) {
        this.counter = counter;
        return this;
    }

    /**
     * Sets the backend properties.
     *
     * @param backendProperties the backend properties
     * @return the backend session
     */
    public BackendSession setBackendProperties(SafeJsonObject backendProperties) {
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

        processProperties();

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

        if (client==null) {
            connectionsCounter = new ConnectionsCounter(this.toString(), vertx, queueService);

            client = vertx.createHttpClient();
            if (keepAlive!=null) {
                client.setKeepAlive(keepAlive);
                client.setTCPKeepAlive(keepAlive);
                client.setMaxPoolSize(maxPoolSize);
                client.setTryUseCompression(true);
            }

            if (!"".equals(host) || port!=-1) {
                client.setHost(host)
                      .setPort(port);
            }
            client.exceptionHandler(new Handler<Throwable>() {
                @Override
                public void handle(Throwable e) {
                    if (queueService!=null) {
                        queueService.publishBackendFail(backendId);
                        connectionsCounter.publishZero();
                    }
                }
            });
            connectionsCounter.registerConnectionsCounter();
        }
        connectionsCounter.addConnection(remoteUser);

        if (counter!=null && client!=null && getSessionController().isNewConnection() &&
                !"".equals(serverHost) && !"UNDEF".equals(serverHost) &&
                !"".equals(backendId) && !"UNDEF".equals(backendId)) {
            counter.sendActiveSessions(serverHost, backendId, 1L);
        }

        return client;
    }

    /**
     * Process properties.
     */
    private void processProperties() {
        keepAlive           = backendProperties.getBoolean(Backend.KEEPALIVE_FIELDNAME, true);
    }

    /**
     * Gets the session controller.
     *
     * @return the session controller
     */
    public ConnectionsCounter getSessionController() {
        return connectionsCounter;
    }

    /**
     * Close connection and destroy http client instance.
     */
    public void close() {
        if (connectionsCounter!=null) {
            connectionsCounter.unregisterConnectionsCounter();
            connectionsCounter.clearConnectionsMap();
            connectionsCounter = null;
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
     */
    public void setQueueService(IQueueService queueService) {
        this.queueService = queueService;
    }

    /**
     * Sets the remote user.
     *
     * @param remoteUser the new remote user
     */
    public void setRemoteUser(RemoteUser remoteUser) {
        this.remoteUser = remoteUser;
    }

    /**
     * Sets the max pool size.
     *
     * @param maxPoolSize the new max pool size
     */
    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

}
