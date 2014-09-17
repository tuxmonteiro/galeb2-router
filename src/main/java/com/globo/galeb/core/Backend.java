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

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonObject;

import static com.globo.galeb.core.Constants.QUEUE_HEALTHCHECK_FAIL;

public class Backend extends Entity {

    public static String propertyKeepAliveFieldName           = "keepalive";
    public static String propertyConnectionTimeoutFieldName   = "connectionTimeout";
    public static String propertyKeepaliveMaxRequestFieldName = "keepaliveMaxRequest";
    public static String propertyKeepAliveTimeOutFieldName    = "keepAliveTimeOut";
    public static String propertyMaxPoolSizeFieldName         = "maxPoolSize";

    public static String propertyStatusFieldName              = "_status";
    public static String propertyActiveConnectionsFieldName   = "_activeConnections";

    private final Vertx vertx;
    private final EventBus eb;
    private final ConnectionsCounter connectionsCounter;
    private final String host;
    private final Integer port;

    private HttpClient client;

    public Long keepAliveMaxRequest  = null;
    public Long keepAliveTimeOut     = null;

    private Long keepAliveTimeMark;
    private Long requestCount;

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
        this.eb = (vertx!=null) ? vertx.eventBus() : null;
        this.client = null;
        this.id = json.getString(IJsonable.jsonIdFieldName, "127.0.0.1:80");

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

        boolean defaultKeepAlive           = true;
        int     defaultConnectionTimeout   = 60000; // 10 minutes
        Long    defaultKeepaliveMaxRequest = Long.MAX_VALUE-1;
        Long    defaultKeepAliveTimeOut    = 86400000L; // One day
        int     defaultMaxPoolSize         = 1;

        if (json.containsField(IJsonable.jsonPropertiesFieldName)) {
            JsonObject jsonProperties = json.getObject(jsonPropertiesFieldName);
            properties.putBoolean(propertyKeepAliveFieldName, jsonProperties.getBoolean(propertyKeepAliveFieldName, defaultKeepAlive));
            properties.putNumber(propertyConnectionTimeoutFieldName, jsonProperties.getInteger(propertyConnectionTimeoutFieldName, defaultConnectionTimeout));
            properties.putNumber(propertyKeepaliveMaxRequestFieldName, jsonProperties.getLong(propertyKeepaliveMaxRequestFieldName, defaultKeepaliveMaxRequest));
            properties.putNumber(propertyKeepAliveTimeOutFieldName, jsonProperties.getLong(propertyKeepAliveTimeOutFieldName, defaultKeepAliveTimeOut));
            properties.putNumber(propertyMaxPoolSizeFieldName, jsonProperties.getInteger(propertyMaxPoolSizeFieldName, defaultMaxPoolSize));
        } else {
            properties.putBoolean(propertyKeepAliveFieldName, defaultKeepAlive);
            properties.putNumber(propertyConnectionTimeoutFieldName, defaultConnectionTimeout);
            properties.putNumber(propertyKeepaliveMaxRequestFieldName, defaultKeepaliveMaxRequest);
            properties.putNumber(propertyKeepAliveTimeOutFieldName, defaultKeepAliveTimeOut);
            properties.putNumber(propertyMaxPoolSizeFieldName, defaultMaxPoolSize);
        }

        this.keepAliveTimeMark = System.currentTimeMillis();
        this.requestCount = 0L;
        this.connectionsCounter = new ConnectionsCounter(this.toString(), vertx);
    }

    private void updateModifiedTimestamp() {
        modifiedAt = System.currentTimeMillis();
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public Integer getConnectionTimeout() {
        return properties.getInteger(propertyConnectionTimeoutFieldName);
    }

    public Backend setConnectionTimeout(Integer timeout) {
        properties.putNumber(propertyConnectionTimeoutFieldName, timeout);
        updateModifiedTimestamp();
        return this;
    }

    public boolean isKeepalive() {
        return properties.getBoolean(propertyKeepAliveFieldName);
    }

    public Backend setKeepAlive(boolean keepalive) {
        properties.putBoolean(propertyKeepAliveFieldName, keepalive);
        updateModifiedTimestamp();
        return this;
    }

    public Long getKeepAliveMaxRequest() {
        if (keepAliveMaxRequest==null) {
            keepAliveMaxRequest = properties.getLong(propertyKeepaliveMaxRequestFieldName);
        }
        return keepAliveMaxRequest;
    }

    public Backend setKeepAliveMaxRequest(Long maxRequestCount) {
      properties.putNumber(propertyKeepaliveMaxRequestFieldName, maxRequestCount);
      updateModifiedTimestamp();
      return this;
    }

    public Long getKeepAliveTimeOut() {
        if (keepAliveTimeOut==null) {
            keepAliveTimeOut = properties.getLong(propertyKeepAliveTimeOutFieldName);
        }
        return keepAliveTimeOut;
    }

    public Backend setKeepAliveTimeOut(Long keepAliveTimeOut) {
        properties.putNumber(propertyKeepAliveTimeOutFieldName, keepAliveTimeOut);
        this.connectionsCounter.setConnectionMapTimeout(getKeepAliveTimeOut());
        updateModifiedTimestamp();
        return this;
    }

    public boolean isKeepAliveLimit() {
        Long keepAliveMaxRequest = getKeepAliveMaxRequest();
        Long keepAliveTimeOut = getKeepAliveTimeOut();
        Long now = System.currentTimeMillis();
        if (requestCount<=keepAliveMaxRequest) {
            requestCount++;
        }
        if ((requestCount>=keepAliveMaxRequest) || (requestCount==Long.MAX_VALUE) ||
                (now-keepAliveTimeMark)>keepAliveTimeOut) {
            keepAliveTimeMark = now;
            requestCount = 0L;
            return true;
        }
        return false;
    }

    public Integer getMaxPoolSize() {
        return properties.getInteger(propertyMaxPoolSizeFieldName);
    }

    public Backend setMaxPoolSize(Integer maxPoolSize) {
        properties.putNumber(propertyMaxPoolSizeFieldName, maxPoolSize);
        updateModifiedTimestamp();
        return this;
    }

    // Lazy initialization
    public HttpClient connect(String remoteIP, String remotePort) {
        if (client==null && vertx!=null) {
            client = vertx.createHttpClient()
                .setKeepAlive(isKeepalive())
                .setTCPKeepAlive(isKeepalive())
                .setConnectTimeout(getConnectionTimeout())
                .setMaxPoolSize(getMaxPoolSize());
            if (!"".equals(host) || port!=-1) {
                client.setHost(host)
                      .setPort(port);
            }
            client.exceptionHandler(new Handler<Throwable>() {
                @Override
                public void handle(Throwable e) {
                    eb.publish(QUEUE_HEALTHCHECK_FAIL, id);
                    connectionsCounter.initEventBus();
                }
            });
            connectionsCounter.registerEventBus();
        }
        connectionsCounter.addConnection(remoteIP, remotePort);
        return client;
    }

    public ConnectionsCounter getSessionController() {
        return connectionsCounter;
    }

    public void close() {
        if (client!=null) {
            try {
                client.close();
            } catch (IllegalStateException e) {
                // Already closed. Ignore exception.
            } finally {
                client=null;
                keepAliveMaxRequest  = null;
                keepAliveTimeOut     = null;
                connectionsCounter.unregisterEventBus();
            }
        }
        connectionsCounter.clearConnectionsMap();
    }

    public boolean isClosed() {
        updateModifiedTimestamp();
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

    @Override
    public JsonObject toJson() {
        prepareJson();
        idObj.putNumber(propertyActiveConnectionsFieldName, getSessionController().getActiveConnections());
        return super.toJson();
    }

}