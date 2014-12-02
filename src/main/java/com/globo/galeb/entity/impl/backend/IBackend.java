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
package com.globo.galeb.entity.impl.backend;

import org.vertx.java.core.http.HttpClient;

import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.request.RemoteUser;

/**
 * Interface IBackend.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 18, 2014.
 */
public interface IBackend extends IJsonable {

    /** The Constant KEEPALIVE_FIELDNAME. */
    public static final String KEEPALIVE_FIELDNAME             = "keepalive";

    /** The Constant CONNECTION_TIMEOUT_FIELDNAME. */
    public static final String CONNECTION_TIMEOUT_FIELDNAME    = "connectionTimeout";

    /** The Constant KEEPALIVE_MAXREQUEST_FIELDNAME. */
    public static final String KEEPALIVE_MAXREQUEST_FIELDNAME  = "keepaliveMaxRequest";

    /** The Constant KEEPALIVE_TIMEOUT_FIELDNAME. */
    public static final String KEEPALIVE_TIMEOUT_FIELDNAME     = "keepAliveTimeOut";

    /** The Constant MAXPOOL_SIZE_FIELDNAME. */
    public static final String MAXPOOL_SIZE_FIELDNAME          = "maxPoolSize";

    /** The Constant PIPELINING_FIELDNAME. */
    public static final String PIPELINING_FIELDNAME            = "pipelining";

    /** The Constant RECEIVED_BUFFER_SIZE_FIELDNAME. */
    public static final String RECEIVED_BUFFER_SIZE_FIELDNAME  = "receiveBufferSize";

    /** The Constant SEND_BUFFER_SIZE_FIELDNAME. */
    public static final String SEND_BUFFER_SIZE_FIELDNAME      = "sendBufferSize";

    /** The Constant USE_POOLED_BUFFERS_FIELDNAME. */
    public static final String USE_POOLED_BUFFERS_FIELDNAME    = "usePooledBuffers";

    /** The Constant MIN_SESSION_POOL_SIZE_FIELDNAME. */
    public static final String MIN_SESSION_POOL_SIZE_FIELDNAME = "minSessionPoolSize";

    /** The Constant ACTIVE_CONNECTIONS_FIELDNAME. */
    public static final String ACTIVE_CONNECTIONS_FIELDNAME    = "_activeConnections";

    /** The Constant MAXCONN_FIELDNAME. */
    public static final String MAXCONN_FIELDNAME               = "maxConn";

    /** The Constant TCP_NODELAY - Vert.x defaults (org.vertx.java.core.net.impl.SocketDefaults). */
    public static final boolean TCP_NODELAY                    = true;

    /** The Constant TCP_SEND_BUFFER_SIZE - Vert.x defaults (org.vertx.java.core.net.impl.SocketDefaults). */
    public static final int     TCP_SEND_BUFFER_SIZE           = 8 * 1024;

    /** The Constant TCP_RECEIVED_BUFFER_SIZE - Vert.x defaults (org.vertx.java.core.net.impl.SocketDefaults). */
    public static final int     TCP_RECEIVED_BUFFER_SIZE       = 32 * 1024;

    /** The Constant DEFAULT_KEEPALIVE. */
    public static final boolean DEFAULT_KEEPALIVE              = true;

    /** The Constant DEFAULT_CONNECTION_TIMEOUT. */
    public static final int     DEFAULT_CONNECTION_TIMEOUT     = 60000; // 10 minutes

    /** The Constant DEFAULT_KEEPALIVE_MAXREQUEST. */
    public static final long    DEFAULT_KEEPALIVE_MAXREQUEST   = Long.MAX_VALUE-1;

    /** The Constant DEFAULT_KEEPALIVE_TIMEOUT. */
    public static final long    DEFAULT_KEEPALIVE_TIMEOUT      = 86400000L; // One day

    /** The Constant DEFAULT_MAX_POOL_SIZE. */
    public static final int     DEFAULT_MAX_POOL_SIZE          = 1;

    /** The Constant DEFAULT_USE_POOLED_BUFFERS. */
    public static final boolean DEFAULT_USE_POOLED_BUFFERS     = false;

    /** The Constant DEFAULT_SEND_BUFFER_SIZE. */
    public static final int     DEFAULT_SEND_BUFFER_SIZE       = TCP_SEND_BUFFER_SIZE;

    /** The Constant DEFAULT_RECEIVE_BUFFER_SIZE. */
    public static final int     DEFAULT_RECEIVE_BUFFER_SIZE    = TCP_RECEIVED_BUFFER_SIZE;

    /** The Constant DEFAULT_PIPELINING. */
    public static final boolean DEFAULT_PIPELINING             = false;

    /** The Constant DEFAULT_PIPELINING. */
    public static final int     DEFAULT_MIN_SESSION_POOL_SIZE  = 1;


    /**
     * Gets the host.
     *
     * @return the host
     */
    public String getHost();

    /**
     * Gets the port.
     *
     * @return the port
     */
    public Integer getPort();

    /**
     * Gets the connection timeout.
     *
     * @return the connection timeout
     */
    public Integer getConnectionTimeout();

    /**
     * Sets the connection timeout.
     *
     * @param timeout the timeout
     * @return the backend
     */
    public IBackend setConnectionTimeout(Integer timeout);

    /**
     * Checks if is keepalive.
     *
     * @return the boolean
     */
    public Boolean isKeepalive();

    /**
     * Sets the keep alive.
     *
     * @param keepalive the keepalive
     * @return the backend
     */
    public IBackend setKeepAlive(boolean keepalive);

    /**
     * Gets the keep alive max request.
     *
     * @return the keep alive max request
     */
    public Long getKeepAliveMaxRequest();

    /**
     * Sets the keep alive max request.
     *
     * @param maxRequestCount the max request count
     * @return the backend
     */
    public IBackend setKeepAliveMaxRequest(Long maxRequestCount);

    /**
     * Gets the keep alive time out.
     *
     * @return the keep alive time out
     */
    public Long getKeepAliveTimeOut();

    /**
     * Sets the keep alive time out.
     *
     * @param keepAliveTimeOut the keep alive time out
     * @return the backend
     */
    public IBackend setKeepAliveTimeOut(Long keepAliveTimeOut);

    /**
     * Gets the max pool size.
     *
     * @return the max pool size
     */
    public Integer getMaxPoolSize();

    /**
     * Sets the max pool size.
     *
     * @param maxPoolSize the max pool size
     * @return the backend
     */
    public IBackend setMaxPoolSize(Integer maxPoolSize);

    /**
     * Checks if is use pooled buffers.
     *
     * @return the boolean
     */
    public Boolean isUsePooledBuffers();

    /**
     * Gets the send buffer size.
     *
     * @return the send buffer size
     */
    public Integer getSendBufferSize();

    /**
     * Gets the receive buffer size.
     *
     * @return the receive buffer size
     */
    public Integer getReceiveBufferSize();

    /**
     * Checks if is pipelining.
     *
     * @return the boolean
     */
    public Boolean isPipelining();

    /**
     * Gets the min session pool size.
     *
     * @return the min session pool size
     */
    public int getMinSessionPoolSize();

    /**
     * Sets the min session pool size.
     *
     * @param minPoolSize the new min session pool size
     */
    public IBackend setMinSessionPoolSize(int minPoolSize);

    /**
     * Connect and gets HttpClient instance (through BackendSession).
     *
     * @return the http client
     */
    public HttpClient connect(RemoteUser remoteUser);

    /**
     * Close connection and destroy backendSession instance.
     */
    public void close(String remoteUser) throws RuntimeException;

    /**
     * Gets the active connections.
     *
     * @return the active connections
     */
    public int getActiveConnections();

    /**
     * Checks if is closed.
     *
     * @return true, if is closed
     */
    public boolean isClosed(String remoteUser);

    /**
     * Start session pool.
     *
     * @return the backend
     */
    public IBackend startSessionPool();

    /**
     * Close all forced.
     */
    public void closeAllForced();

    /**
     * Close all.
     */
    public void closeAll();

    /**
     * Sets the metric prefix.
     *
     * @param prefix the prefix
     * @return this
     */
    public IBackend setMetricPrefix(String prefix);

    /**
     * Sets the max conn.
     *
     * @param maxConn the max conn
     * @return this
     */
    public IBackend setMaxConn(int maxConn);


}
