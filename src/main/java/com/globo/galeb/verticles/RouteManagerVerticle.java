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
import com.globo.galeb.core.SafeJsonObject;
import com.globo.galeb.core.Server;
import com.globo.galeb.core.ServerResponse;
import com.globo.galeb.core.bus.IEventObserver;
import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.core.bus.VertxQueueService;
import com.globo.galeb.handlers.rest.DeleteMatcherHandler;
import com.globo.galeb.handlers.rest.GetMatcherHandler;
import com.globo.galeb.handlers.rest.PostMatcherHandler;
import com.globo.galeb.handlers.rest.PutMatcherHandler;
import com.globo.galeb.metrics.CounterWithStatsd;
import com.globo.galeb.metrics.ICounter;

import org.vertx.java.core.Handler;
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
    private IQueueService queueService;

    private final String patternRegex = "\\/([^\\/]+)[\\/]?([^\\/]+)?";

    @Override
    public void start() {
        log = container.logger();
        final SafeJsonObject conf = new SafeJsonObject(container.config());
        final ICounter counter = new CounterWithStatsd(conf, vertx, log);
        server = new Server(vertx, container, counter);
        queueService = new VertxQueueService(vertx.eventBus(), log);
        farm = new Farm(this, queueService);

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

        routeMatcher.getWithRegEx(patternRegex, new GetMatcherHandler(routeManagerId, log, farm));

        routeMatcher.post("/:uriBase", new PostMatcherHandler(routeManagerId, log, queueService));

        routeMatcher.deleteWithRegEx(patternRegex, new DeleteMatcherHandler(routeManagerId, log, queueService));

        routeMatcher.putWithRegEx(patternRegex, new PutMatcherHandler(routeManagerId, log, queueService));

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

}
