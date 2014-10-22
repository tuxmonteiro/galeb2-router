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

import java.util.Iterator;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.core.Farm;
import com.globo.galeb.core.IJsonable;
import com.globo.galeb.core.Virtualhost;

public class FarmMap extends MessageToMap<Farm> {

    public FarmMap() {
        super();
    }

    @Override
    public boolean add() {
        boolean isOk = false;
        Farm farm = map.get("farm");

        JsonArray virtualhosts = entity.getArray("virtualhosts", new JsonArray());

        Iterator<Object> virtualhostIterator = virtualhosts.iterator();
        while (virtualhostIterator.hasNext()) {
            Object virtualhostObj = virtualhostIterator.next();
            JsonObject virtualhostJson = (JsonObject) virtualhostObj;

            VirtualhostMap virtualhostMap = new VirtualhostMap();
            virtualhostMap.staticConf(staticConf);

            MessageBus virtualhostMessageBus = new MessageBus()
                                                .setEntity(virtualhostJson.encode())
                                                .setUri("/virtualhost")
                                                .make();

            virtualhostMap.setMessageBus(virtualhostMessageBus)
                          .setLogger(log)
                          .setVertx(vertx)
                          .setMap(farm.getVirtualhostsToMap())
                          .setVerticleId(verticleId);

            virtualhostMap.add();

            JsonArray backends = virtualhostJson.getObject(Virtualhost.BACKENDS_FIELDNAME, new JsonObject())
                                                .getArray(Virtualhost.BACKENDS_ELIGIBLE_FIELDNAME);
            if (backends==null) {
                continue;
            }

            Iterator<Object> backendIterator = backends.iterator();
            while (backendIterator.hasNext()) {
                Object backendObj = backendIterator.next();
                JsonObject backendJson = (JsonObject) backendObj;

                BackendMap backendMap = new BackendMap();
                backendMap.staticConf(staticConf);

                MessageBus backendMessageBus = new MessageBus()
                                                    .setEntity(backendJson.encode())
                                                    .setParentId(virtualhostJson.getString(IJsonable.ID_FIELDNAME))
                                                    .setUri("/backend")
                                                    .make();

                backendMap.setMessageBus(backendMessageBus)
                          .setLogger(log)
                          .setVertx(vertx)
                          .setMap(farm.getVirtualhostsToMap())
                          .setVerticleId(verticleId);
                backendMap.add();
            }
            isOk = true;
        }

        return isOk;
    }

    @Override
    public boolean del() {
        boolean isOk = false;
        Farm farm = map.get("farm");

        farm.clearAll();

        return isOk;
    }

    @Override
    public boolean reset() {
        // TODO
        return false;
    }

    @Override
    public boolean change() {
        // TODO
        return false;
    }

}
