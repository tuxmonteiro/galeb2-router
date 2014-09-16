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
package com.globo.galeb.verticles;

import static com.globo.galeb.core.Constants.QUEUE_HEALTHCHECK_FAIL;
import static com.globo.galeb.core.Constants.QUEUE_HEALTHCHECK_OK;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.globo.galeb.core.Backend;
import com.globo.galeb.core.HttpCode;
import com.globo.galeb.core.IEventObserver;
import com.globo.galeb.core.IJsonable;
import com.globo.galeb.core.MessageBus;
import com.globo.galeb.core.QueueMap;
import com.globo.galeb.core.QueueMap.ACTION;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class HealthManagerVerticle extends Verticle implements IEventObserver {

    private final Map<String, Set<String>> backendsMap = new HashMap<>();
    private final Map<String, Set<String>> badBackendsMap = new HashMap<>();
    private final String httpHeaderHost = HttpHeaders.HOST.toString();

    @Override
    public void start() {
        final Logger log = container.logger();

        final JsonObject conf = container.config();
        final Long checkInterval = conf.getLong("checkInterval", 5000L); // Milliseconds Interval
        final String uriHealthCheck = conf.getString("uriHealthCheck","/"); // Recommended = "/health"

        new QueueMap(this, null).register();

        final EventBus eb = vertx.eventBus();
        eb.registerHandler(QUEUE_HEALTHCHECK_OK, new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                String backend = message.body();
                try {
                    moveBackend(backend, true, eb);
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
                    moveBackend(backend, false, eb);
                } catch (UnsupportedEncodingException e) {
                    log.error(e.getMessage());
                }
                log.error(String.format("Backend %s FAIL", backend));
            };
        });

        vertx.setPeriodic(checkInterval, new Handler<Long>() {
            @Override
            public void handle(Long timerID) {
                log.info("Checking bad backends...");
                if (badBackendsMap!=null) {
                    Iterator<String> it = badBackendsMap.keySet().iterator();
                    while (it.hasNext()) {
                        final String backend = it.next();
                        String[] hostWithPort = backend.split(":");
                        String host = hostWithPort[0];
                        Integer port = Integer.parseInt(hostWithPort[1]);
                        try {
                            HttpClient client = vertx.createHttpClient()
                                .setHost(host)
                                .setPort(port)
                                .exceptionHandler(new Handler<Throwable>() {
                                    @Override
                                    public void handle(Throwable event) {}
                                });
                            HttpClientRequest cReq = client.get(uriHealthCheck, new Handler<HttpClientResponse>() {
                                    @Override
                                    public void handle(HttpClientResponse cResp) {
                                        if (cResp!=null && cResp.statusCode()==HttpCode.Ok) {
                                            eb.publish(QUEUE_HEALTHCHECK_OK, backend);
                                            log.info(String.format("Backend %s OK. Enabling it", backend));
                                        }
                                    }
                                });
                            cReq.headers().set(httpHeaderHost, (String) badBackendsMap.get(backend).toArray()[0]);
                            cReq.exceptionHandler(new Handler<Throwable>() {
                                @Override
                                public void handle(Throwable event) {}
                            });
                            cReq.end();
                        } catch (RuntimeException e) {
                            log.error(e.getMessage());
                        }
                    }
                }
            }
        });
        log.info(String.format("Instance %s started", this.toString()));
    }

    @Override
    public void setVersion(Long version) {}

    @Override
    public void postAddEvent(String message) {

        MessageBus messageBus = new MessageBus(message);
        if ("backend".equals(messageBus.getUriBase())) {
            boolean backendStatus = messageBus.getEntity().getBoolean(Backend.propertyStatusFieldName, true);
            String backendId = messageBus.getEntityId();
            String virtualhostId = messageBus.getParentId();
            final Map <String, Set<String>> tempMap = backendStatus ? backendsMap : badBackendsMap;

            if (!tempMap.containsKey(backendId)) {
                tempMap.put(backendId, new HashSet<String>());
            }
            Set<String> virtualhosts = tempMap.get(backendId);
            virtualhosts.add(virtualhostId);
        }
    };

    @Override
    public void postDelEvent(String message) {
        MessageBus messageBus = new MessageBus(message);
        if ("backend".equals(messageBus.getUriBase())) {

            boolean backendStatus = messageBus.getEntity().getBoolean(Backend.propertyStatusFieldName, true);
            String backendId = messageBus.getEntityId();
            String virtualhostId = messageBus.getParentId();
            final Map <String, Set<String>> tempMap = backendStatus ? backendsMap : badBackendsMap;

            if (tempMap.containsKey(backendId)) {
                Set<String> virtualhosts = tempMap.get(backendId);
                virtualhosts.remove(virtualhostId);
                if (virtualhosts.isEmpty()) {
                    tempMap.remove(backendId);
                }
            }
        }
    };

    private void moveBackend(final String backend, final Boolean status, final EventBus eb) throws UnsupportedEncodingException {

        Set<String> virtualhosts = status ? badBackendsMap.get(backend) : backendsMap.get(backend);

        if (virtualhosts!=null) {
            Iterator<String> it = virtualhosts.iterator();
            while (it.hasNext()) {
                String virtualhost = it.next();
                JsonObject backendJson = new JsonObject().putString(IJsonable.jsonIdFieldName, backend);
                JsonObject virtualhostJson = new JsonObject().putString(IJsonable.jsonIdFieldName, virtualhost);

                String messageDel = new MessageBus()
                                            .setParentId(virtualhostJson.getString(IJsonable.jsonIdFieldName))
                                            .setEntity(backendJson
                                                    .putBoolean(Backend.propertyStatusFieldName, !status).encode())
                                            .setUri(String.format("/backend/%s", URLEncoder.encode(backend,"UTF-8")))
                                            .make()
                                            .toString();
                if (eb!=null) {
                    eb.publish(ACTION.DEL.toString(), messageDel);
                }

                String messageAdd = new MessageBus()
                                            .setParentId(virtualhostJson.getString(IJsonable.jsonIdFieldName))
                                            .setEntity(backendJson
                                                    .putBoolean(Backend.propertyStatusFieldName, status).encode())
                                            .setUri("/backend")
                                            .make()
                                            .toString();
                if (eb!=null) {
                    eb.publish(ACTION.ADD.toString(), messageAdd);
                }
            }
        }
    }

}
