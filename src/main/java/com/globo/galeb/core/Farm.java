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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import com.globo.galeb.core.bus.ICallbackQueueAction;
import com.globo.galeb.core.bus.ICallbackSharedData;
import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.core.bus.MessageToMap;
import com.globo.galeb.core.bus.MessageToMapBuilder;
import com.globo.galeb.verticles.RouterVerticle;

public class Farm extends Entity implements ICallbackQueueAction, ICallbackSharedData {

    private final Map<String, Virtualhost> virtualhosts = new HashMap<>();
    private Long version = 0L;
    private final Verticle verticle;
    private final Logger log;
    private final IQueueService queueService;

    private final ConcurrentMap<String, String> sharedMap;


    public Farm(final Verticle verticle, final IQueueService queueService) {
        this.id = "";
        this.verticle = verticle;
        this.queueService = queueService;
        if (verticle!=null) {
            this.sharedMap = verticle.getVertx().sharedData().getMap("farm.sharedData");
            this.sharedMap.put("farm", toJson().encodePrettily());
            this.sharedMap.put("backends", "{}");
            properties.mergeIn(verticle.getContainer().config());
            this.log = verticle.getContainer().logger();
            registerQueueAction();
        } else {
            this.log = null;
            this.sharedMap = null;
        }

    }

    public Long getVersion() {
        return version;
    }

    @Override
    public void setVersion(long version) {
        this.version = version;
        String infoMessage = String.format("Version changed to %d", version);
        if (verticle!=null) {
            log.info(infoMessage);
        } else {
            System.out.println(infoMessage);
        }
    }

    public Vertx getVertx() {
        return (verticle!=null) ? verticle.getVertx() : null;
    }

    public Logger getLogger() {
        return (verticle!=null) ? log : null;
    }

    public String getVerticleId() {
        return (verticle!=null) ? verticle.toString() : "";
    }

    public Set<Backend> getBackends() {
        Set<Backend> backends = new HashSet<>();
        for (Virtualhost virtualhost: virtualhosts.values()) {
            backends.addAll(virtualhost.getBackends(true));
            backends.addAll(virtualhost.getBackends(false));
        }
        return backends;
    }

    public Set<Virtualhost> getVirtualhosts() {
        return new HashSet<Virtualhost>(virtualhosts.values());
    }

    public Virtualhost getVirtualhost(String key) {
        return "".equals(key) ? null : virtualhosts.get(key);
    }

    @Override
    public JsonObject toJson() {
        prepareJson();

        idObj.removeField(jsonStatusFieldName);
        idObj.putNumber("version", version);
        JsonArray virtualhostArray = new JsonArray();

        for (String vhost : virtualhosts.keySet()) {
            Virtualhost virtualhost = virtualhosts.get(vhost);
            if (virtualhost==null) {
                continue;
            }
            virtualhostArray.add(virtualhost.toJson());
        }

        idObj.putArray("virtualhosts", virtualhostArray);
        return super.toJson();
    }

    public Map<String, Virtualhost> getVirtualhostsToMap() {
        return virtualhosts;
    }

    @Override
    public boolean addToMap(String message) {
        @SuppressWarnings("rawtypes")
        MessageToMap messageToMap = MessageToMapBuilder.getInstance(message, this);
        if (properties.containsField(Constants.CONF_STARTER_CONF)) {
            JsonObject starterConf = properties.getObject(Constants.CONF_STARTER_CONF);
            if (starterConf.containsField(Constants.CONF_ROOT_ROUTER)) {
                JsonObject staticConf = starterConf.getObject(Constants.CONF_ROOT_ROUTER);
                return messageToMap.staticConf(staticConf.encode()).add();
            }
        }
        return messageToMap.add();
    }

    @Override
    public boolean delFromMap(String message) {
        return MessageToMapBuilder.getInstance(message, this).del();
    }

    public void registerQueueAction() {
        queueService.registerQueueAdd(verticle, this);
        queueService.registerQueueDel(verticle, this);
        queueService.registerQueueVersion(verticle, this);
        queueService.registerUpdateSharedData(verticle, this);
    }

    public void clearAll() {
        virtualhosts.clear();
    }

    @Override
    public void updateSharedData() {
        if (verticle instanceof RouterVerticle) {
            this.sharedMap.put("farm", toJson().encodePrettily());
            String backendClassName = Backend.class.getSimpleName().toLowerCase();
            this.sharedMap.put("backends", collectionToJson("", getBackends(), backendClassName));
        }
    }

    public String getFarmJson() {
        queueService.updateSharedData();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignore) {}
        return this.sharedMap.get("farm");
    }

    public String getVirtualhostJson(String id) {
        queueService.updateSharedData();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignore) {}
        return getJsonObject(id, this.sharedMap.get("farm"), Virtualhost.class.getSimpleName().toLowerCase());
    }

    public String getBackendJson(String id) {
        queueService.updateSharedData();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignore) {}
        return getJsonObject(id, this.sharedMap.get("backends"), Backend.class.getSimpleName().toLowerCase());
    }

    private String getJsonObject(String key, String jsonCollection, String clazz) {
        if ("".equals(jsonCollection)) {
            return "{}";
        }
        JsonObject json = new JsonObject(jsonCollection);
        JsonArray jsonArray = json.getArray(String.format("%ss", clazz));
        if (jsonArray!=null) {
            if ("".equals(key)) {
                return jsonArray.encodePrettily();
            }
            Iterator<Object> jsonArrayIterator = jsonArray.iterator();
            while (jsonArrayIterator.hasNext()) {
                JsonObject entity = (JsonObject)jsonArrayIterator.next();
                String id = entity.getString(IJsonable.jsonIdFieldName);
                if (id.equals(key)) {
                    return entity.encodePrettily();
                }
            }
            return jsonArray.encodePrettily();
        }
        return "{}";
    }

    public String collectionToJson(String key, Collection<? extends Entity> collection, String clazz) {
        String result = "";
        boolean isArray = false;
        JsonArray entityArray = new JsonArray();

        for (Entity entityObj: collection) {
            entityArray.add(entityObj.toJson());
            if (!"".equals(key)) {
                if (entityObj.toString().equalsIgnoreCase(key)) {
                    result = entityObj.toJson().encodePrettily();
                    break;
                }
            } else {
                isArray = true;
            }
        }
        return !isArray ? result : new JsonObject().putArray(String.format("%ss", clazz), entityArray).encodePrettily();
    }

}
