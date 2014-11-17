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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.core.bus.ICallbackConnectionCounter;
import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.core.entity.EntitiesMap;
import com.globo.galeb.core.entity.Entity;
import com.globo.galeb.scheduler.IScheduler;
import com.globo.galeb.scheduler.ISchedulerHandler;
import com.globo.galeb.scheduler.impl.VertxPeriodicScheduler;

/**
 * Class Backend.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class Backend extends EntitiesMap<BackendSession> implements ICallbackConnectionCounter {

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

    /** The Constant ELEGIBLE_FIELDNAME. */
    public static final String ELEGIBLE_FIELDNAME              = "_elegible";

    /** The Constant ACTIVE_CONNECTIONS_FIELDNAME. */
    public static final String ACTIVE_CONNECTIONS_FIELDNAME    = "_activeConnections";


    /** The Constant NUM_CONNECTIONS_INFO. */
    public static final String NUM_CONNECTIONS_INFO            = "numConnections";

    /** The Constant UUID_INFO_ID. */
    public static final String UUID_INFO_ID                    = "uuid";


    /** The Constant DEFAULT_KEEPALIVE. */
    public static final Boolean DEFAULT_KEEPALIVE             = true;

    /** The Constant DEFAULT_CONNECTION_TIMEOUT. */
    public static final Integer DEFAULT_CONNECTION_TIMEOUT    = 60000; // 10 minutes

    /** The Constant DEFAULT_KEEPALIVE_MAXREQUEST. */
    public static final Long    DEFAULT_KEEPALIVE_MAXREQUEST  = Long.MAX_VALUE-1;

    /** The Constant DEFAULT_KEEPALIVE_TIMEOUT. */
    public static final Long    DEFAULT_KEEPALIVE_TIMEOUT     = 86400000L; // One day

    /** The Constant DEFAULT_MAX_POOL_SIZE. */
    public static final Integer DEFAULT_MAX_POOL_SIZE         = 1;

    /** The Constant DEFAULT_USE_POOLED_BUFFERS. */
    public static final Boolean DEFAULT_USE_POOLED_BUFFERS    = false;

    /** The Constant DEFAULT_SEND_BUFFER_SIZE. */
    public static final Integer DEFAULT_SEND_BUFFER_SIZE      = Constants.TCP_SEND_BUFFER_SIZE;

    /** The Constant DEFAULT_RECEIVE_BUFFER_SIZE. */
    public static final Integer DEFAULT_RECEIVE_BUFFER_SIZE   = Constants.TCP_RECEIVED_BUFFER_SIZE;

    /** The Constant DEFAULT_PIPELINING. */
    public static final Boolean DEFAULT_PIPELINING            = false;


    /** The host name or IP. */
    private final String host;

    /** The port. */
    private final Integer port;

    /** The my uuid. */
    private final String myUUID;

    /** The queue active connections. */
    private final String queueActiveConnections;

    /** The min session pool size. */
    private int minSessionPoolSize = 1;

    /** The pool avaliable. */
    private final Set<BackendSession> poolAvaliable = new HashSet<>();

    /** The cleanup session scheduler. */
    private IScheduler cleanupSessionScheduler    = null;

    /** The is locked. */
    private java.util.concurrent.atomic.AtomicBoolean isLocked = new AtomicBoolean(false);

    /** The registered. */
    private boolean registered = false;

    /** The num external sessions. */
    private int numExternalSessions = 0;

    /**
     * Class CleanUpSessionHandler.
     *
     * @author See AUTHORS file.
     * @version 1.0.0, Nov 4, 2014.
     */
    class CleanUpSessionHandler implements ISchedulerHandler {

        /** The backend. */
        private final Backend backend;

        /**
         * Instantiates a new clean up session handler.
         *
         * @param backend the backend
         */
        public CleanUpSessionHandler(Backend backend) {
            this.backend = backend;
        }

        /* (non-Javadoc)
         * @see com.globo.galeb.scheduler.ISchedulerHandler#handle()
         */
        @Override
        public void handle() {

            if (!getEntities().isEmpty() && !backend.isLocked.get()) {
                backend.isLocked.set(true);
                Map<String, BackendSession> tmpSessions = new HashMap<>(getEntities());
                for (BackendSession backendSession : tmpSessions.values()) {
                    if (backendSession.isClosed()) {
                        removeEntity(backendSession);
                    }
                }
                backend.isLocked.set(false);
            }

            publishConnection(getEntities().size());
            numExternalSessions = 0;

        }
    }

    /**
     * Instantiates a new backend.
     *
     * @param json the json
     */
    public Backend(JsonObject json) {
        super(json);

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

        this.queueActiveConnections = String.format("%s%s", IQueueService.QUEUE_BACKEND_CONNECTIONS_PREFIX, this);
        this.myUUID = UUID.randomUUID().toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        cleanupSessionScheduler.cancel();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.entity.Entity#start()
     */
    @Override
    public void start() {
        registerConnectionsCounter();
        publishConnection(0);
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
     * Gets the connection timeout.
     *
     * @return the connection timeout
     */
    public Integer getConnectionTimeout() {
        return (Integer) getOrCreateProperty(CONNECTION_TIMEOUT_FIELDNAME, DEFAULT_CONNECTION_TIMEOUT);
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
        return (Boolean) getOrCreateProperty(KEEPALIVE_FIELDNAME, DEFAULT_KEEPALIVE);

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
        return (Long) getOrCreateProperty(KEEPALIVE_MAXREQUEST_FIELDNAME, DEFAULT_KEEPALIVE_MAXREQUEST);
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
        return (Long) getOrCreateProperty(KEEPALIVE_TIMEOUT_FIELDNAME, DEFAULT_KEEPALIVE_TIMEOUT);
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
        return (Integer) getOrCreateProperty(MAXPOOL_SIZE_FIELDNAME, DEFAULT_MAX_POOL_SIZE);
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
        return (Boolean) getOrCreateProperty(USE_POOLED_BUFFERS_FIELDNAME, DEFAULT_USE_POOLED_BUFFERS);
    }

    /**
     * Gets the send buffer size.
     *
     * @return the send buffer size
     */
    public Integer getSendBufferSize() {
        return (Integer) getOrCreateProperty(SEND_BUFFER_SIZE_FIELDNAME, DEFAULT_SEND_BUFFER_SIZE);
    }

    /**
     * Gets the receive buffer size.
     *
     * @return the receive buffer size
     */
    public Integer getReceiveBufferSize() {
        return (Integer) getOrCreateProperty(RECEIVED_BUFFER_SIZE_FIELDNAME, DEFAULT_RECEIVE_BUFFER_SIZE);

    }

    /**
     * Checks if is pipelining.
     *
     * @return the boolean
     */
    public Boolean isPipelining() {
        return (Boolean) getOrCreateProperty(PIPELINING_FIELDNAME, DEFAULT_PIPELINING);
    }

    /**
     * Gets the min session pool size.
     *
     * @return the min session pool size
     */
    public int getMinSessionPoolSize() {
        return minSessionPoolSize;
    }

    /**
     * Sets the min session pool size.
     *
     * @param minPoolSize the new min session pool size
     */
    public Backend setMinSessionPoolSize(int minPoolSize) {
        this.minSessionPoolSize = minPoolSize;
        return this;
    }

    /**
     * Connect and gets HttpClient instance (through BackendSession).
     *
     * @return the http client
     */
    public HttpClient connect(RemoteUser remoteUser) {
        if (remoteUser==null) {
            return null;
        }

        String remoteUserId = remoteUser.toString();

        final BackendSession backendSession;

        if (cleanupSessionScheduler==null && (Vertx) getPlataform()!=null) {
            cleanupSessionScheduler = new VertxPeriodicScheduler((Vertx) getPlataform())
                                            .setPeriod(10000L)
                                            .setHandler(new CleanUpSessionHandler(this))
                                            .start();
        }

        if (getEntityById(remoteUserId)!=null) {
            backendSession = getEntityById(remoteUserId);
        } else {

            if (!poolAvaliable.isEmpty()) {

                backendSession = poolAvaliable.iterator().next();
                backendSession.setRemoteUser(remoteUserId);
                poolAvaliable.remove(backendSession);
                addEntity(backendSession);

            } else {

                backendSession = new BackendSession(
                        new JsonObject().putString(ID_FIELDNAME, remoteUserId)
                                        .putString(PARENT_ID_FIELDNAME, id)
                                        .putObject(PROPERTIES_FIELDNAME, properties));

                backendSession.setPlataform(plataform)
                              .setQueueService(queueService)
                              .start();

                addEntity(backendSession);
            }


            String backendId = this.toString();
            if (!"".equals(parentId) && !"UNDEF".equals(parentId) &&
                    !"".equals(backendId) && !"UNDEF".equals(backendId)) {
                counter.sendActiveSessions(parentId, backendId, 1L);
            }

        }

        return backendSession.connect();
    }

    /**
     * Close connection and destroy backendSession instance.
     */
    public void close(String remoteUser) {
        if (!(remoteUser==null) && getEntityById(remoteUser)!=null) {
            BackendSession backendSession = getEntityById(remoteUser);
            removeEntity(remoteUser);

            if (poolAvaliable.size()>=minSessionPoolSize) {
                backendSession.close();
            } else {
                backendSession.setRemoteUser(UUID.randomUUID().toString());
                poolAvaliable.add(backendSession);
            }

        }
    }

    /**
     * Gets the active connections.
     *
     * @return the active connections
     */
    public int getActiveConnections() {
        return getEntities().size() + numExternalSessions;
    }

    /**
     * Checks if is closed.
     *
     * @return true, if is closed
     */
    public boolean isClosed(String remoteUser) {
        if (!(remoteUser==null) && getEntityById(remoteUser)!=null) {
            return getEntityById(remoteUser).isClosed();
        }
        return true;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.Entity#toJson()
     */
    @Override
    public JsonObject toJson() {
        prepareJson();
        idObj.putString(Entity.PARENT_ID_FIELDNAME, parentId);
        idObj.putNumber(ACTIVE_CONNECTIONS_FIELDNAME, getActiveConnections());

        return super.toJson();
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
            publishConnection(0);
            queueService.unregisterConnectionsCounter(this, queueActiveConnections);
        }
    }

    /**
     * Publish zero to all instances.
     */
    public void publishConnection(int numConnections) {
        if (queueService!=null) {
            queueService.publishActiveConnections(queueActiveConnections, makeConnectionInfoMessage(numConnections));
        }
    }

    /**
     * Make connection info message.
     *
     * @param numConnection the num connection
     * @return the json object
     */
    public JsonObject makeConnectionInfoMessage(int numConnection) {
        JsonObject myConnections = new JsonObject();
        myConnections.putString(UUID_INFO_ID, myUUID);
        myConnections.putNumber(NUM_CONNECTIONS_INFO, numConnection);
        return myConnections;
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
     * @see com.globo.galeb.core.bus.ICallbackConnectionCounter#callbackGlobalConnectionsInfo(org.(Vertx) getPlataform().java.core.json.JsonObject)
     */
    @Override
    public void callbackGlobalConnectionsInfo(JsonObject message) {
        String uuid = message.getString(UUID_INFO_ID);
        if (uuid != myUUID) {
            int numConnections = message.getInteger(NUM_CONNECTIONS_INFO);
            if (numConnections>=0) {
                numExternalSessions += numConnections;
            } else {
                numExternalSessions = 0;
            }
        }
    }

    /**
     * Start session pool.
     *
     * @return the backend
     */
    public Backend startSessionPool() {

        for (int i=0 ; i<minSessionPoolSize ; i++) {
            BackendSession backendSession = new BackendSession(
                    new JsonObject().putString(ID_FIELDNAME, UUID.randomUUID().toString())
                                    .putString(PARENT_ID_FIELDNAME, id)
                                    .putObject(PROPERTIES_FIELDNAME, properties));

            backendSession.setPlataform(plataform)
                          .setQueueService(queueService)
                          .start();

            backendSession.connect();
            poolAvaliable.add(backendSession);
        }

        return this;
    }

    /**
     * Close all forced.
     */
    public void closeAllForced() {
        for (BackendSession backendSession: getEntities().values()) {
            backendSession.close();
        }

        Iterator<BackendSession> poolAvaliableIter = poolAvaliable.iterator();
        while (poolAvaliableIter.hasNext()) {
            BackendSession backendSession = poolAvaliableIter.next();
            if (!backendSession.isClosed()) {
                backendSession.close();
            }
        }
        poolAvaliable.clear();
        clearEntities();
        if (cleanupSessionScheduler!=null) {
            cleanupSessionScheduler.cancel();
        }
    }

    /**
     * Close all.
     */
    public void closeAll() {
        for (String remoteUser: getEntities().keySet()) {
            close(remoteUser);
        }
    }
}
