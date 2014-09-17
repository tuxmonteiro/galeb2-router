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

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class Farm extends Serializable {

    private final Map<String, Virtualhost> virtualhosts = new HashMap<>();
    private Long version = 0L;
    private final QueueMap queueMap;

    public Farm(final Verticle verticle) {
        this.id = "";
        this.queueMap = new QueueMap(verticle, virtualhosts);
        queueMap.register();
        if (verticle!=null) properties.mergeIn(verticle.getContainer().config());
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public QueueMap getQueueMap() {
        return queueMap;
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

}
