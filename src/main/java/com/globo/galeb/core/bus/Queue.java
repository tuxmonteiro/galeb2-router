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
package com.globo.galeb.core.bus;

import static com.globo.galeb.core.Constants.QUEUE_HEALTHCHECK_FAIL;
import static com.globo.galeb.core.Constants.QUEUE_HEALTHCHECK_OK;
import static com.globo.galeb.core.bus.Queue.ACTION.ADD;
import static com.globo.galeb.core.bus.Queue.ACTION.DEL;
import static com.globo.galeb.core.bus.Queue.ACTION.SET_VERSION;

import java.io.UnsupportedEncodingException;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import com.globo.galeb.core.SafeJsonObject;

public class Queue {

    public enum ACTION {

        ADD         ("route.add"),
        DEL         ("route.del"),
        SET_VERSION ("route.version");

        private String queue;
        private ACTION(final String queue) {
            this.queue = queue;
        }
        @Override
        public String toString() {
            return queue;
        }
    }

    public final EventBus eb;
    private final Logger log;

    public Queue(final EventBus eb, final Logger log) {
        this.eb=eb;
        this.log=log;
    }

    public void queueToAdd(SafeJsonObject json, final String uri) {
        putMessageToQueue(json, ACTION.ADD, uri);
    }

    public void queueToDel(SafeJsonObject json, final String uri) {
        putMessageToQueue(json, ACTION.DEL, uri);
    }

    public void queueToChange(SafeJsonObject json, final String uri) {
        // putMessageToQueue(json, ACTION.CHANGE, uri);
        String messageLog = String.format("%s: Change not implemented", this.toString());
        if (log!=null) {
            log.warn(messageLog);
        } else {
            System.out.println(messageLog);
        }
    }

    private void putMessageToQueue(SafeJsonObject json, ACTION action, final String uri) {
        Long version = 0L;

        try {
            version = json.getLong("version");
        } catch (DecodeException e) {
            if (log!=null) {
                log.error(e.getMessage());
            } else {
                System.err.println(e.getMessage());
            }
            return;
        }
        json.removeField("version");

        String parentId = json.getString(MessageBus.parentIdFieldName, "");

        MessageBus messageBus = new MessageBus()
                                    .setUri(uri)
                                    .setEntity(json.encode());

        if (!"".equals(parentId)) {
            messageBus.setParentId(parentId);
        }

        String message = messageBus.make().toString();

        publishAction(message, action);
        publishAction(String.format("%d", version), ACTION.SET_VERSION);

    }

    private void publishAction(String message, final ACTION action) {
        if (eb!=null) {
            eb.publish(action.toString(), message);
        } else {
            System.out.println(String.format("publish at %s queue: %s", action.toString(), message));
        }
        String debugMessage = String.format("Sending %s to %s",message, action.toString());
        if (log!=null) {
            log.debug(debugMessage);
        } else {
            System.out.println(debugMessage);
        }
    }

    public void registerHealthcheck(final ICallbackHealthcheck callbackHealthcheck) {
        if (eb==null) {
            return;
        }
        eb.registerHandler(QUEUE_HEALTHCHECK_OK, new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                String backend = message.body();
                try {
                    callbackHealthcheck.moveBackend(backend, true);
                } catch (UnsupportedEncodingException e) {
                    log.error(e.getMessage());
                }
                log.debug(String.format("Backend %s OK", backend));
            };
        });
        eb.registerHandler(QUEUE_HEALTHCHECK_FAIL, new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                String backend = message.body();
                try {
                    callbackHealthcheck.moveBackend(backend, false);
                } catch (UnsupportedEncodingException e) {
                    log.error(e.getMessage());
                }
                log.error(String.format("Backend %s FAIL", backend));
            };
        });

    }

    public void publishBackendOk(String backend) {
        if (eb==null) {
            return;
        }
        eb.publish(QUEUE_HEALTHCHECK_OK, backend);
        log.info(String.format("Backend %s OK. Enabling it", backend));
    }

    public void publishBackendFail(String backend) {
        if (eb==null) {
            return;
        }
        eb.publish(QUEUE_HEALTHCHECK_FAIL, backend);
        log.info(String.format("Backend %s Fail. disabling it", backend));
    }

    public void publishBackendConnections(String queueActiveConnections, JsonObject myConnections) {
        eb.publish(queueActiveConnections, myConnections);
    }

    private Handler<Message<JsonObject>> connectionCounterHandler(final ICallbackConnectionCounter connectionsCounter) {
        return new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                connectionsCounter.callbackGlobalConnectionsInfo(message.body());
            }
        };
    }
    public void registerConnectionsCounter(final ICallbackConnectionCounter connectionsCounter,
            String queueActiveConnections) {
        if (eb==null) {
            return;
        }
        if (!connectionsCounter.isRegistered()) {
            eb.registerLocalHandler(queueActiveConnections, connectionCounterHandler(connectionsCounter));
            connectionsCounter.setRegistered(true);
        }
    }

    public void publishActiveConnections(String queueActiveConnections,
            JsonObject myConnections) {
        if (eb==null) {
            return;
        }
        eb.publish(queueActiveConnections, myConnections);
    }

    public void unregisterConnectionsCounter(final ICallbackConnectionCounter connectionsCounter,
            String queueActiveConnections) {
        if (connectionsCounter.isRegistered() && eb!=null) {
            eb.unregisterHandler(queueActiveConnections, connectionCounterHandler(connectionsCounter));
            connectionsCounter.setRegistered(false);
        }
    }

    public void registerQueueAdd(final Verticle verticle, final ICallbackQueueAction callbackQueueAction) {
        Handler<Message<String>> addHandler = new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                callbackQueueAction.addToMap(message.body());

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

    public void registerQueueDel(final Verticle verticle, final ICallbackQueueAction callbackQueueAction) {
        Handler<Message<String>> queueDelHandler =  new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                callbackQueueAction.delFromMap(message.body());
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

    public void registerQueueVersion(final Verticle verticle, final ICallbackQueueAction callbackQueueAction) {
        Handler<Message<String>> queueVersionHandler = new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                try {
                    callbackQueueAction.setVersion(Long.parseLong(message.body()));
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

}
