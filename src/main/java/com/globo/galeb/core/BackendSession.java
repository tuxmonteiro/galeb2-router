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
import org.vertx.java.core.http.HttpClient;

import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.metrics.ICounter;

public class BackendSession {

    private final Vertx vertx;
    private final String serverHost;
    private final String backendId;

    private HttpClient         client             = null;
    private ConnectionsCounter connectionsCounter = null;
    private IQueueService      queueService       = null;

    private SafeJsonObject backendProperties = new SafeJsonObject();

    private ICounter   counter              = null;
//    private Long       keepAliveMaxRequest  = Long.MAX_VALUE;
//    private Long       keepAliveTimeOut     = Long.MAX_VALUE;

//    private Long       keepAliveTimeMark    = System.currentTimeMillis();;
//    private Long       requestCount         = 0L;
    private RemoteUser remoteUser           = null;
    private Boolean    keepAlive            = true;
    private String     counterKey           = null;
    private int        maxPoolSize          = 1;

    public BackendSession(final Vertx vertx, String serverHost, String backendId) {
        this.vertx = vertx;
        this.serverHost = serverHost;
        this.backendId = backendId;
    }

    public BackendSession setCounter(ICounter counter) {
        this.counter = counter;
        return this;
    }

    public BackendSession setBackendProperties(SafeJsonObject backendProperties) {
        this.backendProperties = backendProperties;
        return this;
    }


//    public boolean isKeepAliveLimit() {
//        boolean limitedExceed = false;
//        Long now = System.currentTimeMillis();
//        if (requestCount<=keepAliveMaxRequest) {
//            requestCount++;
//        }
//        if ((requestCount>=keepAliveMaxRequest) || (requestCount==Long.MAX_VALUE) ||
//                (now-keepAliveTimeMark)>keepAliveTimeOut) {
//            requestCount = 0L;
//            limitedExceed = true;
//        }
//        keepAliveTimeMark = now;
//
//        return limitedExceed;
//    }

    // Lazy initialization
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
//            connectionsCounter.setConnectionMapTimeout(keepAliveTimeOut);

            client = vertx.createHttpClient();
            if (keepAlive!=null) {
                client.setKeepAlive(keepAlive);
                client.setTCPKeepAlive(keepAlive);
                client.setMaxPoolSize(maxPoolSize);
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

        if (counter!=null && client!=null && getSessionController().isNewConnection()) {
            counter.sendActiveSessions(getCounterKey(serverHost, backendId),1L);
        }

        return client;
    }

    private void processProperties() {
//        keepAliveMaxRequest = backendProperties.getLong(Backend.propertyKeepaliveMaxRequestFieldName, Long.MAX_VALUE);
//        keepAliveTimeOut    = backendProperties.getLong(Backend.propertyKeepAliveTimeOutFieldName, Long.MAX_VALUE);
        keepAlive           = backendProperties.getBoolean(Backend.propertyKeepAliveFieldName, true);
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
//                keepAliveMaxRequest  = null;
//                keepAliveTimeOut     = null;
            }
        }
        if (connectionsCounter!=null) {
            connectionsCounter.unregisterConnectionsCounter();
            connectionsCounter.clearConnectionsMap();
            connectionsCounter = null;
        }
    }

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

    public void setQueueService(IQueueService queueService) {
        this.queueService = queueService;
    }

//    public void setKeepAliveMaxRequest(Long keepAliveMaxRequest) {
//        this.keepAliveMaxRequest = keepAliveMaxRequest;
//    }
//
//    public void setKeepAliveTimeOut(Long keepAliveTimeOut) {
//        this.keepAliveTimeOut = keepAliveTimeOut;
//    }

//    public boolean checkKeepAliveLimit() {
//        if (isKeepAliveLimit()) {
//            close();
//            return true;
//        }
//        return false;
//    }

    private String getCounterKey(String aVirtualhost, String aBackend) {
        if (counter==null) {
            return "UNDEF";
        }
        if (counterKey==null || "".equals(counterKey)) {
            String strDefault = "UNDEF";
            String result = String.format("%s.%s",
                    counter.cleanupString(aVirtualhost, strDefault),
                    counter.cleanupString(aBackend, strDefault));
            if (!"".equals(aVirtualhost) && !"".equals(aBackend)) {
                counterKey = result;
            }
            return result;
        } else {
            return counterKey;
        }
    }

    public void setRemoteUser(RemoteUser remoteUser) {
        this.remoteUser = remoteUser;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

}
