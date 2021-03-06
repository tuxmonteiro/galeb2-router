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
package com.globo.galeb.verticles;

import com.globo.galeb.bus.IQueueService;
import com.globo.galeb.bus.VertxQueueService;
import com.globo.galeb.entity.impl.Farm;
import com.globo.galeb.handlers.RouterRequestHandler;
import com.globo.galeb.logger.SafeLogger;
import com.globo.galeb.metrics.CounterWithEventBus;
import com.globo.galeb.metrics.ICounter;
import com.globo.galeb.server.Server;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

/**
 * Class RouterVerticle.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class RouterVerticle extends Verticle {

  /* (non-Javadoc)
   * @see org.vertx.java.platform.Verticle#start()
   */
  @Override
  public void start() {

      final SafeLogger log = new SafeLogger().setLogger(container.logger());
      final JsonObject conf = container.config();
      final ICounter counter = new CounterWithEventBus(vertx.eventBus());
      final IQueueService queueService = new VertxQueueService(vertx.eventBus(), log);
      final JsonObject starterConf = conf.getObject(ConfVerticleDictionary.CONF_STARTER_CONF, new JsonObject());
      final Farm farm = new Farm(this);
      farm.setLogger(new SafeLogger().setLogger(log))
          .setPlataform(vertx)
          .setQueueService(queueService)
          .setStaticConf(starterConf.getObject(ConfVerticleDictionary.CONF_ROOT_ROUTER, new JsonObject()))
          .setCounter(counter)
          .start();

      final Server server = new Server(vertx, container, counter);

      try {
          final Handler<HttpServerRequest> handlerHttpServerRequest =
                  new RouterRequestHandler(vertx, farm, counter, queueService, log);

          server.setDefaultPort(8000)
              .setHttpServerRequestHandler(handlerHttpServerRequest).start(this);

      } catch (RuntimeException e) {
          log.debug(e);
      }

      log.info(String.format("Instance %s started", this.toString()));

   }

}
