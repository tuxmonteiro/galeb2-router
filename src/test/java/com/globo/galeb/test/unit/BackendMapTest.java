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
package com.globo.galeb.test.unit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.impl.DefaultVertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.impl.LogDelegate;

import com.globo.galeb.core.Farm;
import com.globo.galeb.core.bus.BackendPoolMap;
import com.globo.galeb.core.bus.MessageBus;
import com.globo.galeb.core.bus.BackendMap;
import com.globo.galeb.core.entity.Entity;
import com.globo.galeb.test.unit.util.FakeLogger;

public class BackendMapTest {

    private BackendMap backendMap;
    private MessageBus messageBus;
    private MessageBus messageBusParent;
    private BackendPoolMap backendPoolMap;
    private Farm farm;

    @Before
    public void setUp() throws Exception {
        Vertx vertx = mock(DefaultVertx.class);

        HttpClient httpClient = mock(HttpClient.class);
        when(vertx.createHttpClient()).thenReturn(httpClient);

        LogDelegate logDelegate = mock(LogDelegate.class);
        FakeLogger logger = new FakeLogger(logDelegate);
        logger.setQuiet(false);
        logger.setTestId("");

        farm = new Farm(null);
        farm.start();

        messageBus = new MessageBus();
        messageBus.setUri("/backend")
                  .setParentId("test.localdomain")
                  .setEntity(new JsonObject().putString(Entity.ID_FIELDNAME, "127.0.0.1:8080").encode());

        backendMap = new BackendMap();
        backendMap.setMessageBus(messageBus).setLogger(logger).setFarm(farm);

        messageBusParent = new MessageBus();
        messageBusParent.setUri("/virtualhost")
                  .setEntity(new JsonObject().putString(Entity.ID_FIELDNAME, "test.localdomain").encode());

        backendPoolMap = new BackendPoolMap();
        backendPoolMap.setMessageBus(messageBusParent).setLogger(logger).setFarm(farm);

    }

    @Test
    public void addReturnFalseIfParentIdNotExist() {
        assertFalse(backendMap.add());
    }

    @Test
    public void addReturnTrueIfParentIdExist() {
        backendPoolMap.add();
        assertTrue(backendMap.add());
    }

    @Test
    public void delAllReturnTrue() {
        assertTrue(backendMap.del());
    }

    @Test
    public void delReturnTrueIfExist() {
        backendPoolMap.add();
        backendMap.add();
        assertTrue(backendMap.del());
    }

    @Test
    public void resetReturnFalse() {
        assertFalse(backendMap.reset());
    }

    @Test
    public void changeReturnFalse() {
        assertFalse(backendMap.change());
    }

}
