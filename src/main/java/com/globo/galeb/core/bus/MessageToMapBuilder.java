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

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;

import com.globo.galeb.core.Farm;

public class MessageToMapBuilder {

    private MessageToMapBuilder() {}

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
