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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.list.UniqueArrayList;
import com.globo.galeb.loadbalance.ILoadBalancePolicy;
import com.globo.galeb.loadbalance.impl.DefaultLoadBalancePolicy;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class Virtualhost extends Entity {

    public static final String BACKENDS_FIELDNAME            = "backends";
    public static final String BACKENDS_ELIGIBLE_FIELDNAME   = "eligible";
    public static final String BACKENDS_FAILED_FIELDNAME     = "failed";

    public static final String LOADBALANCE_POLICY_FIELDNAME  = "loadBalancePolicy";
    public static final String REQUEST_TIMEOUT_FIELDNAME     = "requestTimeOut";
    public static final String ENABLE_CHUNCKED_FIELDNAME     = "enableChunked";
    public static final String ENABLE_ACCESSLOG_FIELDNAME    = "enableAccessLog";

    public static final String TRANSIENT_STATE_FIELDNAME     = "_transientState";

    private final UniqueArrayList<Backend> backends;
    private final UniqueArrayList<Backend> badBackends;
    private final Vertx                    vertx;
    private IQueueService                  queueService      = null;
    private ILoadBalancePolicy             loadbalancePolicy = null;
    private Long                           requestTimeOut    = 60000L;
    private int                            maxPoolSize       = 1;
    private boolean                        enableChunked     = true;
    private boolean                        enableAccessLog   = false;

    public Virtualhost(JsonObject json, final Vertx vertx) {
        super();
        this.id = json.getString(IJsonable.ID_FIELDNAME, "UNDEF");
        this.backends = new UniqueArrayList<Backend>();
        this.badBackends = new UniqueArrayList<Backend>();
        this.vertx = vertx;

        properties.mergeIn(json.getObject(IJsonable.PROPERTIES_FIELDNAME, new JsonObject()));
        getLoadBalancePolicy();
    }

    @Override
    public String toString() {
        return getVirtualhostName();
    }

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

    private void updateModifiedTimestamp() {
        modifiedAt = System.currentTimeMillis();
    }

    public boolean addBackend(String backend, boolean backendOk) {
        return addBackend(new JsonObject().putString(IJsonable.ID_FIELDNAME, backend), backendOk);
    }

    public boolean addBackend(JsonObject backendJson, boolean backendOk) {
        updateModifiedTimestamp();
        Backend backend = new Backend(backendJson, vertx);
        backend.setQueueService(queueService);
        backend.setMaxPoolSize(maxPoolSize);
        setTransientState();
        return backendOk ? backends.add(backend) : badBackends.add(backend);
    }

    public UniqueArrayList<Backend> getBackends(boolean backendOk) {
        return backendOk ? backends: badBackends;
    }

    public String getVirtualhostName() {
        return id;
    }

    public Boolean removeBackend(String backend, boolean backendOk) {
        updateModifiedTimestamp();
        if (backendOk) {
            setTransientState();
            return backends.remove(new Backend(backend, vertx));
        } else {
            return badBackends.remove(new Backend(backend, vertx));
        }
    }

    public void clear(boolean backendOk) {
        updateModifiedTimestamp();
        if (backendOk) {
            backends.clear();
            setTransientState();
        } else {
            badBackends.clear();
        }
    }

    public void clearAll() {
        updateModifiedTimestamp();
        backends.clear();
        badBackends.clear();
        setTransientState();
    }

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

    public Virtualhost setLoadBalancePolicy(String loadBalancePolicyName) {
        properties.putString(Virtualhost.LOADBALANCE_POLICY_FIELDNAME, loadBalancePolicyName);
        return this;
    }

    public ILoadBalancePolicy getLoadBalancePolicy() {
        String loadBalancePolicyStr = properties.getString(LOADBALANCE_POLICY_FIELDNAME,
                DefaultLoadBalancePolicy.class.getSimpleName());
        loadbalancePolicy = loadBalancePolicyClassLoader(loadBalancePolicyStr);
        if (loadbalancePolicy.isDefault()) {
            properties.putString(LOADBALANCE_POLICY_FIELDNAME, loadbalancePolicy.toString());
        }
        return loadbalancePolicy;
    }

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

    public boolean hasBackends() {
        return !backends.isEmpty();
    }

    public boolean hasBadBackends() {
        return !badBackends.isEmpty();
    }

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

    public void setTransientState() {
        idObj.putBoolean(TRANSIENT_STATE_FIELDNAME, true);
    }

    private void unSetTransientState() {
        idObj.putBoolean(TRANSIENT_STATE_FIELDNAME, false);
    }

    public Long getRequestTimeOut() {
        return this.requestTimeOut;
    }

    @Override
    public Virtualhost setStaticConf(String staticConf) {
        super.setStaticConf(staticConf);
        requestTimeOut = this.staticConf.getLong(REQUEST_TIMEOUT_FIELDNAME, requestTimeOut);
        maxPoolSize = this.staticConf.getInteger(Backend.MAXPOOL_SIZE_FIELDNAME, maxPoolSize);
        enableChunked = this.staticConf.getBoolean(ENABLE_CHUNCKED_FIELDNAME, enableChunked);
        enableAccessLog = this.staticConf.getBoolean(ENABLE_ACCESSLOG_FIELDNAME, enableAccessLog);

        return this;
    }

    public Boolean isChunked() {
        return properties.getBoolean(ENABLE_CHUNCKED_FIELDNAME, enableChunked);
    }

    public Boolean hasAccessLog() {
        return properties.getBoolean(ENABLE_ACCESSLOG_FIELDNAME, enableAccessLog);
    }
}
