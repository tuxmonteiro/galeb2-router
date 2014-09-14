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

import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class QueueMap {

    public enum ACTION {

        ADD("route.add"),
        DEL("route.del"),
        SET_VERSION("route.version");

        private String queue;
        private ACTION(final String queue) {
            this.queue = queue;
        }

        @Override
        public String toString() {
            return queue;
        }

    }

    private final Verticle verticle;
    private final Vertx vertx;
    private final EventBus eb;
    private final Logger log;
    private final Map<String, Virtualhost> virtualhosts;


    public static String buildMessage(String virtualhostStr,
                                      String backendStr,
                                      String uriStr,
                                      String properties)
    {
        JsonObject messageJson = new JsonObject();
        JsonObject virtualhostObj = new JsonObject().putString(Serializable.jsonIdFieldName, virtualhostStr);

        try {
            virtualhostObj.putObject(Serializable.jsonPropertiesFieldName, new JsonObject(properties));
        } catch (DecodeException ignoreBadJson) {
            virtualhostObj.putObject(Serializable.jsonPropertiesFieldName, new JsonObject());
        }
        messageJson.putString("virtualhost", virtualhostObj.encode());
        messageJson.putString("backend", backendStr);
        messageJson.putString("uri", uriStr);

        return messageJson.toString();
    }

    public QueueMap(final Verticle verticle, final Map<String, Virtualhost> virtualhosts) {
        this.verticle = verticle;
        this.vertx = (verticle != null) ? verticle.getVertx() : null;
        this.eb=(verticle != null) ? verticle.getVertx().eventBus() : null;
        this.log=(verticle != null) ? verticle.getContainer().logger() : null;
        this.virtualhosts=virtualhosts;
    }

    public boolean processAddMessage(String message) {
        if (virtualhosts==null) {
            return false;
        }

        boolean isOk = true;
        JsonObject messageJson = new JsonObject(message);
        JsonObject virtualhostJson = new JsonObject(messageJson.getString("virtualhost", "{}"));
        String virtualhost = virtualhostJson.getString(Serializable.jsonIdFieldName, "");
        String backendStr = messageJson.getString("backend", "{}");
        JsonObject backend = new JsonObject(backendStr);
        String uri = messageJson.getString("uri", "");
        String uriBase = uri.split("/")[1];

        switch (uriBase) {
            case "virtualhost":
                if (!virtualhosts.containsKey(virtualhost)) {
                    Virtualhost newVirtualhostObj = new Virtualhost(virtualhostJson, vertx);
                    virtualhosts.put(virtualhost, newVirtualhostObj);
                    log.info(String.format("[%s] Virtualhost %s added", verticle.toString(), virtualhost));
                    isOk = true;
                } else {
                    isOk = false;
                }
                break;
            case "backend":
                if (!virtualhosts.containsKey(virtualhost)) {
                    log.warn(String.format("[%s] Backend not created, because Virtualhost %s not exist", verticle.toString(), virtualhost));
                    isOk = false;
                } else {

                    String hostWithPort = backend.getString(Serializable.jsonIdFieldName, "");
                    boolean status = backend.getBoolean("status", true);

                    final Virtualhost vhost = virtualhosts.get(virtualhost);
                    if (vhost.addBackend(backend, status)) {
                        log.info(String.format("[%s] Backend %s (%s) added", verticle.toString(), hostWithPort, virtualhost));
                    } else {
                        log.warn(String.format("[%s] Backend %s (%s) already exist", verticle.toString(), hostWithPort, virtualhost));
                        isOk = false;
                    }
                }
                break;
            default:
                log.warn(String.format("[%s] uriBase %s not supported", verticle.toString(), uriBase));
                isOk = false;
                break;
        }
        return isOk;
    }

    public boolean processDelMessage(String message) {
        if (virtualhosts==null) {
            return false;
        }

        boolean isOk = true;
        JsonObject messageJson = new JsonObject(message);
        JsonObject virtualhostJson = new JsonObject(messageJson.getString("virtualhost", "{}"));
        String virtualhost = virtualhostJson.getString(Serializable.jsonIdFieldName, "");
        String backendStr = messageJson.getString("backend", "{}");
        JsonObject backend = new JsonObject(backendStr);
        boolean status = !"0".equals(messageJson.getString("status", ""));
        String uri = messageJson.getString("uri", "");

        String uriBase = uri.split("/")[1];

        switch (uriBase) {
            case "virtualhost":
                if (virtualhosts.containsKey(virtualhost)) {
                    virtualhosts.get(virtualhost).clearAll();
                    virtualhosts.remove(virtualhost);
                    log.info(String.format("[%s] Virtualhost %s removed", verticle.toString(), virtualhost));
                } else {
                    log.warn(String.format("[%s] Virtualhost not removed. Virtualhost %s not exist", verticle.toString(), virtualhost));
                    isOk = false;
                }
                break;
            case "backend":
                String backendId = backend.getString(Serializable.jsonIdFieldName);
                if ("".equals(backendId)) {
                    log.warn(String.format("[%s] Backend UNDEF", verticle.toString()));
                    isOk = false;
                } else if (!virtualhosts.containsKey(virtualhost)) {
                    log.warn(String.format("[%s] Backend not removed. Virtualhost %s not exist", verticle.toString(), virtualhost));
                    isOk = false;
                } else {
                    final Virtualhost virtualhostObj = virtualhosts.get(virtualhost);
                    if (virtualhostObj!=null && virtualhostObj.removeBackend(backend.getString(Serializable.jsonIdFieldName), status)) {
                        log.info(String.format("[%s] Backend %s (%s) removed", verticle.toString(), backendId, virtualhost));
                    } else {
                        log.warn(String.format("[%s] Backend not removed. Backend %s (%s) not exist", verticle.toString(), backendId, virtualhost));
                        isOk = false;
                    }
                }
                break;
            default:
                log.warn(String.format("[%s] uriBase %s not supported", verticle.toString(), uriBase));
                isOk = false;
                break;
        }
        return isOk;
    }

    public void processVersionMessage(String message) {
        try {
            setVersion(Long.parseLong(message));
        } catch (java.lang.NumberFormatException e) {}
    }

    public void register() {
        registerQueueAdd();
        registerQueueDel();
        registerQueueVersion();
    }

    private void registerQueueAdd() {
        Handler<Message<String>> queueAddHandler = new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                processAddMessage(message.body());
                postAddEvent(message.body());
            }
        };
        if (eb != null) {
            eb.registerHandler(ACTION.ADD.toString(), queueAddHandler);
        }
    }

    private void registerQueueDel() {
        Handler<Message<String>> queueDelHandler =  new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                processDelMessage(message.body());
                postDelEvent(message.body());
            }
        };
        if (eb!=null) {
            eb.registerHandler(ACTION.DEL.toString(),queueDelHandler);
        }
    }

    private void registerQueueVersion() {
        Handler<Message<String>> queueVersionHandler = new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                processVersionMessage(message.body());
            }
        };
        if (eb!=null) {
            eb.registerHandler(ACTION.SET_VERSION.toString(), queueVersionHandler);
        }
    }

    private void setVersion(Long version) {
        if (verticle instanceof IEventObserver) {
            ((IEventObserver)verticle).setVersion(version);
            log.info(String.format("[%s] POST /version: %d", verticle.toString(), version));
        }
    }

    private void postDelEvent(String message) {
        if (verticle instanceof IEventObserver) {
            ((IEventObserver)verticle).postDelEvent(message);
        }
    }

    private void postAddEvent(String message) {
        if (verticle instanceof IEventObserver) {
            ((IEventObserver)verticle).postAddEvent(message);
        }
    }
}
