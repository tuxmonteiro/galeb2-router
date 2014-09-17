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

import java.util.Iterator;
import java.util.Map;

import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class QueueMap {

    private enum ACTION {

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
        MessageBus messageBus = new MessageBus(message);
        String uriBase = messageBus.getUriBase();
        SafeJsonObject entity = messageBus.getEntity();
        String entityId = messageBus.getEntityId();
        String parentId = messageBus.getParentId();

        if ("".equals(entityId)) {
            log.error(String.format("[%s] Inaccessible Entity Id: %s", verticle.toString(), entity.encode()));
            return false;
        }

        switch (uriBase) {
            case "virtualhost":
                if (!virtualhosts.containsKey(entityId)) {
                    Virtualhost newVirtualhostObj = new Virtualhost(entity, vertx);
                    virtualhosts.put(entityId, newVirtualhostObj);
                    log.info(String.format("[%s] Virtualhost %s added", verticle.toString(), entityId));
                    isOk = true;
                } else {
                    isOk = false;
                }
                break;
            case "backend":
                if ("".equals(parentId)) {
                    log.error(String.format("[%s] Inaccessible ParentId: %s", verticle.toString(), entity.encode()));
                    return false;
                }
                if (!virtualhosts.containsKey(parentId)) {
                    log.warn(String.format("[%s] Backend not created, because Virtualhost %s not exist", verticle.toString(), parentId));
                    isOk = false;
                } else {
                    boolean status = entity.getBoolean(Backend.propertyStatusFieldName, true);

                    final Virtualhost vhost = virtualhosts.get(parentId);
                    if (vhost.addBackend(entity, status)) {
                        log.info(String.format("[%s] Backend %s (%s) added", verticle.toString(), entityId, parentId));
                    } else {
                        log.warn(String.format("[%s] Backend %s (%s) already exist", verticle.toString(), entityId, parentId));
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
        MessageBus messageBus = new MessageBus(message);
        String uriBase = messageBus.getUriBase();
        SafeJsonObject entity = messageBus.getEntity();
        String entityId = messageBus.getEntityId();
        String parentId = messageBus.getParentId();

        if ("".equals(entityId)) {
            log.error(String.format("[%s] Inaccessible Entity Id: %s", verticle.toString(), entity.encode()));
            return false;
        }

        switch (uriBase) {
            case "virtualhost":
                if (virtualhosts.containsKey(entityId)) {
                    virtualhosts.get(entityId).clearAll();
                    virtualhosts.remove(entityId);
                    log.info(String.format("[%s] Virtualhost %s removed", verticle.toString(), entityId));
                } else {
                    log.warn(String.format("[%s] Virtualhost not removed. Virtualhost %s not exist", verticle.toString(), entityId));
                    isOk = false;
                }
                break;
            case "backend":
                if ("".equals(parentId)) {
                    log.error(String.format("[%s] Inaccessible ParentId: %s", verticle.toString(), entity.encode()));
                    return false;
                }
                boolean status = entity.getBoolean(Backend.propertyStatusFieldName, true);

                if ("".equals(entityId)) {
                    log.warn(String.format("[%s] Backend UNDEF", verticle.toString()));
                    isOk = false;
                } else if (!virtualhosts.containsKey(parentId)) {
                    log.warn(String.format("[%s] Backend not removed. Virtualhost %s not exist", verticle.toString(), parentId));
                    isOk = false;
                } else {
                    final Virtualhost virtualhostObj = virtualhosts.get(parentId);
                    if (virtualhostObj!=null && virtualhostObj.removeBackend(entityId, status)) {
                        log.info(String.format("[%s] Backend %s (%s) removed", verticle.toString(), entityId, parentId));
                    } else {
                        log.warn(String.format("[%s] Backend not removed. Backend %s (%s) not exist", verticle.toString(), entityId, parentId));
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

    public void sendActionAdd(SafeJsonObject json, final String uri) {
        putMessageToBus(json, ACTION.ADD, uri);
    }

    public void sendActionDel(SafeJsonObject json, final String uri) {
        putMessageToBus(json, ACTION.DEL, uri);
    }

    private void putMessageToBus(SafeJsonObject json, QueueMap.ACTION action, final String uri) {
        Long timestamp = 0L;

        try {
            timestamp = json.getLong("version");
        } catch (DecodeException e) {
            log.error(e.getMessage());
            return;
        }
        json.removeField("version");

        String parentId = json.getString(MessageBus.parentIdFieldName, "");
        json.removeField(parentId);

        MessageBus messageBus = new MessageBus()
                                    .setUri(uri)
                                    .setEntity(json.encode());

        if (!"".equals(parentId)) {
            messageBus.setParentId(parentId);
        }

        String message = messageBus.make().toString();

        sendAction(message, action);
        sendAction(String.format("%d", timestamp), ACTION.SET_VERSION);

    }

    public void sendGroupActionAdd(SafeJsonObject json, final String uri) {
        putGroupMessageToBus(json, ACTION.ADD, uri);
    }

    public void sendGroupActionDel(SafeJsonObject json, final String uri) {
        putGroupMessageToBus(json, ACTION.DEL, uri);
    }

    private void putGroupMessageToBus(final SafeJsonObject json, final QueueMap.ACTION action, final String uri) throws RuntimeException {
        SafeJsonObject virtualhosts = new SafeJsonObject().makeArray(json.getArray("virtualhosts"));
        Long timestamp = json.getLong("version", 0L);
        String message = "{}";

        Iterator<Object> it = virtualhosts.toList().iterator();
        while (it.hasNext()) {
            SafeJsonObject vhostJson = (SafeJsonObject) it.next();
            String vhostId = vhostJson.getString(IJsonable.jsonIdFieldName);

            message = new MessageBus()
                .setEntity(vhostJson)
                .setUri("/virtualhost")
                .make()
                .toString();

            sendAction(message, action);

            if (vhostJson.containsField(Virtualhost.backendsFieldName)) {
                SafeJsonObject backends = vhostJson.getObject(Virtualhost.backendsFieldName)
                        .getJsonArray(Virtualhost.backendsElegibleFieldName);

                Iterator<Object> backendsIterator = backends.toList().iterator();
                while (backendsIterator.hasNext()) {
                    SafeJsonObject backendJson = (SafeJsonObject) backendsIterator.next();

                    message = new MessageBus()
                                            .setEntity(backendJson)
                                            .setParentId(vhostId)
                                            .setUri("/backend")
                                            .make()
                                            .toString();

                    sendAction(message, action);
                }
            }
        }
        sendAction(String.format("%d", timestamp), ACTION.SET_VERSION);
    }

    private void sendAction(String message, final QueueMap.ACTION action) {
        eb.publish(action.toString(), message);
        log.debug(String.format("Sending %s to %s",message, action.toString()));
    }

}
