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

public class Backend implements Serializable {

    public static String propertyKeepAliveFieldName           = "keepalive";
    public static String propertyConnectionTimeoutFieldName   = "connectionTimeout";
    public static String propertyKeepaliveMaxRequestFieldName = "keepaliveMaxRequest";
    public static String propertyKeepAliveTimeOutFieldName    = "keepAliveTimeOut";
    public static String propertyMaxPoolSizeFieldName         = "maxPoolSize";
    public static String propertyActiveConnectionsFieldName   = "activeConnections";

    private final Vertx vertx;
    private final EventBus eb;
    private final ConnectionsCounter connectionsCounter;
    private final String id;
    private final String host;
    private final Integer port;
    private final Long createdAt = System.currentTimeMillis();
    private Long modifiedAt = System.currentTimeMillis();

    private HttpClient client;
    private Integer connectionTimeout;
    private boolean keepalive;
    private Long keepAliveMaxRequest;
    private Long keepAliveTimeOut;
    private int backendMaxPoolSize;

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

    public Backend(final String hostWithPort, final Vertx vertx) {
        String[] hostWithPortArray = hostWithPort!=null ? hostWithPort.split(":") : null;
        this.vertx = vertx;
        this.eb = (vertx!=null) ? vertx.eventBus() : null;
        this.client = null;
        if (hostWithPortArray != null && hostWithPortArray.length>1) {
            this.host = hostWithPortArray[0];
            int myPort;
            try {
                myPort = Integer.parseInt(hostWithPortArray[1]);
            } catch (NumberFormatException e) {
                myPort = -1;
            }
            this.port = myPort;
        } else {
            this.host = hostWithPort;
            this.port = -1;
        }
        this.id = hostWithPort;
        this.connectionTimeout = 60000;
        this.keepalive = true;
        this.keepAliveMaxRequest = Long.MAX_VALUE-1;
        this.keepAliveTimeMark = System.currentTimeMillis();
        this.keepAliveTimeOut = 86400000L; // One day
        this.requestCount = 0L;
        this.connectionsCounter = new ConnectionsCounter(this.toString(), vertx);
    }

    public Backend(JsonObject json, final Vertx vertx) {
        this(json.getString(jsonIdFieldName, "127.0.0.1:0"), vertx);
        if (json.containsField(jsonPropertiesFieldName)) {
            JsonObject properties = json.getObject(jsonPropertiesFieldName);
            this.connectionTimeout = properties.getInteger(propertyConnectionTimeoutFieldName, connectionTimeout);
            this.keepalive = properties.getBoolean(propertyKeepAliveFieldName, keepalive);
            this.keepAliveMaxRequest = properties.getLong(propertyKeepaliveMaxRequestFieldName, keepAliveMaxRequest);
            this.keepAliveTimeOut = properties.getLong(propertyKeepAliveTimeOutFieldName, keepAliveTimeOut); // One day
        }
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
        return connectionTimeout;
    }

    public Backend setConnectionTimeout(Integer timeout) {
        this.connectionTimeout = timeout;
        updateModifiedTimestamp();
        return this;
    }

    public boolean isKeepalive() {
        return keepalive;
    }

    public Backend setKeepAlive(boolean keepalive) {
        this.keepalive = keepalive;
        updateModifiedTimestamp();
        return this;
    }

    public Long getKeepAliveMaxRequest() {
      return keepAliveMaxRequest;
    }

    public Backend setKeepAliveMaxRequest(Long maxRequestCount) {
      this.keepAliveMaxRequest = maxRequestCount;
      updateModifiedTimestamp();
      return this;
    }

    public Long getKeepAliveTimeOut() {
        return keepAliveTimeOut;
    }

    public Backend setKeepAliveTimeOut(Long keepAliveTimeOut) {
        this.keepAliveTimeOut = keepAliveTimeOut;
        this.connectionsCounter.setConnectionMapTimeout(getKeepAliveTimeOut());
        updateModifiedTimestamp();
        return this;
    }

    public boolean isKeepAliveLimit() {
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
        return backendMaxPoolSize;
    }

    public Backend setMaxPoolSize(Integer maxPoolSize) {
        this.backendMaxPoolSize = maxPoolSize;
        updateModifiedTimestamp();
        return this;
    }

    // Lazy initialization
    public HttpClient connect(String remoteIP, String remotePort) {
        final String backend = this.toString();
        if (client==null && vertx!=null) {
            client = vertx.createHttpClient()
                .setKeepAlive(keepalive)
                .setTCPKeepAlive(keepalive)
                .setConnectTimeout(connectionTimeout)
                .setMaxPoolSize(backendMaxPoolSize);
            if (!"".equals(host) || port!=-1) {
                client.setHost(host)
                      .setPort(port);
            }
            client.exceptionHandler(new Handler<Throwable>() {
                @Override
                public void handle(Throwable e) {
                    eb.publish(QUEUE_HEALTHCHECK_FAIL, backend);
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
        JsonObject backendJson = new JsonObject();
        backendJson.putString(jsonIdFieldName, id);
        backendJson.putNumber(jsonCreatedAtFieldName, createdAt);
        backendJson.putNumber(jsonModifiedAtFieldName, modifiedAt);

        JsonObject propertiesJson = new JsonObject();
        propertiesJson.putBoolean(propertyKeepAliveFieldName, keepalive);
        propertiesJson.putNumber(propertyConnectionTimeoutFieldName, connectionTimeout);
        propertiesJson.putNumber(propertyKeepaliveMaxRequestFieldName, keepAliveMaxRequest);
        propertiesJson.putNumber(propertyKeepAliveTimeOutFieldName, keepAliveTimeOut);
        propertiesJson.putNumber(propertyMaxPoolSizeFieldName, backendMaxPoolSize);
        propertiesJson.putNumber(propertyActiveConnectionsFieldName, getSessionController().getActiveConnections());

        backendJson.putObject(jsonPropertiesFieldName, propertiesJson);

        return backendJson;
    }

}