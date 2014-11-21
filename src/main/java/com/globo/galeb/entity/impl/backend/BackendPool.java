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
import com.globo.galeb.criteria.LoadBalanceCriterionFactory;
import com.globo.galeb.criteria.impl.LoadBalanceCriterion;
import com.globo.galeb.criteria.impl.RandomCriterion;
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

    /** The Constant LOADBALANCE_POLICY_FIELDNAME. */
    public static final String LOADBALANCE_POLICY_FIELDNAME  = "loadBalancePolicy";

    /** The Constant BACKENDS_FIELDNAME. */
    public static final String BACKENDS_FIELDNAME            = "backends";

    /** The Constant REQUEST_TIMEOUT_FIELDNAME. */
    public static final String REQUEST_TIMEOUT_FIELDNAME     = "requestTimeOut";

    /** The Constant ENABLE_CHUNCKED_FIELDNAME. */
    public static final String ENABLE_CHUNCKED_FIELDNAME     = "enableChunked";

    /** The Constant ENABLE_ACCESSLOG_FIELDNAME. */
    public static final String ENABLE_ACCESSLOG_FIELDNAME    = "enableAccessLog";


    /** The rule return type. */
    private final String               returnType           = BackendPool.class.getSimpleName();

    /** The bad backends. */
    private final EntitiesMap<IBackend> badBackends         = new BadBackendPool("badbackends");

    /** The load balance policy. */
    private ICriterion<IBackend>        loadBalanceCriterion   = new LoadBalanceCriterion();

    /** The request time out. */
    private Long                       requestTimeOut      = 60000L;

    /** The max pool size. */
    private int                        maxPoolSize         = 1;

    /** The keep alive max request. */
    private long                       keepAliveMaxRequest = Long.MAX_VALUE;

    /** The keep alive time out. */
    private long                       keepAliveTimeOut    = 86400000L;

    /** The enable chunked. */
    private boolean                    enableChunked       = true;

    /** The enable access log. */
    private boolean                    enableAccessLog     = false;

    /** The min session pool size. */
    private int                        minSessionPoolSize  =  1;

    /** The keep alive. */
    private boolean                    keepAlive           = true;

    /** The loadBalance Name */
    private String                     loadBalanceName     = RandomCriterion.class.getSimpleName()
                                                                 .replaceAll(LoadBalanceCriterionFactory.CLASS_SUFFIX, "");

    /**
     * Instantiates a new backend pool.
     */
    public BackendPool() {
        super();
        this.status = StatusType.RUNNING_STATUS.toString();
    }

    /**
     * Instantiates a new backend pool.
     *
     * @param id the id
     */
    public BackendPool(String id) {
        super(id);
        this.status = StatusType.RUNNING_STATUS.toString();
    }

    /**
     * Instantiates a new backend pool.
     *
     * @param json the json
     */
    public BackendPool(JsonObject json) {
        super(json);
        this.status = StatusType.RUNNING_STATUS.toString();
        this.loadBalanceName = json.getObject(PROPERTIES_FIELDNAME, new JsonObject())
                                       .getString(LOADBALANCE_POLICY_FIELDNAME,
                                               RandomCriterion.class.getSimpleName()
                                                   .replaceAll(LoadBalanceCriterionFactory.CLASS_SUFFIX, ""));

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
        return loadBalanceCriterion.setLog(logger)
                                   .given(getEntities())
                                   .when(loadBalanceName)
                                   .when(requestData)
                                   .thenGetResult();
    }

    /**
     * Sets the load balance policy.
     *
     * @param loadbalanceName the load balance policy name
     * @return the backend pool
     */
    public BackendPool setLoadBalancePolicy(String loadbalanceName) {
        loadBalanceCriterion.setLog(logger)
                            .given(getEntities())
                            .when(loadbalanceName)
                            .when(ICriterion.CriterionAction.RESET_REQUIRED)
                            .thenGetResult();
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.entity.EntitiesMap#addEntity(com.globo.galeb.core.entity.Entity)
     */
    @Override
    public boolean addEntity(IBackend backend) {

        ((IBackend) ((Entity) backend.setMaxPoolSize(maxPoolSize)
                                     .setKeepAlive(keepAlive)
                                     .setKeepAliveMaxRequest(keepAliveMaxRequest)
                                     .setKeepAliveTimeOut(keepAliveTimeOut)
                                     .setMinSessionPoolSize(minSessionPoolSize))
                                     .setStatus(StatusType.RUNNING_STATUS.toString()))
                                     .startSessionPool();

        return super.addEntity(backend);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.entity.EntitiesMap#removeEntity(com.globo.galeb.core.entity.Entity)
     */
    @Override
    public boolean removeEntity(IBackend backend) {
        backend.closeAllForced();
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
        super.clearEntities();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.entity.Entity#toJson()
     */
    @Override
    public JsonObject toJson() {
        properties.putNumber(REQUEST_TIMEOUT_FIELDNAME, requestTimeOut);
        properties.putNumber(Backend.MAXPOOL_SIZE_FIELDNAME, maxPoolSize);
        properties.putNumber(Backend.KEEPALIVE_MAXREQUEST_FIELDNAME, keepAliveMaxRequest);
        properties.putNumber(Backend.KEEPALIVE_TIMEOUT_FIELDNAME, keepAliveTimeOut);
        properties.putNumber(Backend.MIN_SESSION_POOL_SIZE_FIELDNAME, minSessionPoolSize);

        prepareJson();

        JsonArray backendsJson = new JsonArray();

        for (IBackend backend: getEntities().values()) {
            if (backend!=null) {
                backendsJson.addObject(((Entity) backend).toJson());
            }
        }

        for (IBackend badBackend: getBadBackends().getEntities().values()) {
            if (badBackend!=null) {
                backendsJson.addObject(((Entity) badBackend).toJson());
            }
        }

        idObj.putArray(BACKENDS_FIELDNAME, backendsJson);

        return super.toJson();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.Entity#setStaticConf(java.lang.String)
     */
    @Override
    public BackendPool setStaticConf(String staticConf) {
        super.setStaticConf(staticConf);
        requestTimeOut = this.staticConf.getLong(REQUEST_TIMEOUT_FIELDNAME, requestTimeOut);
        maxPoolSize = this.staticConf.getInteger(Backend.MAXPOOL_SIZE_FIELDNAME, maxPoolSize);
        minSessionPoolSize = this.staticConf.getInteger(Backend.MIN_SESSION_POOL_SIZE_FIELDNAME, minSessionPoolSize);
        keepAliveMaxRequest = this.staticConf.getLong(Backend.KEEPALIVE_MAXREQUEST_FIELDNAME, keepAliveMaxRequest);
        keepAliveTimeOut = this.staticConf.getLong(Backend.KEEPALIVE_TIMEOUT_FIELDNAME, keepAliveTimeOut);
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
    public boolean addBadBackend(Backend entity) {
        entity.setStatus(StatusType.FAILED_STATUS.toString());
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
     * Interchange backend status.
     *
     * @param entity the entity
     */
    public void interchangeBackendStatus(Backend entity) {
        if (entity!=null) {
            String backendStatus = entity.getStatus();
            if (backendStatus.equals(UNDEF)||backendStatus.equals(StatusType.FAILED_STATUS.toString())) {
                removeBadBackend(entity);
                addEntity(entity);
            } else {
                removeEntity(entity);
                addBadBackend(entity);
            }
        }
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

