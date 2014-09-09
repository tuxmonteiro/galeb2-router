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

import static com.globo.galeb.core.Constants.*;

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

    private final String                  virtualhostName;
    private final UniqueArrayList<Backend> backends;
    private final UniqueArrayList<Backend> badBackends;
    private final Vertx                   vertx;

    private ILoadBalancePolicy connectPolicy     = null;
    private ILoadBalancePolicy persistencePolicy = null;

    public Virtualhost(String virtualhostName, final Vertx vertx) {
        super();
        this.virtualhostName = virtualhostName;
        this.backends = new UniqueArrayList<Backend>();
        this.badBackends = new UniqueArrayList<Backend>();
        this.vertx = vertx;
    }

    public Virtualhost(JsonObject json, final Vertx vertx) {
        this(json.getString("name", "UNDEF"), vertx);
        JsonObject properties = json.getObject("properties");
        mergeIn(properties);
    }

    @Override
    public String toString() {
        return getVirtualhostName();
    }

    public boolean addBackend(String backend, boolean backendOk) {
        String[] backendWithPort = backend.split(":");
        String host = backendWithPort[0];
        int port = 0;
        if (backendWithPort.length>1) {
            port = Integer.parseInt(backendWithPort[1]);
        }
        return addBackend(new JsonObject()
                    .putString("host", host)
                    .putNumber("port", port),
                backendOk);
    }

    public boolean addBackend(JsonObject backendJson, boolean backendOk) {
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
        if (backendOk) {
            putBoolean(transientStateFieldName, true);
            return backends.remove(new Backend(backend, vertx));
        } else {
            return badBackends.remove(new Backend(backend, vertx));
        }
    }

    public void clear(boolean backendOk) {
        if (backendOk) {
            backends.clear();
            putBoolean(transientStateFieldName, true);
        } else {
            badBackends.clear();
        }
    }

    public void clearAll() {
        backends.clear();
        badBackends.clear();
        putBoolean(transientStateFieldName, true);
    }

    public Backend getChoice(RequestData requestData) {
        // Default: isNewConnection = true
        return getChoice(requestData, true);
    }

    public Backend getChoice(RequestData requestData, boolean isNewConnection) {
        requestData.setProperties(this);
        Backend chosen;
        if (isNewConnection) {
            if (connectPolicy==null) {
                getLoadBalancePolicy();
            }
            chosen = connectPolicy.getChoice(backends, requestData);
        } else {
            if (persistencePolicy==null) {
                getPersistencePolicy();
            }
            chosen = persistencePolicy.getChoice(backends, requestData);
        }
        putBoolean(transientStateFieldName, false);
        return chosen;
    }

    public ILoadBalancePolicy getLoadBalancePolicy() {
        String loadBalancePolicyStr = getString(loadBalancePolicyFieldName, defaultLoadBalancePolicy);
        connectPolicy = loadBalancePolicyClassLoader(loadBalancePolicyStr);
        if (connectPolicy.isDefault()) {
            putString(loadBalancePolicyFieldName, connectPolicy.toString());
        }
        return connectPolicy;
    }

    public ILoadBalancePolicy getPersistencePolicy() {
        String persistencePolicyStr = getString(persistencePolicyFieldName, defaultLoadBalancePolicy);
        persistencePolicy = loadBalancePolicyClassLoader(persistencePolicyStr);
        if (persistencePolicy.isDefault()) {
            putString(persistencePolicyFieldName, persistencePolicy.toString());
        }
        return persistencePolicy;
    }

    public ILoadBalancePolicy loadBalancePolicyClassLoader(String loadBalancePolicyName) {
        try {

            @SuppressWarnings("unchecked")
            Class<ILoadBalancePolicy> classLoader = (Class<ILoadBalancePolicy>) Class.forName(
                            String.format("%s.%s", packageOfLoadBalancePolicyClasses, loadBalancePolicyName));
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

        virtualhostJson.putString("name", getVirtualhostName());
        virtualhostJson.putObject("properties", propertiesJson);
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
        virtualhostJson.putArray("backends", backendsJson);
        virtualhostJson.putArray("badBackends", badBackendsJson);

        return virtualhostJson;
    }

}
