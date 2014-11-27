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
package com.globo.galeb.entity.impl.backend;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.criteria.ICriterion;
import com.globo.galeb.criteria.impl.LoadBalanceCriterion;
import com.globo.galeb.entity.EntitiesMap;
import com.globo.galeb.entity.Entity;
import com.globo.galeb.request.RequestData;
import com.globo.galeb.rulereturn.IRuleReturn;


/**
 * Class BackendPool.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 6, 2014.
 */
public class BackendPool extends EntitiesMap<IBackend> implements IRuleReturn {

    /** The Constant BACKENDS_FIELDNAME. */
    public static final String BACKENDS_FIELDNAME           = "backends";

    /** The Constant REQUEST_TIMEOUT_FIELDNAME. */
    public static final String REQUEST_TIMEOUT_FIELDNAME    = "requestTimeOut";

    /** The Constant ENABLE_CHUNCKED_FIELDNAME. */
    public static final String ENABLE_CHUNCKED_FIELDNAME    = "enableChunked";

    /** The Constant ENABLE_ACCESSLOG_FIELDNAME. */
    public static final String ENABLE_ACCESSLOG_FIELDNAME   = "enableAccessLog";

    /** The rule return type. */
    private final String               returnType           = BackendPool.class.getSimpleName();

    /** The bad backends. */
    private final EntitiesMap<IBackend> badBackends         = new BadBackendPool("badbackends");

    /** The load balance policy. */
    private ICriterion<IBackend>       loadBalanceCriterion = new LoadBalanceCriterion<IBackend>();

    /** The request time out. */
    private Long                       requestTimeOut      = 60000L;

    /** The max pool size. */
    private int                        maxPoolSize         = IBackend.DEFAULT_MAX_POOL_SIZE;

    /** The keep alive max request. */
    private long                       keepAliveMaxRequest = IBackend.DEFAULT_KEEPALIVE_MAXREQUEST;

    /** The keep alive time out. */
    private long                       keepAliveTimeOut    = IBackend.DEFAULT_KEEPALIVE_TIMEOUT;

    /** The enable chunked. */
    private boolean                    enableChunked       = true;

    /** The enable access log. */
    private boolean                    enableAccessLog     = false;

    /** The min session pool size. */
    private int                        minSessionPoolSize  = IBackend.DEFAULT_MIN_SESSION_POOL_SIZE;

    /** The keep alive. */
    private boolean                    keepAlive           = IBackend.DEFAULT_KEEPALIVE;

    /** The pipelining. */
    private boolean                    pipelining          = IBackend.DEFAULT_PIPELINING;

    /** The receive buffer size. */
    private int                        receiveBufferSize   = IBackend.TCP_RECEIVED_BUFFER_SIZE;

    /** The send buffer size. */
    private int                        sendBufferSize      = IBackend.TCP_SEND_BUFFER_SIZE;

    /** The use pooled buffers. */
    private boolean                    usePooledBuffers    = IBackend.DEFAULT_USE_POOLED_BUFFERS;

    /**
     * Instantiates a new backend pool.
     */
    public BackendPool() {
        super();
        this.status = StatusType.RUNNING_STATUS;
    }

    /**
     * Instantiates a new backend pool.
     *
     * @param id the id
     */
    public BackendPool(String id) {
        super(id);
        this.status = StatusType.RUNNING_STATUS;
    }

    /**
     * Instantiates a new backend pool.
     *
     * @param json the json
     */
    public BackendPool(JsonObject json) {
        super(json);
        this.status = StatusType.RUNNING_STATUS;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.rules.IRuleReturn#getReturnType()
     */
    @Override
    public String getReturnType() {
        return returnType;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.rules.IRuleReturn#getReturnId()
     */
    @Override
    public String getReturnId() {
        return id;
    }

    /**
     * Gets the choice.
     *
     * @param requestData the request data
     * @return backend
     */
    public IBackend getChoice(RequestData requestData) {

        keepAlive = requestData.getKeepAlive();

        requestData.getProperties().mergeIn(properties);

        return loadBalanceCriterion.setLog(logger)
                                   .given(getEntities())
                                   .when(requestData)
                                   .thenGetResult();
    }

    /**
     * Reset load balance.
     *
     * @return this
     */
    public BackendPool resetLoadBalance() {
        loadBalanceCriterion.action(ICriterion.CriterionAction.RESET_REQUIRED);
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.entity.EntitiesMap#addEntity(com.globo.galeb.core.entity.Entity)
     */
    @Override
    public boolean addEntity(IBackend backend) {
        ((IBackend) ((Entity) backend.setMaxPoolSize(maxPoolSize)
                                     .setKeepAlive(keepAlive)
                                     .setPipelining(pipelining)
                                     .setReceiveBufferSize(receiveBufferSize)
                                     .setSendBufferSize(sendBufferSize)
                                     .setUsePooledBuffers(usePooledBuffers)
                                     .setKeepAliveMaxRequest(keepAliveMaxRequest)
                                     .setKeepAliveTimeOut(keepAliveTimeOut)
                                     .setMinSessionPoolSize(minSessionPoolSize))
                                     .setStatus(StatusType.RUNNING_STATUS))
                                     .startSessionPool();

        resetLoadBalance();

        return super.addEntity(backend);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.entity.EntitiesMap#removeEntity(com.globo.galeb.core.entity.Entity)
     */
    @Override
    public boolean removeEntity(IBackend backend) {
        backend.closeAllForced();
        resetLoadBalance();
        return super.removeEntity(backend);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.entity.EntitiesMap#clearEntities()
     */
    @Override
    public void clearEntities() {
        for (IBackend backend: getEntities().values()) {
            backend.closeAllForced();
        }
        resetLoadBalance();
        super.clearEntities();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.entity.Entity#toJson()
     */
    @Override
    public JsonObject toJson() {
        properties.putNumber(REQUEST_TIMEOUT_FIELDNAME, requestTimeOut);
        properties.putNumber(IBackend.MAXPOOL_SIZE_FIELDNAME, maxPoolSize);
        properties.putNumber(IBackend.KEEPALIVE_MAXREQUEST_FIELDNAME, keepAliveMaxRequest);
        properties.putNumber(IBackend.KEEPALIVE_TIMEOUT_FIELDNAME, keepAliveTimeOut);
        properties.putNumber(IBackend.MIN_SESSION_POOL_SIZE_FIELDNAME, minSessionPoolSize);
        if (!properties.containsField(LoadBalanceCriterion.LOADBALANCE_POLICY_FIELDNAME)) {
            properties.putString(LoadBalanceCriterion.LOADBALANCE_POLICY_FIELDNAME, LoadBalanceCriterion.LOADBALANCE_POLICY_DEFAULT);
        }
        prepareJson();

        JsonArray backendsJson = new JsonArray();
        for (IBackend backend: getEntities().values()) {
            backendsJson.addObject(backend.toJson());
        }
        for (IBackend backend: getBadBackends().getEntities().values()) {
            ((Entity) backend).setStatus(StatusType.FAILED_STATUS);
            backendsJson.addObject(backend.toJson());
        }
        idObj.putArray(BACKENDS_FIELDNAME, backendsJson);

        return super.toJson();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.entity.Entity#setStaticConf(org.vertx.java.core.json.JsonObject)
     */
    @Override
    public BackendPool setStaticConf(JsonObject staticConf) {
        super.setStaticConf(staticConf);
        requestTimeOut = this.staticConf.getLong(REQUEST_TIMEOUT_FIELDNAME, requestTimeOut);
        maxPoolSize = this.staticConf.getInteger(IBackend.MAXPOOL_SIZE_FIELDNAME, maxPoolSize);
        minSessionPoolSize = this.staticConf.getInteger(IBackend.MIN_SESSION_POOL_SIZE_FIELDNAME, minSessionPoolSize);
        keepAliveMaxRequest = this.staticConf.getLong(IBackend.KEEPALIVE_MAXREQUEST_FIELDNAME, keepAliveMaxRequest);
        keepAliveTimeOut = this.staticConf.getLong(IBackend.KEEPALIVE_TIMEOUT_FIELDNAME, keepAliveTimeOut);
        enableChunked = this.staticConf.getBoolean(ENABLE_CHUNCKED_FIELDNAME, enableChunked);
        enableAccessLog = this.staticConf.getBoolean(ENABLE_ACCESSLOG_FIELDNAME, enableAccessLog);

        return this;
    }

    /**
     * Gets the bad backends map.
     *
     * @return the bad backends map
     */
    public EntitiesMap<IBackend> getBadBackends() {
        return badBackends;
    }

    /**
     * Gets the length of bad backend pool.
     *
     * @return the length of bad backend pool
     */
    public int getNumBadBackend() {
        return badBackends.getNumEntities();
    }

    /**
     * Gets the bad backend by id.
     *
     * @param entityId the entity id
     * @return the bad backend by id
     */
    public IBackend getBadBackendById(String entityId) {
        return badBackends.getEntityById(entityId);
    }

    /**
     * Clear bad backend.
     */
    public void clearBadBackend() {
        for (IBackend backend: badBackends.getEntities().values()) {
            backend.closeAllForced();
        }
        badBackends.clearEntities();
    }

    /**
     * Adds the bad backend.
     *
     * @param entity the entity
     * @return true, if successful
     */
    public boolean addBadBackend(IBackend entity) {
        ((Entity) entity).setStatus(StatusType.FAILED_STATUS);
        entity.closeAllForced();
        return badBackends.addEntity(entity);
    }

    /**
     * Removes the bad backend.
     *
     * @param entity the entity
     * @return true, if successful
     */
    public boolean removeBadBackend(IBackend entity) {
        return badBackends.removeEntity(entity);
    }

    /**
     * Removes the bad backend by id.
     *
     * @param id the id
     * @return true, if successful
     */
    public boolean removeBadBackend(String id) {
        return badBackends.removeEntity(id);
    }

    /**
     * Removes the bad backend.
     *
     * @param json the json
     * @return true, if successful
     */
    public boolean removeBadBackend(JsonObject json) {
        return badBackends.removeEntity(json);
    }

    /**
     * Clear all.
     */
    public void clearAll() {
        clearEntities();
        clearBadBackend();
    }

    /**
     * Checks if is chunked.
     *
     * @return true, if chunked
     */
    public Boolean isChunked() {
        return enableChunked;
    }

    /**
     * Checks if access log is enabled.
     *
     * @return true, if enabled
     */
    public Boolean hasAccessLog() {
        return enableAccessLog;
    }

}

