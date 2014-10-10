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

import static com.globo.galeb.core.bus.IQueueService.ACTION.*;

import java.io.UnsupportedEncodingException;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import com.globo.galeb.core.SafeJsonObject;

public class VertxQueueService implements IQueueService {

    public final EventBus eb;
    private final Logger log;

    public VertxQueueService(final EventBus eb, final Logger log) {
        this.eb=eb;
        this.log=log;
    }

    @Override
    public void queueToAdd(SafeJsonObject json, final String uri) {
        putMessageToQueue(json, IQueueService.ACTION.ADD, uri);
    }

    @Override
    public void queueToDel(SafeJsonObject json, final String uri) {
        putMessageToQueue(json, IQueueService.ACTION.DEL, uri);
    }

    @Override
    public void queueToChange(SafeJsonObject json, final String uri) {
        // putMessageToQueue(json, ACTION.CHANGE, uri);
        String messageLog = String.format("%s: Change not implemented", this.toString());
        if (log!=null) {
            log.warn(messageLog);
        } else {
            System.out.println(messageLog);
        }
    }

    private void putMessageToQueue(SafeJsonObject json, IQueueService.ACTION action, final String uri) {
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
        publishAction(String.format("%d", version), IQueueService.ACTION.SET_VERSION);

    }

    private void publishAction(String message, final IQueueService.ACTION action) {
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

    @Override
    public void registerHealthcheck(final ICallbackHealthcheck callbackHealthcheck) {
        if (eb==null) {
            return;
        }
        eb.registerHandler(IQueueService.QUEUE_HEALTHCHECK_OK, new Handler<Message<String>>() {
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
        eb.registerHandler(IQueueService.QUEUE_HEALTHCHECK_FAIL, new Handler<Message<String>>() {
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

    @Override
    public void publishBackendOk(String backend) {
        if (eb==null) {
            return;
        }
        eb.publish(IQueueService.QUEUE_HEALTHCHECK_OK, backend);
        log.info(String.format("Backend %s OK. Enabling it", backend));
    }

    @Override
    public void publishBackendFail(String backend) {
        if (eb==null) {
            return;
        }
        eb.publish(IQueueService.QUEUE_HEALTHCHECK_FAIL, backend);
        log.info(String.format("Backend %s Fail. disabling it", backend));
    }

    @Override
    public void publishBackendConnections(String queueActiveConnections, SafeJsonObject myConnections) {
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

    @Override
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

    @Override
    public void publishActiveConnections(String queueActiveConnections,
            SafeJsonObject myConnections) {
        if (eb==null) {
            return;
        }
        eb.publish(queueActiveConnections, myConnections);
    }

    @Override
    public void unregisterConnectionsCounter(final ICallbackConnectionCounter connectionsCounter,
            String queueActiveConnections) {
        if (connectionsCounter.isRegistered() && eb!=null) {
            eb.unregisterHandler(queueActiveConnections, connectionCounterHandler(connectionsCounter));
            connectionsCounter.setRegistered(false);
        }
    }

    private void logEventBusNull() {
        String logMessage = "registerQueueAdd is not possible: EventBus is null";
        if (log!=null)  {
            log.warn(logMessage);
        } else {
            System.err.println(logMessage);
        }
    }

    private void logQueueRegistered(String starter, String queue) {
        String message = String.format("[%s] %s registered", starter, queue);
        if (log!=null) {
            log.info(message);
        } else {
            System.err.println(message);
        }
    }

    @Override
    public void registerQueueAdd(final Object starter, final ICallbackQueueAction callbackQueueAction) {
        final Verticle verticle;
        if (starter instanceof Verticle) {
            verticle = (Verticle)starter;
        } else {
            log.error("Starter is not instanceof Verticle");
            return;
        }
        Handler<Message<String>> addHandler = new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                log.debug(String.format("Received %s at %s queue", message.body(), ADD.toString()));
                callbackQueueAction.addToMap(message.body());

                if (verticle != null && verticle instanceof IEventObserver) {
                    ((IEventObserver)verticle).postAddEvent(message.body());
                }
            }
        };
        if (eb!=null) {
            eb.registerHandler(ADD.toString(), addHandler);
            logQueueRegistered(starter.toString(), ADD.toString());
        } else {
            logEventBusNull();
        }
    }

    @Override
    public void registerQueueDel(final Object starter, final ICallbackQueueAction callbackQueueAction) {
        final Verticle verticle;
        if (starter instanceof Verticle) {
            verticle = (Verticle)starter;
        } else {
            log.error("Starter is not instanceof Verticle");
            return;
        }
        Handler<Message<String>> queueDelHandler =  new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                log.debug(String.format("Received %s at %s queue", message.body(), DEL.toString()));
                callbackQueueAction.delFromMap(message.body());
                if (verticle != null && verticle instanceof IEventObserver) {
                    ((IEventObserver)verticle).postDelEvent(message.body());
                }
            }
        };
        if (eb!=null) {
            eb.registerHandler(DEL.toString(),queueDelHandler);
            logQueueRegistered(starter.toString(), DEL.toString());
        } else {
            logEventBusNull();
        }
    }

    @Override
    public void registerQueueVersion(final Object starter, final ICallbackQueueAction callbackQueueAction) {
        Handler<Message<String>> queueVersionHandler = new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                log.debug(String.format("Received %s at %s queue", message.body(), SET_VERSION.toString()));
                try {
                    callbackQueueAction.setVersion(Long.parseLong(message.body()));
                } catch (java.lang.NumberFormatException ignore) {
                    // not change version
                }
            }
        };
        if (eb!=null) {
            eb.registerHandler(SET_VERSION.toString(), queueVersionHandler);
            logQueueRegistered(starter.toString(), SET_VERSION.toString());
        } else {
            logEventBusNull();
        }
    }

    @Override
    public void registerUpdateSharedData(final Object starter,
            final ICallbackSharedData callbackSharedData) {
        Handler<Message<String>> queueUpdateSharedData = new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> ignore) {
                callbackSharedData.updateSharedData();
            }
        };
        if (eb!=null) {
            eb.registerLocalHandler(SHARED_DATA.toString(), queueUpdateSharedData);
            logQueueRegistered(starter.toString(), SHARED_DATA.toString());
        } else {
            logEventBusNull();
        }
    }

    @Override
    public void updateSharedData() {
        if (eb!=null) {
            eb.publish(SHARED_DATA.toString(), "update");
        } else {
            logEventBusNull();
        }
    }

    @Override
    public void notifyBackendFail(String backendId) {
        if (eb!=null) {
            eb.publish(IQueueService.QUEUE_HEALTHCHECK_FAIL, backendId);
        } else {
            logEventBusNull();
        }
    }
}
