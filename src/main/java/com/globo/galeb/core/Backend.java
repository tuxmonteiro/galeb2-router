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

import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.metrics.ICounter;

/**
 * Class Backend.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class Backend extends Entity {

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

    /** The Constant ELEGIBLE_FIELDNAME. */
    public static final String ELEGIBLE_FIELDNAME              = "_elegible";

    /** The Constant ACTIVE_CONNECTIONS_FIELDNAME. */
    public static final String ACTIVE_CONNECTIONS_FIELDNAME    = "_activeConnections";

    /** The vertx. */
    private final Vertx vertx;

    /** The host name or IP. */
    private final String host;

    /** The port. */
    private final Integer port;

    /** The backend session. */
    private BackendSession     backendSession     = null;

    /** The virtualhost id. */
    private String     virtualhostId = "";

    /** The default keep alive. */
    private Boolean defaultKeepAlive           = true;

    /** The default connection timeout. */
    private Integer defaultConnectionTimeout   = 60000; // 10 minutes

    /** The default keepalive max request. */
    private Long    defaultKeepaliveMaxRequest = Long.MAX_VALUE-1;

    /** The default keep alive time out. */
    private Long    defaultKeepAliveTimeOut    = 86400000L; // One day

    /** The default max pool size. */
    private Integer defaultMaxPoolSize         = 1;

    /** The default use pooled buffers. */
    private Boolean defaultUsePooledBuffers    = false;

    /** The default send buffer size. */
    private Integer defaultSendBufferSize      = Constants.TCP_SEND_BUFFER_SIZE;

    /** The default receive buffer size. */
    private Integer defaultReceiveBufferSize   = Constants.TCP_RECEIVED_BUFFER_SIZE;

    /** The default pipelining. */
    private Boolean defaultPipelining          = false;

    /** The remote user. */
    private RemoteUser remoteUser              = null;

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return id;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Backend other = (Backend) obj;
        if (id == null) {
            if (other.id != null) return false;
        } else {
            if (!id.equalsIgnoreCase(other.id)) return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    /**
     * Instantiates a new backend.
     *
     * @param backendId the backend id
     * @param vertx the vertx
     */
    public Backend(final String backendId, final Vertx vertx) {
        this(new JsonObject().putString(IJsonable.ID_FIELDNAME, backendId), vertx);
    }

    /**
     * Instantiates a new backend.
     *
     * @param json the json
     * @param vertx the vertx
     */
    public Backend(JsonObject json, final Vertx vertx) {
        super();
        this.vertx = vertx;
        this.id = json.getString(IJsonable.ID_FIELDNAME, "127.0.0.1:80");
        this.virtualhostId = json.getString(IJsonable.PARENT_ID_FIELDNAME, "");

        String[] hostWithPortArray = id!=null ? id.split(":") : null;
        if (hostWithPortArray != null && hostWithPortArray.length>1) {
            this.host = hostWithPortArray[0];
            int myPort;
            try {
                myPort = Integer.parseInt(hostWithPortArray[1]);
            } catch (NumberFormatException e) {
                myPort = 80;
            }
            this.port = myPort;
        } else {
            this.host = id;
            this.port = 80;
        }

        if (json.containsField(IJsonable.PROPERTIES_FIELDNAME)) {
            JsonObject jsonProperties = json.getObject(PROPERTIES_FIELDNAME, new SafeJsonObject());
            properties.mergeIn(jsonProperties);
        }
    }

    /**
     * Sets the queue service.
     *
     * @param queueService the queue service
     * @return the backend
     */
    public Backend setQueueService(IQueueService queueService) {
        if (backendSession!=null) {
            backendSession.setQueueService(queueService);
        }
        return this;
    }

    /**
     * Gets the host.
     *
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets the port.
     *
     * @return the port
     */
    public Integer getPort() {
        return port;
    }

    /**
     * Sets the remote user.
     *
     * @param remoteUser the remote user
     * @return the backend
     */
    public Backend setRemoteUser(RemoteUser remoteUser) {
        this.remoteUser = remoteUser;
        return this;
    }

    /**
     * Update modified timestamp.
     */
    private void updateModifiedTimestamp() {
        modifiedAt = System.currentTimeMillis();
    }

    /**
     * Gets the or create json property.
     *
     * @param fieldName the field name
     * @param defaultData the default data
     * @return the or create json property
     */
    private Object getOrCreateJsonProperty(String fieldName, Object defaultData) {
        if (!properties.containsField(fieldName)) {
            properties.putValue(fieldName, defaultData);
            updateModifiedTimestamp();
        }
        return properties.getField(fieldName);
    }

    /**
     * Gets the connection timeout.
     *
     * @return the connection timeout
     */
    public Integer getConnectionTimeout() {
        return (Integer) getOrCreateJsonProperty(CONNECTION_TIMEOUT_FIELDNAME, defaultConnectionTimeout);
    }

    /**
     * Sets the connection timeout.
     *
     * @param timeout the timeout
     * @return the backend
     */
    public Backend setConnectionTimeout(Integer timeout) {
        properties.putNumber(CONNECTION_TIMEOUT_FIELDNAME, timeout);
        updateModifiedTimestamp();
        return this;
    }

    /**
     * Checks if is keepalive.
     *
     * @return the boolean
     */
    public Boolean isKeepalive() {
        return (Boolean) getOrCreateJsonProperty(KEEPALIVE_FIELDNAME, defaultKeepAlive);

    }

    /**
     * Sets the keep alive.
     *
     * @param keepalive the keepalive
     * @return the backend
     */
    public Backend setKeepAlive(boolean keepalive) {
        properties.putBoolean(KEEPALIVE_FIELDNAME, keepalive);
        updateModifiedTimestamp();
        return this;
    }

    /**
     * Gets the keep alive max request.
     *
     * @return the keep alive max request
     */
    public Long getKeepAliveMaxRequest() {
        return (Long) getOrCreateJsonProperty(KEEPALIVE_MAXREQUEST_FIELDNAME, defaultKeepaliveMaxRequest);
    }

    /**
     * Sets the keep alive max request.
     *
     * @param maxRequestCount the max request count
     * @return the backend
     */
    public Backend setKeepAliveMaxRequest(Long maxRequestCount) {
      properties.putNumber(KEEPALIVE_MAXREQUEST_FIELDNAME, maxRequestCount);
      updateModifiedTimestamp();
      return this;
    }

    /**
     * Gets the keep alive time out.
     *
     * @return the keep alive time out
     */
    public Long getKeepAliveTimeOut() {
        return (Long) getOrCreateJsonProperty(KEEPALIVE_TIMEOUT_FIELDNAME, defaultKeepAliveTimeOut);
    }

    /**
     * Sets the keep alive time out.
     *
     * @param keepAliveTimeOut the keep alive time out
     * @return the backend
     */
    public Backend setKeepAliveTimeOut(Long keepAliveTimeOut) {
        properties.putNumber(KEEPALIVE_TIMEOUT_FIELDNAME, keepAliveTimeOut);
        updateModifiedTimestamp();
        return this;
    }

    /**
     * Gets the max pool size.
     *
     * @return the max pool size
     */
    public Integer getMaxPoolSize() {
        return (Integer) getOrCreateJsonProperty(MAXPOOL_SIZE_FIELDNAME, defaultMaxPoolSize);
    }

    /**
     * Sets the max pool size.
     *
     * @param maxPoolSize the max pool size
     * @return the backend
     */
    public Backend setMaxPoolSize(Integer maxPoolSize) {
        properties.putNumber(MAXPOOL_SIZE_FIELDNAME, maxPoolSize);
        updateModifiedTimestamp();
        return this;
    }

    /**
     * Checks if is use pooled buffers.
     *
     * @return the boolean
     */
    public Boolean isUsePooledBuffers() {
        return (Boolean) getOrCreateJsonProperty(USE_POOLED_BUFFERS_FIELDNAME, defaultUsePooledBuffers);
    }

    /**
     * Gets the send buffer size.
     *
     * @return the send buffer size
     */
    public Integer getSendBufferSize() {
        return (Integer) getOrCreateJsonProperty(SEND_BUFFER_SIZE_FIELDNAME, defaultSendBufferSize);
    }

    /**
     * Gets the receive buffer size.
     *
     * @return the receive buffer size
     */
    public Integer getReceiveBufferSize() {
        return (Integer) getOrCreateJsonProperty(RECEIVED_BUFFER_SIZE_FIELDNAME, defaultReceiveBufferSize);

    }

    /**
     * Checks if is pipelining.
     *
     * @return the boolean
     */
    public Boolean isPipelining() {
        return (Boolean) getOrCreateJsonProperty(PIPELINING_FIELDNAME, defaultPipelining);
    }

    /**
     * Sets the counter.
     *
     * @param counter the counter
     * @return the backend
     */
    public Backend setCounter(ICounter counter) {
        if (backendSession!=null) {
            backendSession.setCounter(counter);
        }
        return this;
    }

    /**
     * Connect and gets HttpClient instance (through BackendSession).
     *
     * @return the http client
     */
    public HttpClient connect() {
        if (backendSession==null) {
            backendSession = new BackendSession(vertx, virtualhostId, id);
            backendSession.setMaxPoolSize(getMaxPoolSize());
        }
        backendSession.setBackendProperties(new SafeJsonObject(properties));
        backendSession.setRemoteUser(remoteUser);
        return backendSession.connect();
    }

    /**
     * Close connection and destroy backendSession instance.
     */
    public void close() {
        if (backendSession==null) {
            return;
        }
        backendSession.close();
        remoteUser = null;
        backendSession = null;
    }

    /**
     * Gets the active connections.
     *
     * @return the active connections
     */
    public Integer getActiveConnections() {
        if (backendSession==null) {
            return 0;
        }
        ConnectionsCounter connectionsCounter = backendSession.getSessionController();
        if (connectionsCounter!=null) {
            return backendSession.getSessionController().getActiveConnections();
        } else {
            return 0;
        }
    }

    /**
     * Checks if is closed.
     *
     * @return true, if is closed
     */
    public boolean isClosed() {
        if (backendSession==null) {
            return true;
        }
        return backendSession.isClosed();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.Entity#toJson()
     */
    @Override
    public JsonObject toJson() {
        prepareJson();
        idObj.putString(Entity.PARENT_ID_FIELDNAME, virtualhostId);

        ConnectionsCounter connectionsCounter = null;
        if (backendSession!=null) {
            connectionsCounter = backendSession.getSessionController();
        }
        if (connectionsCounter!=null) {
            idObj.putNumber(ACTIVE_CONNECTIONS_FIELDNAME, connectionsCounter.getActiveConnections());
        }
        return super.toJson();
    }
}