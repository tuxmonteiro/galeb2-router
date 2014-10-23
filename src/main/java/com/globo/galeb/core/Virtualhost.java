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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.list.UniqueArrayList;
import com.globo.galeb.loadbalance.ILoadBalancePolicy;
import com.globo.galeb.loadbalance.impl.DefaultLoadBalancePolicy;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Class Virtualhost.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class Virtualhost extends Entity {

    /** The Constant BACKENDS_FIELDNAME. */
    public static final String BACKENDS_FIELDNAME            = "backends";

    /** The Constant BACKENDS_ELIGIBLE_FIELDNAME. */
    public static final String BACKENDS_ELIGIBLE_FIELDNAME   = "eligible";

    /** The Constant BACKENDS_FAILED_FIELDNAME. */
    public static final String BACKENDS_FAILED_FIELDNAME     = "failed";

    /** The Constant LOADBALANCE_POLICY_FIELDNAME. */
    public static final String LOADBALANCE_POLICY_FIELDNAME  = "loadBalancePolicy";

    /** The Constant REQUEST_TIMEOUT_FIELDNAME. */
    public static final String REQUEST_TIMEOUT_FIELDNAME     = "requestTimeOut";

    /** The Constant ENABLE_CHUNCKED_FIELDNAME. */
    public static final String ENABLE_CHUNCKED_FIELDNAME     = "enableChunked";

    /** The Constant ENABLE_ACCESSLOG_FIELDNAME. */
    public static final String ENABLE_ACCESSLOG_FIELDNAME    = "enableAccessLog";

    /** The Constant TRANSIENT_STATE_FIELDNAME. */
    public static final String TRANSIENT_STATE_FIELDNAME     = "_transientState";

    /** The backends. */
    private final UniqueArrayList<Backend> backends;

    /** The bad backends. */
    private final UniqueArrayList<Backend> badBackends;

    /** The vertx. */
    private final Vertx                    vertx;

    /** The queue service. */
    private IQueueService                  queueService      = null;

    /** The loadbalance policy. */
    private ILoadBalancePolicy             loadbalancePolicy = null;

    /** The request time out. */
    private Long                           requestTimeOut    = 60000L;

    /** The max pool size. */
    private int                            maxPoolSize       = 1;

    /** The enable chunked. */
    private boolean                        enableChunked     = true;

    /** The enable access log. */
    private boolean                        enableAccessLog   = false;

    /**
     * Instantiates a new virtualhost.
     *
     * @param json the json properties
     * @param vertx the vertx
     */
    public Virtualhost(JsonObject json, final Vertx vertx) {
        super();
        this.id = json.getString(IJsonable.ID_FIELDNAME, "UNDEF");
        this.backends = new UniqueArrayList<Backend>();
        this.badBackends = new UniqueArrayList<Backend>();
        this.vertx = vertx;

        properties.mergeIn(json.getObject(IJsonable.PROPERTIES_FIELDNAME, new JsonObject()));
        getLoadBalancePolicy();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getVirtualhostName();
    }

    /**
     * Sets the queueService.
     *
     * @param queueService the queue service instance
     */
    public void setQueue(final IQueueService queueService) {
        if (this.queueService==null) {
            this.queueService = queueService;
            for(Backend backend: backends) {
                backend.setQueueService(queueService);
            }
            for(Backend badbackend: badBackends) {
                badbackend.setQueueService(queueService);
            }
        }
    }

    /**
     * Update modified timestamp.
     */
    private void updateModifiedTimestamp() {
        modifiedAt = System.currentTimeMillis();
    }

    /**
     * Adds a backend.
     *
     * @param backend the backend
     * @param backendOk the backend ok
     * @return true, if successful
     */
    public boolean addBackend(String backend, boolean backendOk) {
        return addBackend(new JsonObject().putString(IJsonable.ID_FIELDNAME, backend), backendOk);
    }

    /**
     * Adds a backend.
     *
     * @param backendJson the backend json
     * @param backendOk the backend ok
     * @return true, if successful
     */
    public boolean addBackend(JsonObject backendJson, boolean backendOk) {
        updateModifiedTimestamp();
        Backend backend = new Backend(backendJson, vertx);
        backend.setQueueService(queueService);
        backend.setMaxPoolSize(maxPoolSize);
        setTransientState();
        return backendOk ? backends.add(backend) : badBackends.add(backend);
    }

    /**
     * Gets backends.
     *
     * @param backendOk the backend ok
     * @return the backends
     */
    public UniqueArrayList<Backend> getBackends(boolean backendOk) {
        return backendOk ? backends: badBackends;
    }

    /**
     * Gets the virtualhost name.
     *
     * @return the virtualhost name
     */
    public String getVirtualhostName() {
        return id;
    }

    /**
     * Removes a backend.
     *
     * @param backend the backend
     * @param backendOk the backend ok
     * @return the boolean
     */
    public Boolean removeBackend(String backend, boolean backendOk) {
        updateModifiedTimestamp();
        if (backendOk) {
            setTransientState();
            return backends.remove(new Backend(backend, vertx));
        } else {
            return badBackends.remove(new Backend(backend, vertx));
        }
    }

    /**
     * Clear backends (filtered to backendOk).
     *
     * @param backendOk the backend ok
     */
    public void clear(boolean backendOk) {
        updateModifiedTimestamp();
        if (backendOk) {
            backends.clear();
            setTransientState();
        } else {
            badBackends.clear();
        }
    }

    /**
     * Clear all backends.
     */
    public void clearAll() {
        updateModifiedTimestamp();
        backends.clear();
        badBackends.clear();
        setTransientState();
    }

    /**
     * Gets the backend chosen.
     *
     * @param requestData the request data
     * @return the choice
     */
    public Backend getChoice(RequestData requestData) {
        requestData.setProperties(properties);
        Backend chosen;
        if (loadbalancePolicy==null) {
            getLoadBalancePolicy();
        }
        chosen = loadbalancePolicy.getChoice(backends, requestData);
        unSetTransientState();
        return chosen;
    }

    /**
     * Sets the load balance policy.
     *
     * @param loadBalancePolicyName the load balance policy name
     * @return the virtualhost
     */
    public Virtualhost setLoadBalancePolicy(String loadBalancePolicyName) {
        properties.putString(Virtualhost.LOADBALANCE_POLICY_FIELDNAME, loadBalancePolicyName);
        return this;
    }

    /**
     * Gets the load balance policy.
     *
     * @return the load balance policy
     */
    public ILoadBalancePolicy getLoadBalancePolicy() {
        String loadBalancePolicyStr = properties.getString(LOADBALANCE_POLICY_FIELDNAME,
                DefaultLoadBalancePolicy.class.getSimpleName());
        loadbalancePolicy = loadBalancePolicyClassLoader(loadBalancePolicyStr);
        if (loadbalancePolicy.isDefault()) {
            properties.putString(LOADBALANCE_POLICY_FIELDNAME, loadbalancePolicy.toString());
        }
        return loadbalancePolicy;
    }

    /**
     * Load balance policy class loader.
     *
     * @param loadBalancePolicyName the load balance policy name
     * @return load balance policy instance
     */
    public ILoadBalancePolicy loadBalancePolicyClassLoader(String loadBalancePolicyName) {
        try {
            String classFullName=String.format("%s.%s",
                    DefaultLoadBalancePolicy.class.getPackage().getName(), loadBalancePolicyName);

            @SuppressWarnings("unchecked")
            Class<ILoadBalancePolicy> classLoader = (Class<ILoadBalancePolicy>) Class.forName(classFullName);
            Constructor<ILoadBalancePolicy> classPolicy = classLoader.getConstructor();

            return classPolicy.newInstance();

        } catch (   ClassNotFoundException |
                    InstantiationException |
                    IllegalAccessException |
                    IllegalArgumentException |
                    InvocationTargetException |
                    NoSuchMethodException |
                    SecurityException e1 ) {
            ILoadBalancePolicy defaultLoadBalance = new DefaultLoadBalancePolicy();
            return defaultLoadBalance;
        }
    }

    /**
     * Checks if has backends.
     *
     * @return true, if successful
     */
    public boolean hasBackends() {
        return !backends.isEmpty();
    }

    /**
     * Checks if has bad backends.
     *
     * @return true, if successful
     */
    public boolean hasBadBackends() {
        return !badBackends.isEmpty();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.Entity#toJson()
     */
    @Override
    public JsonObject toJson() {
        properties.putNumber(REQUEST_TIMEOUT_FIELDNAME, requestTimeOut);
        properties.putNumber(Backend.MAXPOOL_SIZE_FIELDNAME, maxPoolSize);

        prepareJson();

        JsonArray backendsElegiblesJson = new JsonArray();
        JsonArray backendsFailedJson    = new JsonArray();

        for (Backend backend: backends) {
            if (backend!=null) {
                backendsElegiblesJson.addObject(backend.toJson());
            }
        }

        for (Backend badBackend: badBackends) {
            if (badBackend!=null) {
                backendsFailedJson.addObject(badBackend.toJson());
            }
        }

        JsonObject backendsJson = new JsonObject();

        backendsJson.putArray(BACKENDS_ELIGIBLE_FIELDNAME, backendsElegiblesJson);
        backendsJson.putArray(BACKENDS_FAILED_FIELDNAME, backendsFailedJson);

        idObj.putObject(BACKENDS_FIELDNAME, backendsJson);

        return super.toJson();
    }

    /**
     * Sets transient state.
     */
    public void setTransientState() {
        idObj.putBoolean(TRANSIENT_STATE_FIELDNAME, true);
    }

    /**
     * Unsets transient state.
     */
    private void unSetTransientState() {
        idObj.putBoolean(TRANSIENT_STATE_FIELDNAME, false);
    }

    /**
     * Gets the request time out.
     *
     * @return the request time out
     */
    public Long getRequestTimeOut() {
        return this.requestTimeOut;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.Entity#setStaticConf(java.lang.String)
     */
    @Override
    public Virtualhost setStaticConf(String staticConf) {
        super.setStaticConf(staticConf);
        requestTimeOut = this.staticConf.getLong(REQUEST_TIMEOUT_FIELDNAME, requestTimeOut);
        maxPoolSize = this.staticConf.getInteger(Backend.MAXPOOL_SIZE_FIELDNAME, maxPoolSize);
        enableChunked = this.staticConf.getBoolean(ENABLE_CHUNCKED_FIELDNAME, enableChunked);
        enableAccessLog = this.staticConf.getBoolean(ENABLE_ACCESSLOG_FIELDNAME, enableAccessLog);

        return this;
    }

    /**
     * Checks if is chunked.
     *
     * @return true, if chunked
     */
    public Boolean isChunked() {
        return properties.getBoolean(ENABLE_CHUNCKED_FIELDNAME, enableChunked);
    }

    /**
     * Checks if access log is enabled.
     *
     * @return true, if enabled
     */
    public Boolean hasAccessLog() {
        return properties.getBoolean(ENABLE_ACCESSLOG_FIELDNAME, enableAccessLog);
    }
}
