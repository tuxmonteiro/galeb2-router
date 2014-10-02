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

    public static final String backendsFieldName          = "backends";
    public static final String backendsElegibleFieldName  = "eligible";
    public static final String backendsFailedFieldName    = "failed";

    // Modifiable
    public static final String loadBalancePolicyFieldName = "loadBalancePolicy";

    //
    public static final String transientStateFieldName    = "_transientState";

    private final UniqueArrayList<Backend> backends;
    private final UniqueArrayList<Backend> badBackends;
    private final Vertx                    vertx;
    private IQueueService                  queueService      = null;
    private ILoadBalancePolicy             loadbalancePolicy = null;

    public Virtualhost(JsonObject json, final Vertx vertx) {
        super();
        this.id = json.getString(IJsonable.jsonIdFieldName, "UNDEF");
        this.backends = new UniqueArrayList<Backend>();
        this.badBackends = new UniqueArrayList<Backend>();
        this.vertx = vertx;
        properties.mergeIn(json.getObject(IJsonable.jsonPropertiesFieldName, new JsonObject()));
        if (!properties.containsField(loadBalancePolicyFieldName)) {
            getLoadBalancePolicy();
        }
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
        return addBackend(new JsonObject().putString(IJsonable.jsonIdFieldName, backend), backendOk);
    }

    public boolean addBackend(JsonObject backendJson, boolean backendOk) {
        updateModifiedTimestamp();
        Backend backend = new Backend(backendJson, vertx);
        backend.setQueueService(queueService);
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
        properties.putString(Virtualhost.loadBalancePolicyFieldName, loadBalancePolicyName);
        return this;
    }

    public ILoadBalancePolicy getLoadBalancePolicy() {
        String loadBalancePolicyStr = properties.getString(loadBalancePolicyFieldName,
                DefaultLoadBalancePolicy.class.getSimpleName());
        loadbalancePolicy = loadBalancePolicyClassLoader(loadBalancePolicyStr);
        if (loadbalancePolicy.isDefault()) {
            properties.putString(loadBalancePolicyFieldName, loadbalancePolicy.toString());
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

        JsonObject backends = new JsonObject();

        backends.putArray(backendsElegibleFieldName, backendsElegiblesJson);
        backends.putArray(backendsFailedFieldName, backendsFailedJson);

        idObj.putObject(backendsFieldName, backends);

        return super.toJson();
    }

    public void setTransientState() {
        idObj.putBoolean(transientStateFieldName, true);
    }

    private void unSetTransientState() {
        idObj.putBoolean(transientStateFieldName, false);
    }
}
