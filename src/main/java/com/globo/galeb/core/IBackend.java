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

import org.vertx.java.core.http.HttpClient;

/**
 * Interface IBackend.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 18, 2014.
 */
public interface IBackend extends Comparable<IBackend> {

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
    public void close(String remoteUser);

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

}
