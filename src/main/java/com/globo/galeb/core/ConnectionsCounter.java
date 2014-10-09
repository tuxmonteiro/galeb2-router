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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.core.bus.ICallbackConnectionCounter;
import com.globo.galeb.core.bus.IQueueService;

public class ConnectionsCounter implements ICallbackConnectionCounter {

    public static final String numConnectionFieldName  = "numConnections";
    public static final String uuidFieldName           = "uuid";

    private final Vertx vertx;
    private final IQueueService queueService;

    private Long schedulerId = 0L;
    private Long schedulerDelay = 10000L;
    private Long connectionMapTimeout = 60000L;

    // < remoteWithPort, timestamp >
    private final Map<String, Long> connections = new HashMap<>();
    // < backendInstanceUUID, numConnections >
    private final Map<String, Integer> globalConnections = new HashMap<>();

    private final String queueActiveConnections;
    private final String myUUID;
    private boolean registered = false;

    private boolean newConnection = true;
    private int activeConnections = 0;

    public ConnectionsCounter(final String backendWithPort, final Vertx vertx, final IQueueService queueService) {
        this.vertx = vertx;
        this.queueService = queueService;
        this.queueActiveConnections = String.format("%s%s", IQueueService.QUEUE_BACKEND_CONNECTIONS_PREFIX, backendWithPort);
        this.myUUID = UUID.randomUUID().toString();
    }

    @Override
    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    @Override
    public boolean isRegistered() {
        return registered;
    }

    @Override
    public void callbackGlobalConnectionsInfo(JsonObject message) {
        String uuid = message.getString(uuidFieldName);
        if (uuid != myUUID) {
            int numConnections = message.getInteger(numConnectionFieldName);
            globalConnections.put(uuid, numConnections);
        }
    }

    public boolean addConnection(RemoteUser remoteUser) {
        if (remoteUser==null) {
            return false;
        }
        String remoteUserId = remoteUser.toString();
        newConnection = connections.put(remoteUserId, System.currentTimeMillis()) == null;
        activeScheduler();
        return newConnection;
    }

    public boolean addConnection(String host, Integer port) {
        RemoteUser remoteUser = new RemoteUser(host, port);
        return addConnection(remoteUser);
    }

    public boolean removeConnection(String remoteUserId) {
        return connections.remove(remoteUserId) != null;
    }

    public void clearConnectionsMap() {
        connections.clear();
        globalConnections.clear();
        cancelScheduler();
    }

    public Integer getActiveConnections() {
        int counterActiveConnection = (activeConnections > 0) ? activeConnections : recalcNumConnections();
        return counterActiveConnection;
    }

    public Integer getInstanceActiveConnections() {
        return connections.size();
    }

    public boolean isNewConnection() {
        return newConnection;
    }

    public Long getSchedulerDelay() {
        return schedulerDelay;
    }

    public ConnectionsCounter setSchedulerDelay(Long schedulerDelay) {
        if (!this.schedulerDelay.equals(schedulerDelay)) {
            this.schedulerDelay = schedulerDelay;
            cancelScheduler();
            activeScheduler();
        }
        return this;
    }

    public Long getConnectionMapTimeout() {
        return connectionMapTimeout;
    }

    public ConnectionsCounter setConnectionMapTimeout(Long connectionMapTimeout) {
        this.connectionMapTimeout = connectionMapTimeout;
        return this;
    }

    private void expireLocalConnections() {
        Long timeout = System.currentTimeMillis() - connectionMapTimeout;
        Set<String> connectionIds = new HashSet<>(connections.keySet());
        for (String remote : connectionIds) {
            if (connections.get(remote)<timeout) {
                removeConnection(remote);
            }
        }
    }

    private void clearGlobalConnections() {
        globalConnections.clear();
    }

    private void notifyNumConnections() {
        Integer localConnections = getInstanceActiveConnections();
        if (localConnections>0 && queueService!=null) {
            SafeJsonObject myConnections = new SafeJsonObject();
            myConnections.putString(uuidFieldName, myUUID);
            myConnections.putNumber(numConnectionFieldName, localConnections);
            queueService.publishBackendConnections(queueActiveConnections, myConnections);
        }
    }

    private int recalcNumConnections() {
        int globalSum = getInstanceActiveConnections();
        for (int externalValue: globalConnections.values()) {
            globalSum =+ externalValue;
        }
        activeConnections = globalSum;
        return activeConnections;
    }

    public void activeScheduler() {
        if (schedulerId==0L && vertx!=null) {
            schedulerId = vertx.setPeriodic(schedulerDelay, new Handler<Long>() {

                @Override
                public void handle(Long event) {
                    expireLocalConnections();
                    recalcNumConnections();
                    clearGlobalConnections();
                    notifyNumConnections();
                }
            });
        }
    }

    public void cancelScheduler() {
        if (schedulerId!=0L && vertx!=null) {
            boolean canceled = vertx.cancelTimer(schedulerId);
            if (canceled) {
                schedulerId=0L;
            }
        }
    }

    public void publishZero() {
        if (queueService!=null) {
            SafeJsonObject myConnections = new SafeJsonObject();
            myConnections.putString(uuidFieldName, myUUID);
            myConnections.putNumber(numConnectionFieldName, 0);
            queueService.publishActiveConnections(queueActiveConnections, myConnections);
        }
    }

    public void registerConnectionsCounter() {
        if (queueService!=null) {
            queueService.registerConnectionsCounter(this, queueActiveConnections);
        }
    }

    public void unregisterConnectionsCounter() {
        if (queueService!=null) {
            publishZero();
            queueService.unregisterConnectionsCounter(this, queueActiveConnections);
        }
    }

}
