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

import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;

import com.globo.galeb.entity.impl.Farm;

/**
 * Class MessageToMapBuilder.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class MessageToMapBuilder {

    /**
     * Instantiates a new MessageToMapBuilder.
     */
    private MessageToMapBuilder() {}

    /**
     * Gets the single instance of MessageToMap.
     *
     * @param message the message
     * @param farm the farm
     * @return single instance of MessageToMap
     */
    @SuppressWarnings("rawtypes")
    public static MessageToMap getInstance(String message, Farm farm) {

        if (farm==null) {
            return new NullMap();
        }

        final MessageBus messageBus = new MessageBus(message);
        final Logger log = farm.getLogger();
        Object plataform = farm.getPlataform();
        final Vertx vertx = (plataform instanceof Vertx) ? (Vertx) plataform : null;
        final String verticleId = farm.getVerticleId();

        switch (messageBus.getUriBase()) {
            case "farm":
                return new FarmMap()
                    .setMessageBus(messageBus)
                    .setLogger(log)
                    .setVertx(vertx)
                    .setFarm(farm)
                    .setVerticleId(verticleId);
            case "virtualhost":
                return new VirtualhostMap()
                    .setMessageBus(messageBus)
                    .setLogger(log)
                    .setVertx(vertx)
                    .setFarm(farm)
                    .setVerticleId(verticleId);
            case "backend":
                return new BackendMap()
                    .setMessageBus(messageBus)
                    .setLogger(log)
                    .setVertx(vertx)
                    .setFarm(farm)
                    .setVerticleId(verticleId);
            case "backendpool":
                return new BackendPoolMap()
                    .setMessageBus(messageBus)
                    .setLogger(log)
                    .setVertx(vertx)
                    .setFarm(farm)
                    .setVerticleId(verticleId);
            case "rule":
                return new RuleMap()
                    .setMessageBus(messageBus)
                    .setLogger(log)
                    .setVertx(vertx)
                    .setFarm(farm)
                    .setVerticleId(verticleId);
            default:
                break;
        }
        return new NullMap().setLogger(log).setVerticleId(verticleId);
    }

}
