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
package com.globo.galeb.core.bus;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;

import com.globo.galeb.core.Farm;

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
        final Vertx vertx = farm.getVertx();
        final String verticleId = farm.getVerticleId();

        switch (messageBus.getUriBase()) {
            case "farm":
                Map<String, Farm> map = new HashMap<>();
                map.put("farm", farm);
                return new FarmMap()
                    .setMessageBus(messageBus)
                    .setLogger(log)
                    .setVertx(vertx)
                    .setMap(map)
                    .setVerticleId(verticleId);
            case "virtualhost":
                return new VirtualhostMap()
                    .setMessageBus(messageBus)
                    .setLogger(log)
                    .setVertx(vertx)
                    .setMap(farm.getVirtualhostsToMap())
                    .setVerticleId(verticleId);
            case "backend":
                return new BackendMap()
                    .setMessageBus(messageBus)
                    .setLogger(log)
                    .setVertx(vertx)
                    .setMap(farm.getVirtualhostsToMap())
                    .setVerticleId(verticleId);
            default:
                break;
        }
        return new NullMap().setLogger(log).setVerticleId(verticleId);
    }

}
