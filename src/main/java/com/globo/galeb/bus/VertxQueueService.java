/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.globo.galeb.bus;

import static com.globo.galeb.bus.IQueueService.ACTION.*;

import java.io.UnsupportedEncodingException;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.logger.SafeLogger;

/**
 * Class VertxQueueService.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class VertxQueueService implements IQueueService {

    /** The Vertx EventBus. */
    private final EventBus eb;

    /** The logger. */
    private final SafeLogger log;

    /**
     * Instantiates a new vertxQueueService.
     *
     * @param eb the eb
     * @param log the log
     */
    public VertxQueueService(final EventBus eb, final SafeLogger log) {
        this.eb=eb;
        this.log=log;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#queueToAdd(com.globo.galeb.core.SafeJsonObject, java.lang.String)
     */
    @Override
    public void queueToAdd(JsonObject json, final String uri) {
        putMessageToQueue(json, IQueueService.ACTION.ADD, uri);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#queueToDel(com.globo.galeb.core.SafeJsonObject, java.lang.String)
     */
    @Override
    public void queueToDel(JsonObject json, final String uri) {
        putMessageToQueue(json, IQueueService.ACTION.DEL, uri);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#queueToChange(com.globo.galeb.core.SafeJsonObject, java.lang.String)
     */
    @Override
    public void queueToChange(JsonObject json, final String uri) {
        // putMessageToQueue(json, ACTION.CHANGE, uri);
        String messageLog = String.format("%s: Change not implemented", this.toString());
        if (log!=null) {
            log.warn(messageLog);
        } else {
            System.out.println(messageLog);
        }
    }

    /**
     * Put message to queue.
     *
     * @param json the json
     * @param action the action
     * @param uri the uri
     */
    private void putMessageToQueue(JsonObject json, IQueueService.ACTION action, final String uri) {
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

        String parentId = json.getString(MessageBus.PARENT_ID_FIELDNAME, "");

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

    /**
     * Publish action.
     *
     * @param message the message
     * @param action the action
     */
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

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#registerHealthcheck(com.globo.galeb.core.bus.ICallbackHealthcheck)
     */
    @Override
    public void registerHealthcheck(final ICallbackHealthcheck callbackHealthcheck) {
        if (eb==null) {
            return;
        }
        eb.registerHandler(IQueueService.QUEUE_HEALTHCHECK_OK, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject backend = message.body();
                try {
                    callbackHealthcheck.moveBackend(backend, true);
                } catch (UnsupportedEncodingException e) {
                    log.error(e.getMessage());
                }
                log.debug(String.format("Backend %s OK", backend));
            };
        });
        eb.registerHandler(IQueueService.QUEUE_HEALTHCHECK_FAIL, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                JsonObject backend = message.body();
                try {
                    callbackHealthcheck.moveBackend(backend, false);
                } catch (UnsupportedEncodingException e) {
                    log.error(e.getMessage());
                }
                log.error(String.format("Backend %s FAIL", backend));
            };
        });

    }

    /* (non-Javadoc)
     * @see com.globo.galeb.bus.IQueueService#publishBackendOk(org.vertx.java.core.json.JsonObject)
     */
    @Override
    public void publishBackendOk(JsonObject backend) {
        if (eb==null) {
            return;
        }
        String backendId = backend.getString(IJsonable.ID_FIELDNAME);
        eb.publish(IQueueService.QUEUE_HEALTHCHECK_OK, backend);
        log.info(String.format("Backend %s OK. Enabling it", backendId));
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.bus.IQueueService#publishBackendFail(org.vertx.java.core.json.JsonObject)
     */
    @Override
    public void publishBackendFail(JsonObject backend) {
        if (eb==null) {
            return;
        }
        String backendId = backend.getString(IJsonable.ID_FIELDNAME);
        eb.publish(IQueueService.QUEUE_HEALTHCHECK_FAIL, backend);
        log.info(String.format("Backend %s Fail. disabling it", backendId));
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#publishBackendConnections(java.lang.String, com.globo.galeb.core.SafeJsonObject)
     */
    @Override
    public void publishBackendConnections(String queueActiveConnections, JsonObject myConnections) {
        eb.publish(queueActiveConnections, myConnections);
    }

    /**
     * Connection counter handler.
     *
     * @param connectionsCounter the connections counter
     * @return the handler
     */
    private Handler<Message<JsonObject>> connectionCounterHandler(final ICallbackConnectionCounter connectionsCounter) {
        return new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                connectionsCounter.callbackGlobalConnectionsInfo(message.body());
            }
        };
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#registerConnectionsCounter(com.globo.galeb.core.bus.ICallbackConnectionCounter, java.lang.String)
     */
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

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#publishActiveConnections(java.lang.String, com.globo.galeb.core.SafeJsonObject)
     */
    @Override
    public void publishActiveConnections(String queueActiveConnections,
            JsonObject myConnections) {
        if (eb==null) {
            return;
        }
        eb.publish(queueActiveConnections, myConnections);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#unregisterConnectionsCounter(com.globo.galeb.core.bus.ICallbackConnectionCounter, java.lang.String)
     */
    @Override
    public void unregisterConnectionsCounter(final ICallbackConnectionCounter connectionsCounter,
            String queueActiveConnections) {
        if (connectionsCounter.isRegistered() && eb!=null) {
            eb.unregisterHandler(queueActiveConnections, connectionCounterHandler(connectionsCounter));
            connectionsCounter.setRegistered(false);
        }
    }

    /**
     * Log if eventBus is null.
     */
    private void logEventBusNull() {
        String logMessage = "registerQueueAdd is not possible: EventBus is null";
        if (log!=null)  {
            log.warn(logMessage);
        } else {
            System.err.println(logMessage);
        }
    }

    /**
     * Log queue registered.
     *
     * @param starter the starter
     * @param queue the queue
     */
    private void logQueueRegistered(String starter, String queue) {
        String message = String.format("[%s] %s registered", starter, queue);
        if (log!=null) {
            log.info(message);
        } else {
            System.err.println(message);
        }
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#registerQueueAdd(java.lang.Object, com.globo.galeb.core.bus.ICallbackQueueAction)
     */
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

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#registerQueueDel(java.lang.Object, com.globo.galeb.core.bus.ICallbackQueueAction)
     */
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

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#registerQueueVersion(java.lang.Object, com.globo.galeb.core.bus.ICallbackQueueAction)
     */
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

}
