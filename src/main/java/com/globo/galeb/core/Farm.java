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

import static com.globo.galeb.core.bus.QueueMap.ACTION.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import com.globo.galeb.core.bus.IEventObserver;
import com.globo.galeb.core.bus.MessageToMapBuilder;

public class Farm extends Entity {

    private final Map<String, Virtualhost> virtualhosts = new HashMap<>();
    private Long version = 0L;
    private final Verticle verticle;
    private final Logger log;
    private final EventBus eb;

    public Farm(final Verticle verticle) {
        this.id = "";
        this.verticle = verticle;
        if (verticle!=null) {
            properties.mergeIn(verticle.getContainer().config());
            this.eb = verticle.getVertx().eventBus();
            this.log = verticle.getContainer().logger();
            register();
        } else {
            this.eb = null;
            this.log = null;
        }

    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
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

    public boolean addToMap(String message) {
        return MessageToMapBuilder.getInstance(message, this).add();
    }

    public boolean delFromMap(String message) {
        return MessageToMapBuilder.getInstance(message, this).del();
    }

    public void registerQueueAdd() {
        Handler<Message<String>> addHandler = new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                addToMap(message.body());

                if (verticle != null && verticle instanceof IEventObserver) {
                    ((IEventObserver)verticle).postAddEvent(message.body());
                }
            }
        };
        if (eb!=null) {
            eb.registerHandler(ADD.toString(), addHandler);
        } else {
            if (log!=null) log.warn("registerQueueAdd is not possible: EventBus is null");
        }
    }

    public void registerQueueDel() {
        Handler<Message<String>> queueDelHandler =  new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                delFromMap(message.body());
                if (verticle != null && verticle instanceof IEventObserver) {
                    ((IEventObserver)verticle).postDelEvent(message.body());
                }
            }
        };
        if (eb!=null) {
            eb.registerHandler(DEL.toString(),queueDelHandler);
        } else {
            Logger log = verticle.getContainer().logger();
            if (log!=null) log.warn("registerQueueDel is not possible: EventBus is null");
        }
    }

    public void registerQueueVersion() {
        Handler<Message<String>> queueVersionHandler = new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                try {
                    setVersion(Long.parseLong(message.body()));
                } catch (java.lang.NumberFormatException ignore) {
                    // not change version
                }
            }
        };
        if (eb!=null) {
            eb.registerHandler(SET_VERSION.toString(), queueVersionHandler);
        } else {
            if (log!=null) log.warn("registerQueueVersion is not possible: EventBus is null");
        }
    }

    public void register() {
        registerQueueAdd();
        registerQueueDel();
        registerQueueVersion();
    }

}
