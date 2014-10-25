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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.core.bus.ICallbackConnectionCounter;
import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.scheduler.IScheduler;
import com.globo.galeb.scheduler.ISchedulerHandler;
import com.globo.galeb.scheduler.impl.NullScheduler;
import com.globo.galeb.scheduler.impl.VertxPeriodicScheduler;

/**
 * Class ConnectionsCounter.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class ConnectionsCounter implements ICallbackConnectionCounter {

    /** The Constant NUM_CONNECTIONS_FIELDNAME. */
    public static final String NUM_CONNECTIONS_FIELDNAME  = "numConnections";

    /** The Constant UUID_FIELDNAME. */
    public static final String UUID_FIELDNAME             = "uuid";

    /** The queue service. */
    private final IQueueService queueService;

    /** the scheduler instance */
    private IScheduler scheduler = new NullScheduler();

    /** The scheduler delay (ms) */
    private Long schedulerDelay = 10000L;

    /** The connection map timeout. */
    private Long connectionMapTimeout = 60000L;

    /** The connections map < remoteWithPort, timestamp >. */
    private final Map<String, Long> connections = new HashMap<>();

    /** The global connections map < backendInstanceUUID, numConnections >. */
    private final Map<String, Integer> globalConnections = new HashMap<>();

    /** The queue active connections. */
    private final String queueActiveConnections;

    /** The my uuid. */
    private final String myUUID;

    /** Is registered? */
    private boolean registered = false;

    /** Is new connection? */
    private boolean newConnection = true;

    /** The active connections. */
    private int activeConnections = 0;

    /**
     * Instantiates a new connections counter.
     *
     * @param backendWithPort the backend with port
     * @param vertx the vertx
     * @param queueService the queue service
     */
    public ConnectionsCounter(final String backendWithPort, final Vertx vertx, final IQueueService queueService) {
        if (vertx!=null) {
            this.scheduler = new VertxPeriodicScheduler(vertx);
        }
        this.queueService = queueService;
        this.queueActiveConnections = String.format("%s%s", IQueueService.QUEUE_BACKEND_CONNECTIONS_PREFIX, backendWithPort);
        this.myUUID = UUID.randomUUID().toString();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.ICallbackConnectionCounter#setRegistered(boolean)
     */
    @Override
    public void setRegistered(boolean registered) {
        this.registered = registered;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.ICallbackConnectionCounter#isRegistered()
     */
    @Override
    public boolean isRegistered() {
        return registered;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.ICallbackConnectionCounter#callbackGlobalConnectionsInfo(org.vertx.java.core.json.JsonObject)
     */
    @Override
    public void callbackGlobalConnectionsInfo(JsonObject message) {
        String uuid = message.getString(UUID_FIELDNAME);
        if (uuid != myUUID) {
            int numConnections = message.getInteger(NUM_CONNECTIONS_FIELDNAME);
            globalConnections.put(uuid, numConnections);
        }
    }

    /**
     * Adds the connection to map.
     *
     * @param remoteUser the remote user
     * @return true, if successful
     */
    public boolean addConnection(RemoteUser remoteUser) {
        if (remoteUser==null) {
            return false;
        }
        String remoteUserId = remoteUser.toString();
        newConnection = connections.put(remoteUserId, System.currentTimeMillis()) == null;
        activeScheduler();
        return newConnection;
    }

    /**
     * Adds the connection to map.
     *
     * @param host the host
     * @param port the port
     * @return true, if successful
     */
    public boolean addConnection(String host, Integer port) {
        RemoteUser remoteUser = new RemoteUser(host, port);
        return addConnection(remoteUser);
    }

    /**
     * Removes the connection from map.
     *
     * @param remoteUserId the remote user id
     * @return true, if successful
     */
    public boolean removeConnection(String remoteUserId) {
        return connections.remove(remoteUserId) != null;
    }

    /**
     * Clear connections map.
     */
    public void clearConnectionsMap() {
        connections.clear();
        globalConnections.clear();
        cancelScheduler();
    }

    /**
     * Gets the active connections.
     *
     * @return the active connections
     */
    public Integer getActiveConnections() {
        int counterActiveConnection = (activeConnections > 0) ? activeConnections : recalcNumConnections();
        return counterActiveConnection;
    }

    /**
     * Gets the active connections from instance only.
     *
     * @return the instance active connections
     */
    public Integer getInstanceActiveConnections() {
        return connections.size();
    }

    /**
     * Checks if is new connection.
     *
     * @return true, if is new connection
     */
    public boolean isNewConnection() {
        return newConnection;
    }

    /**
     * Gets the scheduler delay.
     *
     * @return the scheduler delay
     */
    public Long getSchedulerDelay() {
        return schedulerDelay;
    }

    /**
     * Sets the scheduler delay.
     *
     * @param schedulerDelay the scheduler delay
     * @return the connections counter
     */
    public ConnectionsCounter setSchedulerDelay(Long schedulerDelay) {
        if (!this.schedulerDelay.equals(schedulerDelay)) {
            this.schedulerDelay = schedulerDelay;
            cancelScheduler();
            activeScheduler();
        }
        return this;
    }

    /**
     * Expire local connections.
     */
    private void expireLocalConnections() {
        Long timeout = System.currentTimeMillis() - connectionMapTimeout;
        Set<String> connectionIds = new HashSet<>(connections.keySet());
        for (String remote : connectionIds) {
            if (connections.get(remote)<timeout) {
                removeConnection(remote);
            }
        }
    }

    /**
     * Clear global connections.
     */
    private void clearGlobalConnections() {
        globalConnections.clear();
    }

    /**
     * Notify num connections.
     */
    private void notifyNumConnections() {
        Integer localConnections = getInstanceActiveConnections();
        if (localConnections>0 && queueService!=null) {
            SafeJsonObject myConnections = new SafeJsonObject();
            myConnections.putString(UUID_FIELDNAME, myUUID);
            myConnections.putNumber(NUM_CONNECTIONS_FIELDNAME, localConnections);
            queueService.publishBackendConnections(queueActiveConnections, myConnections);
        }
    }

    /**
     * Recalc num connections.
     *
     * @return the int
     */
    private int recalcNumConnections() {
        int globalSum = getInstanceActiveConnections();
        for (int externalValue: globalConnections.values()) {
            globalSum =+ externalValue;
        }
        activeConnections = globalSum;
        return activeConnections;
    }

    /**
     * Active scheduler.
     */
    public void activeScheduler() {
        ISchedulerHandler taskPeriodic = new ISchedulerHandler() {
            @Override
            public void handle() {
                expireLocalConnections();
                recalcNumConnections();
                clearGlobalConnections();
                notifyNumConnections();
            }
        };
        scheduler.setHandler(taskPeriodic).setPeriod(schedulerDelay).start();
    }

    /**
     * Cancel scheduler.
     */
    public void cancelScheduler() {
        scheduler.cancel();
    }

    /**
     * Publish zero to all instances.
     */
    public void publishZero() {
        if (queueService!=null) {
            SafeJsonObject myConnections = new SafeJsonObject();
            myConnections.putString(UUID_FIELDNAME, myUUID);
            myConnections.putNumber(NUM_CONNECTIONS_FIELDNAME, 0);
            queueService.publishActiveConnections(queueActiveConnections, myConnections);
        }
    }

    /**
     * Register connections counter.
     */
    public void registerConnectionsCounter() {
        if (queueService!=null) {
            queueService.registerConnectionsCounter(this, queueActiveConnections);
        }
    }

    /**
     * Unregister connections counter.
     */
    public void unregisterConnectionsCounter() {
        if (queueService!=null) {
            publishZero();
            queueService.unregisterConnectionsCounter(this, queueActiveConnections);
        }
    }

}
