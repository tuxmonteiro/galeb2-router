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
import com.globo.galeb.core.Server;
import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.core.bus.VertxQueueService;
import com.globo.galeb.handlers.RouterRequestHandler;
import com.globo.galeb.handlers.ws.FrontendWebSocketHandler;
import com.globo.galeb.metrics.CounterWithStatsd;
import com.globo.galeb.metrics.ICounter;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class RouterVerticle extends Verticle {

  @Override
  public void start() {

      final Logger log = container.logger();
      final JsonObject conf = container.config();
      final ICounter counter = new CounterWithStatsd(conf, vertx, log);
      final IQueueService queueService = new VertxQueueService(vertx.eventBus(), log);
      final Farm farm = new Farm(this, queueService);

      final boolean enableChunked = conf.getBoolean("enableChunked", true);
      final boolean enableAccessLog = conf.getBoolean("enableAccessLog", false);

      final Server server = new Server(vertx, container, counter);

      final Handler<HttpServerRequest> handlerHttpServerRequest =
              new RouterRequestHandler(vertx, farm, counter, queueService, log)
                      .setEnableChunked(enableChunked)
                      .setEnableAccessLog(enableAccessLog);

      final Handler<ServerWebSocket> serverWebSocketHandler =
              new FrontendWebSocketHandler(vertx, container, farm);

      server.setDefaultPort(9000)
          .setHttpServerRequestHandler(handlerHttpServerRequest)
          .setWebsocketServerRequestHandler(serverWebSocketHandler).start(this);
      log.info(String.format("Instance %s started", this.toString()));

   }

}
