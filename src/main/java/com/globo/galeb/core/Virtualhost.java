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

import com.globo.galeb.list.UniqueArrayList;
import com.globo.galeb.loadbalance.ILoadBalancePolicy;
import com.globo.galeb.loadbalance.impl.DefaultLoadBalancePolicy;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class Virtualhost extends JsonObject implements Serializable {

    private static final long serialVersionUID = -3715150640575829972L;

    public static final String backendsFieldName          = "backends";
    public static final String badBackendsFieldName       = "badBackends";
    public static final String transientStateFieldName    = "transientState";
    public static final String loadBalancePolicyFieldName = "loadBalancePolicy";

    private final String                   virtualhostName;
    private final UniqueArrayList<Backend> backends;
    private final UniqueArrayList<Backend> badBackends;
    private final Vertx                    vertx;

    private ILoadBalancePolicy loadbalancePolicy     = null;

    private final Long createdAt = System.currentTimeMillis();
    private Long modifiedAt = System.currentTimeMillis();

    private void updateModifiedTimestamp() {
        modifiedAt = System.currentTimeMillis();
    }

    public Virtualhost(JsonObject json, final Vertx vertx) {
        super();
        this.virtualhostName = json.getString(jsonIdFieldName, "UNDEF");
        this.backends = new UniqueArrayList<Backend>();
        this.badBackends = new UniqueArrayList<Backend>();
        this.vertx = vertx;
        mergeIn(json.getObject(jsonPropertiesFieldName));
        if (!this.containsField(loadBalancePolicyFieldName)) {
            getLoadBalancePolicy();
        }
    }

    @Override
    public String toString() {
        return getVirtualhostName();
    }

    public boolean addBackend(String backend, boolean backendOk) {
        return addBackend(new JsonObject().putString(jsonIdFieldName, backend), backendOk);
    }

    public boolean addBackend(JsonObject backendJson, boolean backendOk) {
        updateModifiedTimestamp();
        if (backendOk) {
            putBoolean(transientStateFieldName, true);
            return backends.add(new Backend(backendJson, vertx));
        } else {
            return badBackends.add(new Backend(backendJson, vertx));
        }
    }

    public UniqueArrayList<Backend> getBackends(boolean backendOk) {
        return backendOk ? backends: badBackends;
    }

    public String getVirtualhostName() {
        return virtualhostName;
    }

    public Boolean removeBackend(String backend, boolean backendOk) {
        updateModifiedTimestamp();
        if (backendOk) {
            putBoolean(transientStateFieldName, true);
            return backends.remove(new Backend(backend, vertx));
        } else {
            return badBackends.remove(new Backend(backend, vertx));
        }
    }

    public void clear(boolean backendOk) {
        updateModifiedTimestamp();
        if (backendOk) {
            backends.clear();
            putBoolean(transientStateFieldName, true);
        } else {
            badBackends.clear();
        }
    }

    public void clearAll() {
        updateModifiedTimestamp();
        backends.clear();
        badBackends.clear();
        putBoolean(transientStateFieldName, true);
    }

    public Backend getChoice(RequestData requestData) {
        requestData.setProperties(this);
        Backend chosen;
        if (loadbalancePolicy==null) {
            getLoadBalancePolicy();
        }
        chosen = loadbalancePolicy.getChoice(backends, requestData);
        putBoolean(transientStateFieldName, false);
        return chosen;
    }

    public ILoadBalancePolicy getLoadBalancePolicy() {
        String loadBalancePolicyStr = getString(loadBalancePolicyFieldName,
                DefaultLoadBalancePolicy.class.getSimpleName());
        loadbalancePolicy = loadBalancePolicyClassLoader(loadBalancePolicyStr);
        if (loadbalancePolicy.isDefault()) {
            putString(loadBalancePolicyFieldName, loadbalancePolicy.toString());
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

        JsonObject virtualhostJson = new JsonObject();
        JsonObject propertiesJson = new JsonObject(this.encode());
        JsonArray backendsJson = new JsonArray();
        JsonArray badBackendsJson = new JsonArray();

        virtualhostJson.putString(jsonIdFieldName, getVirtualhostName());
        virtualhostJson.putNumber(jsonCreatedAtFieldName, createdAt);
        virtualhostJson.putNumber(jsonModifiedAtFieldName, modifiedAt);
        virtualhostJson.putObject(jsonPropertiesFieldName, propertiesJson);

        for (Backend backend: backends) {
            if (backend!=null) {
                backendsJson.addObject(backend.toJson());
            }
        }
        for (Backend badBackend: badBackends) {
            if (badBackend!=null) {
                badBackendsJson.addObject(badBackend.toJson());
            }
        }
        virtualhostJson.putArray(backendsFieldName, backendsJson);
        virtualhostJson.putArray(badBackendsFieldName, badBackendsJson);

        return virtualhostJson;
    }

}
