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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import com.globo.galeb.core.bus.ICallbackQueueAction;
import com.globo.galeb.core.bus.MessageToMapBuilder;
import com.globo.galeb.core.bus.Queue;

public class Farm extends Entity implements ICallbackQueueAction {

    private final Map<String, Virtualhost> virtualhosts = new HashMap<>();
    private Long version = 0L;
    private final Verticle verticle;
    private final Logger log;
    private final Queue queue;

    public Farm(final Verticle verticle, final Queue queue) {
        this.id = "";
        this.verticle = verticle;
        this.queue = queue;
        if (verticle!=null) {
            properties.mergeIn(verticle.getContainer().config());
            this.log = verticle.getContainer().logger();
            registerQueueAction();
        } else {
            this.log = null;
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
        return MessageToMapBuilder.getInstance(message, this).add();
    }

    @Override
    public boolean delFromMap(String message) {
        return MessageToMapBuilder.getInstance(message, this).del();
    }

    public void registerQueueAction() {
        queue.registerQueueAdd(verticle, this);
        queue.registerQueueDel(verticle, this);
        queue.registerQueueVersion(verticle, this);
    }

    public void clearAll() {
        virtualhosts.clear();
    }

}
