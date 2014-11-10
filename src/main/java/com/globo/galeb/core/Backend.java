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
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.core.bus.ICallbackConnectionCounter;
import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.core.entity.Entity;
import com.globo.galeb.core.entity.IJsonable;
import com.globo.galeb.metrics.ICounter;
import com.globo.galeb.scheduler.IScheduler;
import com.globo.galeb.scheduler.ISchedulerHandler;
import com.globo.galeb.scheduler.impl.VertxPeriodicScheduler;

/**
 * Class Backend.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class Backend extends Entity implements ICallbackConnectionCounter {

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

    /** The Constant NUM_CONNECTIONS_FIELDNAME. */
    public static final String NUM_CONNECTIONS_FIELDNAME       = "numConnections";

    /** The Constant UUID_FIELDNAME. */
    public static final String UUID_FIELDNAME                  = "uuid";


    /** The host name or IP. */
    private final String host;

    /** The port. */
    private final Integer port;

    /** The my uuid. */
    private final String myUUID;

    /** The queue active connections. */
    private final String queueActiveConnections;

    /** The ICounter. */
    private ICounter           counter            = null;

    /** The queue service. */
    private IQueueService queueService            = null;

    /** The virtualhost id. */
    private String     virtualhostId              = "";

    /** The default keep alive. */
    private Boolean defaultKeepAlive              = true;

    /** The default connection timeout. */
    private Integer defaultConnectionTimeout      = 60000; // 10 minutes

    /** The default keepalive max request. */
    private Long    defaultKeepaliveMaxRequest    = Long.MAX_VALUE-1;

    /** The default keep alive time out. */
    private Long    defaultKeepAliveTimeOut       = 86400000L; // One day

    /** The default max pool size. */
    private Integer defaultMaxPoolSize            = 1;

    /** The default use pooled buffers. */
    private Boolean defaultUsePooledBuffers       = false;

    /** The default send buffer size. */
    private Integer defaultSendBufferSize         = Constants.TCP_SEND_BUFFER_SIZE;

    /** The default receive buffer size. */
    private Integer defaultReceiveBufferSize      = Constants.TCP_RECEIVED_BUFFER_SIZE;

    /** The default pipelining. */
    private Boolean defaultPipelining             = false;

    /** The map of sessions. */
    private final Map<RemoteUser, BackendSession> sessions = new HashMap<>();

    /** The min session pool size. */
    private int minSessionPoolSize = 1;

    /** The pool avaliable. */
    private final Set<BackendSession> poolAvaliable = new HashSet<>();

    /** The pool unavaliable. */
    private final Set<BackendSession> poolUnavaliable = new HashSet<>();

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

            if (!backend.sessions.isEmpty() && !backend.isLocked.get()) {
                backend.isLocked.set(true);
                Map<RemoteUser, BackendSession> tmpSessions = new HashMap<>(backend.sessions);
                for (Entry<RemoteUser, BackendSession> entry : tmpSessions.entrySet()) {
                    RemoteUser remoteUser = entry.getKey();
                    BackendSession backendSession = entry.getValue();
                    if (backendSession.isClosed()) {
                        backend.removeSession(remoteUser);
                    }
                }
                backend.isLocked.set(false);
            }

            publishConnection(sessions.size());
            numExternalSessions = 0;

        }
    }

    /**
     * Instantiates a new backend.
     *
     * @param backendId the backend id
     */
    public Backend(final String backendId) {
        this(new JsonObject().putString(IJsonable.ID_FIELDNAME, backendId)  );
    }

    /**
     * Instantiates a new backend.
     *
     * @param json the json
     */
    public Backend(JsonObject json) {
        super();
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
            JsonObject jsonProperties = json.getObject(PROPERTIES_FIELDNAME, new JsonObject());
            properties.mergeIn(jsonProperties);
        }
        this.queueActiveConnections = String.format("%s%s", IQueueService.QUEUE_BACKEND_CONNECTIONS_PREFIX, this);
        this.myUUID = UUID.randomUUID().toString();
        registerConnectionsCounter();
        publishConnection(0);

    }

    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        cleanupSessionScheduler.cancel();
    }

    /**
     * Sets the queue service.
     *
     * @param queueService the queue service
     * @return this
     */
    @Override
    public Backend setQueueService(IQueueService queueService) {
        this.queueService = queueService;
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
        this.counter = counter;
        return this;
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

        final BackendSession backendSession;

        if (cleanupSessionScheduler==null && (Vertx) getPlataform()!=null) {
            cleanupSessionScheduler = new VertxPeriodicScheduler((Vertx) getPlataform())
                                            .setPeriod(10000L)
                                            .setHandler(new CleanUpSessionHandler(this))
                                            .start();
        }

        if (sessions.containsKey(remoteUser)) {
            backendSession = sessions.get(remoteUser);
        } else {

            if (!poolAvaliable.isEmpty()) {

                backendSession = poolAvaliable.iterator().next();
                poolAvaliable.remove(backendSession);
                poolUnavaliable.add(backendSession);

            } else {

                backendSession = new BackendSession(id)
                                        .setPlataform(getPlataform())
                                        .setQueueService(queueService)
                                        .setMaxPoolSize(getMaxPoolSize())
                                        .setBackendProperties(properties)
                                        .setKeepAliveMaxRequest(getKeepAliveMaxRequest())
                                        .setKeepAliveTimeOut(getKeepAliveTimeOut());

                poolUnavaliable.add(backendSession);

            }

            sessions.put(remoteUser, backendSession);

            String backendId = this.toString();
            if (!"".equals(virtualhostId) && !"UNDEF".equals(virtualhostId) &&
                    !"".equals(backendId) && !"UNDEF".equals(backendId)) {
                counter.sendActiveSessions(virtualhostId, backendId, 1L);
            }

        }

        return backendSession.connect();
    }

    /**
     * Close connection and destroy backendSession instance.
     */
    public void close(RemoteUser remoteUser) {
        if (!(remoteUser==null) && sessions.containsKey(remoteUser)) {
            BackendSession backendSession = sessions.get(remoteUser);
            poolUnavaliable.remove(backendSession);

            if (poolAvaliable.size()>=minSessionPoolSize) {
                backendSession.close();
            } else {
                poolAvaliable.add(backendSession);
            }

            sessions.remove(remoteUser);
        }
    }

    /**
     * Gets the active connections.
     *
     * @return the active connections
     */
    public int getActiveConnections() {
        return sessions.size() + numExternalSessions;
    }

    /**
     * Checks if is closed.
     *
     * @return true, if is closed
     */
    public boolean isClosed(RemoteUser remoteUser) {
        if (!(remoteUser==null) && sessions.containsKey(remoteUser)) {
            return sessions.get(remoteUser).isClosed();
        }
        return true;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.Entity#toJson()
     */
    @Override
    public JsonObject toJson() {
        prepareJson();
        idObj.putString(Entity.PARENT_ID_FIELDNAME, virtualhostId);
        idObj.putNumber(ACTIVE_CONNECTIONS_FIELDNAME, getActiveConnections());

        return super.toJson();
    }

    /**
     * Removes the session.
     *
     * @param remoteUser the remote user
     */
    public void removeSession(RemoteUser remoteUser) {
        sessions.remove(remoteUser);
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
        myConnections.putString(UUID_FIELDNAME, myUUID);
        myConnections.putNumber(NUM_CONNECTIONS_FIELDNAME, numConnection);
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
        String uuid = message.getString(UUID_FIELDNAME);
        if (uuid != myUUID) {
            int numConnections = message.getInteger(NUM_CONNECTIONS_FIELDNAME);
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
            BackendSession backendSession = new BackendSession(id)
                                                .setPlataform(getPlataform())
                                                .setQueueService(queueService)
                                                .setMaxPoolSize(getMaxPoolSize())
                                                .setBackendProperties(properties)
                                                .setKeepAliveMaxRequest(getKeepAliveMaxRequest())
                                                .setKeepAliveTimeOut(getKeepAliveTimeOut());
            backendSession.connect();
            poolAvaliable.add(backendSession);
        }

        return this;
    }

    /**
     * Close all forced.
     */
    public void closeAllForced() {
        for (BackendSession backendSession: sessions.values()) {
            backendSession.close();
        }
        poolAvaliable.clear();
        poolUnavaliable.clear();
        sessions.clear();
        cleanupSessionScheduler.cancel();
    }

    /**
     * Close all.
     */
    public void closeAll() {
        for (RemoteUser remoteUser: sessions.keySet()) {
            close(remoteUser);
        }
    }
}
