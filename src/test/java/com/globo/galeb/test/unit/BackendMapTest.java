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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.impl.DefaultVertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.impl.LogDelegate;

import com.globo.galeb.bus.BackendMap;
import com.globo.galeb.bus.BackendPoolMap;
import com.globo.galeb.bus.MessageBus;
import com.globo.galeb.entity.Entity;
import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.entity.IJsonable.StatusType;
import com.globo.galeb.entity.impl.Farm;
import com.globo.galeb.entity.impl.backend.IBackend;
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
                  .setParentId("pool0")
                  .setEntity(new JsonObject().putString(Entity.ID_FIELDNAME, "127.0.0.1:8080").encode());

        backendMap = new BackendMap();
        backendMap.setMessageBus(messageBus).setFarm(farm);

        messageBusParent = new MessageBus();
        messageBusParent.setUri("/backendpool")
                  .setEntity(new JsonObject().putString(Entity.ID_FIELDNAME, "pool0").encode());

        backendPoolMap = new BackendPoolMap();
        backendPoolMap.setMessageBus(messageBusParent).setFarm(farm);

    }

    @Test
    public void addReturnFalseIfParentIdNotExist() {
        assertThat(backendMap.add()).isFalse();
    }

    @Test
    public void addReturnTrueIfParentIdExist() {
        backendPoolMap.add();
        assertThat(backendMap.add()).isTrue();
    }

    @Test
    public void delAllReturnTrue() {
        assertThat(backendMap.del()).isTrue();
    }

    @Test
    public void delReturnTrueIfExist() {
        backendPoolMap.add();
        backendMap.add();
        assertThat(backendMap.del()).isTrue();
    }

    @Test
    public void resetReturnFalse() {
        assertThat(backendMap.reset()).isFalse();
    }

    @Test
    public void changeReturnFalse() {
        assertThat(backendMap.change()).isFalse();
    }

    @Test
    public void backendAddedInBadBackendPoolIfAddedWithStatusFailed() {
        JsonObject entity = new JsonObject().putString(Entity.ID_FIELDNAME, "127.0.0.2:8080")
                                            .putString(IJsonable.STATUS_FIELDNAME, StatusType.FAILED_STATUS.toString());

        MessageBus messageBusFailed = new MessageBus();
        messageBusFailed.setUri("/backend")
                  .setParentId("pool0")
                  .setEntity(entity.encode());

        backendPoolMap.add();
        backendMap.setMessageBus(messageBusFailed).add();

        IBackend backend = farm.getBackendPools()
                               .getEntityById("pool0")
                               .getBadBackendById(entity.getString(IJsonable.ID_FIELDNAME));

        assertThat(backend).isNotNull();
    }

    public void backendNotAddedInBackendPoolIfAddedWithStatusFailed() {
        JsonObject entity = new JsonObject().putString(Entity.ID_FIELDNAME, "127.0.0.2:8080")
                .putString(IJsonable.STATUS_FIELDNAME, StatusType.RUNNING_STATUS.toString());

        MessageBus messageBusFailed = new MessageBus();
        messageBusFailed.setUri("/backend")
                        .setParentId("pool0")
                        .setEntity(entity.encode());

        backendPoolMap.add();
        backendMap.setMessageBus(messageBusFailed).add();

        IBackend backend = farm.getBackendPools()
           .getEntityById("pool0")
           .getBadBackendById(entity.getString(IJsonable.ID_FIELDNAME));

        assertThat(backend).isNull();
    }

    @Test
    public void backendWithStatusFailedRemovedInBadBackendPool() {
        JsonObject entity = new JsonObject().putString(Entity.ID_FIELDNAME, "127.0.0.2:8080")
                                            .putString(IJsonable.STATUS_FIELDNAME, StatusType.FAILED_STATUS.toString());

        MessageBus messageBusFailedToAdded = new MessageBus();
        messageBusFailedToAdded.setUri("/backend")
                               .setParentId("pool0")
                               .setEntity(entity.encode());
        boolean isOk = backendPoolMap.add();
        backendMap.setMessageBus(messageBusFailedToAdded).add();

        MessageBus messageBusFailedToRemove = new MessageBus();
        messageBusFailedToRemove.setUri("/backend/"+entity.getString(IJsonable.ID_FIELDNAME))
                                .setParentId("pool0")
                                .setEntity(entity.encode());
        backendMap.setMessageBus(messageBusFailedToRemove).del();
        IBackend backend = farm.getBackendPools()
                               .getEntityById("pool0")
                               .getBadBackendById(entity.getString(IJsonable.ID_FIELDNAME));

        assertThat(isOk).isTrue();
        assertThat(backend).isNull();
    }

    public void backendWithStatusFailedNotRemovedInBackendPool() {
        JsonObject entity = new JsonObject().putString(Entity.ID_FIELDNAME, "127.0.0.1:8080")
                                    .putString(IJsonable.STATUS_FIELDNAME, StatusType.FAILED_STATUS.toString());

        MessageBus messageBusFailedToAdded = new MessageBus();
        messageBusFailedToAdded.setUri("/backend")
                               .setParentId("pool0")
                               .setEntity(entity.encode());

        backendPoolMap.add();
        backendMap.setMessageBus(messageBusFailedToAdded).add();

        JsonObject entityWithOtherStatus = new JsonObject().putString(Entity.ID_FIELDNAME, "127.0.0.1:8080")
                                                .putString(IJsonable.STATUS_FIELDNAME, StatusType.RUNNING_STATUS.toString());

        MessageBus messageBusFailedToRemove = new MessageBus();
        messageBusFailedToRemove.setUri("/backend/"+entityWithOtherStatus.getString(IJsonable.ID_FIELDNAME))
                                .setParentId("pool0")
                                .setEntity(entity.encode());

        boolean isOk = backendMap.setMessageBus(messageBusFailedToRemove).del();

        assertThat(isOk).isFalse();
    }

}
