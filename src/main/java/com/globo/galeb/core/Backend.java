/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
 * PARTICULAR PURPOSE.
 */
package com.globo.galeb.core;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.metrics.ICounter;

public class Backend extends Entity {

    public static final String KEEPALIVE_FIELDNAME             = "keepalive";
    public static final String CONNECTION_TIMEOUT_FIELDNAME    = "connectionTimeout";
    public static final String KEEPALIVE_MAXREQUEST_FIELDNAME  = "keepaliveMaxRequest";
    public static final String KEEPALIVE_TIMEOUT_FIELDNAME     = "keepAliveTimeOut";
    public static final String MAXPOOL_SIZE_FIELDNAME          = "maxPoolSize";
    public static final String PIPELINING_FIELDNAME            = "pipelining";
    public static final String RECEIVED_BUFFER_SIZE_FIELDNAME  = "receiveBufferSize";
    public static final String SEND_BUFFER_SIZE_FIELDNAME      = "sendBufferSize";
    public static final String USE_POOLED_BUFFERS_FIELDNAME    = "usePooledBuffers";

    public static final String ELEGIBLE_FIELDNAME              = "_elegible";
    public static final String ACTIVE_CONNECTIONS_FIELDNAME    = "_activeConnections";

    private final Vertx vertx;
    private final String host;
    private final Integer port;

    private ICounter           counter            = null;
    private BackendSession     backendSession     = null;

    private String     virtualhostId = "";

    // defaults
    private Boolean defaultKeepAlive           = true;
    private Integer defaultConnectionTimeout   = 60000; // 10 minutes
    private Long    defaultKeepaliveMaxRequest = Long.MAX_VALUE-1;
    private Long    defaultKeepAliveTimeOut    = 86400000L; // One day
    private Integer defaultMaxPoolSize         = 1;
    private Boolean defaultUsePooledBuffers    = false;
    private Integer defaultSendBufferSize      = Constants.TCP_SEND_BUFFER_SIZE;
    private Integer defaultReceiveBufferSize   = Constants.TCP_RECEIVED_BUFFER_SIZE;
    private Boolean defaultPipelining          = false;

    private RemoteUser remoteUser              = null;

    @Override
    public String toString() {
        return id;
    }

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

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    public Backend(final String backendId, final Vertx vertx) {
        this(new JsonObject().putString(IJsonable.ID_FIELDNAME, backendId), vertx);
    }

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

    public Backend setQueueService(IQueueService queueService) {
        if (backendSession!=null) {
            backendSession.setQueueService(queueService);
        }
        return this;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public Backend setRemoteUser(RemoteUser remoteUser) {
        this.remoteUser = remoteUser;
        return this;
    }

    private void updateModifiedTimestamp() {
        modifiedAt = System.currentTimeMillis();
    }

    private Object getOrCreateJsonProperty(String fieldName, Object defaultData) {
        if (!properties.containsField(fieldName)) {
            properties.putValue(fieldName, defaultData);
            updateModifiedTimestamp();
        }
        return properties.getField(fieldName);
    }

    public Integer getConnectionTimeout() {
        return (Integer) getOrCreateJsonProperty(CONNECTION_TIMEOUT_FIELDNAME, defaultConnectionTimeout);
    }

    public Backend setConnectionTimeout(Integer timeout) {
        properties.putNumber(CONNECTION_TIMEOUT_FIELDNAME, timeout);
        updateModifiedTimestamp();
        return this;
    }

    public Boolean isKeepalive() {
        return (Boolean) getOrCreateJsonProperty(KEEPALIVE_FIELDNAME, defaultKeepAlive);

    }

    public Backend setKeepAlive(boolean keepalive) {
        properties.putBoolean(KEEPALIVE_FIELDNAME, keepalive);
        updateModifiedTimestamp();
        return this;
    }

    public Long getKeepAliveMaxRequest() {
        return (Long) getOrCreateJsonProperty(KEEPALIVE_MAXREQUEST_FIELDNAME, defaultKeepaliveMaxRequest);
    }

    public Backend setKeepAliveMaxRequest(Long maxRequestCount) {
      properties.putNumber(KEEPALIVE_MAXREQUEST_FIELDNAME, maxRequestCount);
      updateModifiedTimestamp();
      return this;
    }

    public Long getKeepAliveTimeOut() {
        return (Long) getOrCreateJsonProperty(KEEPALIVE_TIMEOUT_FIELDNAME, defaultKeepAliveTimeOut);
    }

    public Backend setKeepAliveTimeOut(Long keepAliveTimeOut) {
        properties.putNumber(KEEPALIVE_TIMEOUT_FIELDNAME, keepAliveTimeOut);
        updateModifiedTimestamp();
        return this;
    }

    public Integer getMaxPoolSize() {
        return (Integer) getOrCreateJsonProperty(MAXPOOL_SIZE_FIELDNAME, defaultMaxPoolSize);
    }

    public Backend setMaxPoolSize(Integer maxPoolSize) {
        properties.putNumber(MAXPOOL_SIZE_FIELDNAME, maxPoolSize);
        updateModifiedTimestamp();
        return this;
    }

    public Boolean isUsePooledBuffers() {
        return (Boolean) getOrCreateJsonProperty(USE_POOLED_BUFFERS_FIELDNAME, defaultUsePooledBuffers);
    }

    public Integer getSendBufferSize() {
        return (Integer) getOrCreateJsonProperty(SEND_BUFFER_SIZE_FIELDNAME, defaultSendBufferSize);
    }

    public Integer getReceiveBufferSize() {
        return (Integer) getOrCreateJsonProperty(RECEIVED_BUFFER_SIZE_FIELDNAME, defaultReceiveBufferSize);

    }

    public Boolean isPipelining() {
        return (Boolean) getOrCreateJsonProperty(PIPELINING_FIELDNAME, defaultPipelining);
    }

    public Backend setCounter(ICounter counter) {
        this.counter = counter;
        return this;
    }

    public HttpClient connect() {
        if (backendSession==null) {
            backendSession = new BackendSession(vertx, virtualhostId, id);
            backendSession.setMaxPoolSize(getMaxPoolSize());
            backendSession.setCounter(counter);
        }
        backendSession.setBackendProperties(new SafeJsonObject(properties));
        backendSession.setRemoteUser(remoteUser);
        return backendSession.connect();
    }

    public void close() {
        if (backendSession==null) {
            return;
        }
        backendSession.close();
        remoteUser = null;
        backendSession = null;
    }

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

    public boolean isClosed() {
        if (backendSession==null) {
            return true;
        }
        return backendSession.isClosed();
    }

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