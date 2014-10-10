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

    public static String propertyKeepAliveFieldName           = "keepalive";
    public static String propertyConnectionTimeoutFieldName   = "connectionTimeout";
    public static String propertyKeepaliveMaxRequestFieldName = "keepaliveMaxRequest";
    public static String propertyKeepAliveTimeOutFieldName    = "keepAliveTimeOut";
    public static String propertyMaxPoolSizeFieldName         = "maxPoolSize";
    public static String propertyPipeliningFieldName          = "pipelining";
    public static String propertyReceiveBufferSizeFieldName   = "receiveBufferSize";
    public static String propertySendBufferSizeFieldName      = "sendBufferSize";
    public static String propertyUsePooledBuffersFieldName    = "usePooledBuffers";

//
//    final Long keepAliveTimeOut = conf.getLong("keepAliveTimeOut", 60000L);
//    final Long keepAliveMaxRequest = conf.getLong("maxKeepAliveRequests", 10000L);
//    final Integer backendRequestTimeOut = conf.getInteger("backendRequestTimeOut", 60000);
//    final Integer backendConnectionTimeOut = conf.getInteger("backendConnectionTimeOut", 60000);
//    final Boolean backendForceKeepAlive = conf.getBoolean("backendForceKeepAlive", true);
//    final Integer backendMaxPoolSize = conf.getInteger("backendMaxPoolSize",10);
//    final Boolean backendPipeliging = conf.getBoolean("backendPipeliging");
//    final Integer backendReceiveBufferSize = conf.getInteger("backendReceiveBufferSize");
//    final Integer backendSendBufferSize = conf.getInteger("backendSendBufferSize");


//    if (isPipeliging()!=null) {
//        client.setPipelining(isPipeliging());
//    }
//    if (getReceiveBufferSize()!=null) {
//        client.setReceiveBufferSize(getReceiveBufferSize());
//    }
//    if (getSendBufferSize()!=null) {
//        client.setSendBufferSize(getSendBufferSize());
//    }
//    if (isUsePooledBuffers()!=null) {
//        client.setUsePooledBuffers(isUsePooledBuffers());
//    }

    public static String propertyElegibleFieldName            = "_elegible";
    public static String propertyActiveConnectionsFieldName   = "_activeConnections";

    private final Vertx vertx;
    private final String host;
    private final Integer port;

    private BackendSession     backendSession     = null;

    private String     virtualhostId = "";

    // defaults
    private Boolean defaultKeepAlive           = true;
    private Integer defaultConnectionTimeout   = 60000; // 10 minutes
    private Long    defaultKeepaliveMaxRequest = Long.MAX_VALUE-1;
    private Long    defaultKeepAliveTimeOut    = 86400000L; // One day
    private Integer defaultMaxPoolSize         = 1;
    private Boolean defaultUsePooledBuffers    = false;
    private Integer defaultSendBufferSize      = Constants.tcpSendBufferSize;
    private Integer defaultReceiveBufferSize   = Constants.tcpReceiveBufferSize;
    private Boolean defaultPipelining          = false;

    private RemoteUser remoteUser              = null;
    private int     maxPoolSize                = 1;

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
        this(new JsonObject().putString(IJsonable.jsonIdFieldName, backendId), vertx);
    }

    public Backend(JsonObject json, final Vertx vertx) {
        super();
        this.vertx = vertx;
        this.id = json.getString(IJsonable.jsonIdFieldName, "127.0.0.1:80");
        this.virtualhostId = json.getString(IJsonable.jsonParentIdFieldName, "");

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

        if (json.containsField(IJsonable.jsonPropertiesFieldName)) {
            JsonObject jsonProperties = json.getObject(jsonPropertiesFieldName, new SafeJsonObject());
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
        return (Integer) getOrCreateJsonProperty(propertyConnectionTimeoutFieldName, defaultConnectionTimeout);
    }

    public Backend setConnectionTimeout(Integer timeout) {
        properties.putNumber(propertyConnectionTimeoutFieldName, timeout);
        updateModifiedTimestamp();
        return this;
    }

    public Boolean isKeepalive() {
        return (Boolean) getOrCreateJsonProperty(propertyKeepAliveFieldName, defaultKeepAlive);

    }

    public Backend setKeepAlive(boolean keepalive) {
        properties.putBoolean(propertyKeepAliveFieldName, keepalive);
        updateModifiedTimestamp();
        return this;
    }

    public Long getKeepAliveMaxRequest() {
        return (Long) getOrCreateJsonProperty(propertyKeepaliveMaxRequestFieldName, defaultKeepaliveMaxRequest);
    }

    public Backend setKeepAliveMaxRequest(Long maxRequestCount) {
      properties.putNumber(propertyKeepaliveMaxRequestFieldName, maxRequestCount);
      updateModifiedTimestamp();
      return this;
    }

    public Long getKeepAliveTimeOut() {
        return (Long) getOrCreateJsonProperty(propertyKeepAliveTimeOutFieldName, defaultKeepAliveTimeOut);
    }

    public Backend setKeepAliveTimeOut(Long keepAliveTimeOut) {
        properties.putNumber(propertyKeepAliveTimeOutFieldName, keepAliveTimeOut);
        updateModifiedTimestamp();
        return this;
    }

    public Integer getMaxPoolSize() {
        return (Integer) getOrCreateJsonProperty(propertyMaxPoolSizeFieldName, defaultMaxPoolSize);
    }

    public Backend setMaxPoolSize(Integer maxPoolSize) {
        properties.putNumber(propertyMaxPoolSizeFieldName, maxPoolSize);
        updateModifiedTimestamp();
        return this;
    }

    public Boolean isUsePooledBuffers() {
        return (Boolean) getOrCreateJsonProperty(propertyUsePooledBuffersFieldName, defaultUsePooledBuffers);
    }

    public Integer getSendBufferSize() {
        return (Integer) getOrCreateJsonProperty(propertySendBufferSizeFieldName, defaultSendBufferSize);
    }

    public Integer getReceiveBufferSize() {
        return (Integer) getOrCreateJsonProperty(propertyReceiveBufferSizeFieldName, defaultReceiveBufferSize);

    }

    public Boolean isPipelining() {
        return (Boolean) getOrCreateJsonProperty(propertyPipeliningFieldName, defaultPipelining);
    }

    public Backend setCounter(ICounter counter) {
        if (backendSession!=null) {
            backendSession.setCounter(counter);
        }
        return this;
    }

    public HttpClient connect() {
        if (backendSession==null) {
            backendSession = new BackendSession(vertx, virtualhostId, id);
            backendSession.setMaxPoolSize(maxPoolSize);
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

    public boolean checkKeepAliveLimit() {
        if (backendSession==null) {
            return false;
        }
        return backendSession.checkKeepAliveLimit();
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

    public boolean isKeepAliveLimit() {
        if (backendSession==null) {
            return false;
        }
        return backendSession.isKeepAliveLimit();
    }

    public boolean isClosed() {
        if (backendSession==null) {
            return true;
        }
        return backendSession.isClosed();
    }

    @Override
    public JsonObject toJson() {
        properties.putNumber(propertyMaxPoolSizeFieldName, maxPoolSize);

        prepareJson();
        idObj.putString(Entity.jsonParentIdFieldName, virtualhostId);

        ConnectionsCounter connectionsCounter = null;
        if (backendSession!=null) {
            connectionsCounter = backendSession.getSessionController();
        }
        if (connectionsCounter!=null) {
            idObj.putNumber(propertyActiveConnectionsFieldName, connectionsCounter.getActiveConnections());
        }
        return super.toJson();
    }
}