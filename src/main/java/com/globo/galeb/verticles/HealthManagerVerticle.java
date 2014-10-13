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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.globo.galeb.core.Backend;
import com.globo.galeb.core.Farm;
import com.globo.galeb.core.HttpCode;
import com.globo.galeb.core.SafeJsonObject;
import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.core.bus.ICallbackHealthcheck;
import com.globo.galeb.core.bus.IEventObserver;
import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.core.bus.MessageBus;
import com.globo.galeb.core.bus.VertxQueueService;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class HealthManagerVerticle extends Verticle implements IEventObserver, ICallbackHealthcheck {

    private final Map<String, Set<String>> backendsMap = new HashMap<>();
    private final Map<String, Set<String>> badBackendsMap = new HashMap<>();
    private final String httpHeaderHost = HttpHeaders.HOST.toString();
    private Farm farm;
    private IQueueService queueService;
    private JsonObject conf;
    private Logger log;
    private String uriHealthCheck;

    private class CheckBadBackendTaskHandler implements Handler<Long> {

        @Override
        public void handle(Long schedulerId) {
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
                                    if (cResp!=null && cResp.statusCode()==HttpCode.OK) {
                                        queueService.publishBackendOk(backend);
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

    }

    @Override
    public void start() {
        log = container.logger();
        conf = container.config();
        uriHealthCheck = conf.getString("uriHealthCheck","/"); // Recommended = "/health"
        Long checkInterval = conf.getLong("checkInterval", 5000L); // Milliseconds Interval

        queueService = new VertxQueueService(vertx.eventBus(),container.logger());
        queueService.registerHealthcheck(this);
        farm = new Farm(this, queueService);

        vertx.setPeriodic(checkInterval, new CheckBadBackendTaskHandler());
        log.info(String.format("Instance %s started", this.toString()));
    }

    @Override
    public void setVersion(Long version) {}

    @Override
    public void postAddEvent(String message) {

        MessageBus messageBus = new MessageBus(message);
        if ("backend".equals(messageBus.getUriBase())) {
            boolean backendStatus = messageBus.getEntity().getBoolean(Backend.ELEGIBLE_FIELDNAME, true);
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

            boolean backendStatus = messageBus.getEntity().getBoolean(Backend.ELEGIBLE_FIELDNAME, true);
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

    @Override
    public void moveBackend(String backend, boolean elegible) throws UnsupportedEncodingException {

        Set<String> virtualhosts = elegible ? badBackendsMap.get(backend) : backendsMap.get(backend);

        if (virtualhosts!=null) {
            Iterator<String> it = virtualhosts.iterator();
            while (it.hasNext()) {
                Virtualhost virtualhost = farm.getVirtualhost(it.next());

                SafeJsonObject backendJson = null;
                if (virtualhost!=null) {

                    for (Backend backendSearched: virtualhost.getBackends(!elegible)) {
                        if (backend.equals(backendSearched.toString())) {
                            backendJson = new SafeJsonObject(backendSearched.toJson());
                            break;
                        }
                    }

                    if (backendJson!=null) {
                        String uriDel = String.format("/backend/%s", URLEncoder.encode(backend,"UTF-8"));
                        String uriAdd = "/backend";

                        backendJson.putBoolean(Backend.ELEGIBLE_FIELDNAME, !elegible);
                        queueService.queueToDel(backendJson, uriDel);

                        backendJson.putBoolean(Backend.ELEGIBLE_FIELDNAME, elegible);
                        queueService.queueToAdd(backendJson, uriAdd);
                    }

                }
            }
        }
    }

}
