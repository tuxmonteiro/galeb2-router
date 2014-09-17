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

import com.globo.galeb.core.Farm;
import com.globo.galeb.core.HttpCode;
import com.globo.galeb.core.IEventObserver;
import com.globo.galeb.core.SafeJsonObject;
import com.globo.galeb.core.ManagerService;
import com.globo.galeb.core.Server;
import com.globo.galeb.core.ServerResponse;
import com.globo.galeb.handlers.manager.GetMatcherHandler;
import com.globo.galeb.metrics.CounterWithStatsd;
import com.globo.galeb.metrics.ICounter;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class RouteManagerVerticle extends Verticle implements IEventObserver {

    private static String routeManagerId = "route_manager";

    public Logger log;
    private Server server;
    private String httpServerName = null;
    private Farm farm;

    @Override
    public void start() {
        log = container.logger();
        final SafeJsonObject conf = new SafeJsonObject(container.config());
        final ICounter counter = new CounterWithStatsd(conf, vertx, log);
        server = new Server(vertx, container, counter);
        farm = new Farm(this);

        startHttpServer(conf);

        log.info(String.format("Instance %s started", this.toString()));
    }

    @Override
    public void setVersion(Long version) {
        farm.setVersion(version);
    }

    @Override
    public void postAddEvent(String message) {
        return;
    };

    @Override
    public void postDelEvent(String message) {
        return;
    };

    private void startHttpServer(final SafeJsonObject serverConf) throws RuntimeException {
        final Logger log = this.getContainer().logger();
        final Server server = this.server;

        RouteMatcher routeMatcher = new RouteMatcher();

        routeMatcher.getWithRegEx("\\/([^\\/]+)[\\/]?([^\\/]+)?", new GetMatcherHandler(routeManagerId, log, farm));

        routeMatcher.post("/:uriBase", postMethodHandler());

        routeMatcher.delete("/:uriBase/:id", deleteMethodWithIdHandler());
//        routeMatcher.delete("/:uriBase", deleteMethodHandler());

        routeMatcher.noMatch(new Handler<HttpServerRequest>() {

            @Override
            public void handle(HttpServerRequest req) {
                final ServerResponse serverResponse = new ServerResponse(req, log, null, false);

                if (httpServerName==null) {
                    httpServerName = req.headers().contains(HttpHeaders.HOST) ? req.headers().get(HttpHeaders.HOST) : "SERVER";
                    Server.setHttpServerName(httpServerName);
                }

                int statusCode = HttpCode.BadRequest;
                serverResponse.setStatusCode(statusCode)
                    .setMessage(HttpCode.getMessage(statusCode, true))
                    .setId(routeManagerId)
                    .end();
                log.warn(String.format("%s %s not supported", req.method(), req.uri()));
            }
        });

        server.setDefaultPort(9090).setHttpServerRequestHandler(routeMatcher).start(this);
    }

    private Handler<HttpServerRequest> postMethodHandler() {

        return new Handler<HttpServerRequest>() {

            @Override
            public void handle(final HttpServerRequest req) {
                final ServerResponse serverResponse = new ServerResponse(req, log, null, false);
                final ManagerService managerService = new ManagerService(routeManagerId, log);

                managerService.setRequest(req).setResponse(serverResponse);

                if (!managerService.checkMethodOk("POST") || !managerService.checkUriOk()) {
                    return;
                }

                req.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer body) {
                        String bodyStr = body.toString();
                        String uri = req.uri();
                        int statusCode = managerService.statusFromMessageSchema(bodyStr, uri);

                        if (statusCode==HttpCode.Ok) {
                            if (uri.startsWith("/farm")) {
                                farm.getQueueMap().sendGroupActionAdd(new SafeJsonObject(bodyStr), uri);
                            } else {
                                farm.getQueueMap().sendActionAdd(new SafeJsonObject(bodyStr), uri);
                            }
                        }

                        serverResponse.setStatusCode(statusCode)
                            .setMessage(HttpCode.getMessage(statusCode, true))
                            .setId(routeManagerId)
                            .end();
                    }
                });

            }
        };
    }

    private Handler<HttpServerRequest> deleteMethodWithIdHandler() {

        return new Handler<HttpServerRequest>() {

            @Override
            public void handle(final HttpServerRequest req) {
                final ServerResponse serverResponse = new ServerResponse(req, log, null, false);
                final ManagerService managerService = new ManagerService(routeManagerId, log);

                managerService.setRequest(req).setResponse(serverResponse);

                final String id = managerService.getRequestId();
                if (!managerService.checkMethodOk("DELETE") ||
                        !managerService.checkUriOk() ||
                        !managerService.checkIdPresent()) {
                    return;
                }

                req.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer body) {
                        String bodyStr = body.toString();
                        SafeJsonObject bodyJson = new SafeJsonObject(body.toString());

                        if (!managerService.checkIdConsistency(bodyJson, id)) {
                            return;
                        }
                        int statusCode = managerService.statusFromMessageSchema(bodyStr, req.uri());

                        if (statusCode==HttpCode.Ok) {
                            farm.getQueueMap().sendActionDel(bodyJson, req.uri());
                            statusCode = HttpCode.Accepted;
                        }

                        serverResponse.setStatusCode(statusCode)
                            .setMessage(HttpCode.getMessage(statusCode, true))
                            .setId(routeManagerId)
                            .end();
                    }
                });

            }
        };
    }

}
