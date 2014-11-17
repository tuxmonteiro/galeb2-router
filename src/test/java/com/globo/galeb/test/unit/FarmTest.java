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
import static org.vertx.testtools.VertxAssert.testComplete;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import org.junit.Test;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.impl.LogDelegate;
import org.vertx.testtools.TestVerticle;

import com.globo.galeb.core.Farm;
import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.core.bus.MessageBus;
import com.globo.galeb.core.entity.IJsonable;
import com.globo.galeb.rules.Rule;
import com.globo.galeb.rules.RuleFactory;
import com.globo.galeb.test.unit.util.FakeLogger;

public class FarmTest extends TestVerticle {

    private JsonObject backendPoolJson = new JsonObject().putString(IJsonable.ID_FIELDNAME, "pool0");
    private String backendPoolId = backendPoolJson.getString(IJsonable.ID_FIELDNAME);
    private JsonObject virtualhostJson = new JsonObject().putString(IJsonable.ID_FIELDNAME, "test.virtualhost.com");
    private String virtualhostId = virtualhostJson.getString(IJsonable.ID_FIELDNAME);
    private JsonObject backendJson = new JsonObject().putString(IJsonable.ID_FIELDNAME, "0.0.0.0:00");
    private String backendId = backendJson.getString(IJsonable.ID_FIELDNAME);
    private IQueueService queueService = null;
    private LogDelegate logDelegate = null;
    private FakeLogger logger = null;
    private Farm farm = null;

    private void setUp() {
        if (queueService==null) {
            queueService = mock(IQueueService.class);
        }
        if (logDelegate==null) {
            logDelegate = mock(LogDelegate.class);
        }
        if (logger==null) {
            logger = new FakeLogger(logDelegate);
            ((FakeLogger)logger).setQuiet(false);
            ((FakeLogger)logger).setTestId("");
        }
        if (farm==null) {
            farm = new Farm(this);
            farm.setQueueService(queueService).setLogger(logger).start();
        } else {
            farm.clearBackendPools();
            farm.clearEntities();
        }
    }

    @Test
    public void insert10NewVirtualhost() {
        setUp();

        for (int x=0; x<10;x++) {
            JsonObject virtualhostJson =
                    new JsonObject().putString(IJsonable.ID_FIELDNAME, String.valueOf(x));
            String message = new MessageBus()
                                    .setEntity(virtualhostJson)
                                    .setUri("/virtualhost")
                                    .make()
                                    .toString();

            farm.addToMap(message);
        }

        assertThat(farm.getEntities()).hasSize(10);
        testComplete();
    }

    @Test
    public void insert10NewBackendPools() {
        setUp();

        for (int x=0; x<10;x++) {
            String message = "";
            String aBackendPoolId = UUID.randomUUID().toString();

            JsonObject aBackendPoolJson =
                    new JsonObject().putString(IJsonable.ID_FIELDNAME, aBackendPoolId);
            message = new MessageBus().setEntity(aBackendPoolJson)
                                      .setUri("/backendpool")
                                      .make()
                                      .toString();

            farm.addToMap(message);
        }

        assertThat(farm.getBackendPools().getEntities()).hasSize(10);
        testComplete();
    }

    @Test
    public void insert10NewBackends() {
        setUp();

        String message = "";
        String aBackendPoolId = UUID.randomUUID().toString();
        JsonObject aBackendPoolJson =
                new JsonObject().putString(IJsonable.ID_FIELDNAME, aBackendPoolId);
        message = new MessageBus().setEntity(aBackendPoolJson)
                                  .setUri("/backendpool")
                                  .make()
                                  .toString();

        farm.addToMap(message);

        for (int x=0; x<10;x++) {
            JsonObject aBackendJson =
                    new JsonObject().putString(IJsonable.ID_FIELDNAME, String.format("0:%d", x));
            message = new MessageBus().setEntity(aBackendJson)
                                      .setParentId(aBackendPoolId)
                                      .setUri("/backend")
                                      .make()
                                      .toString();

            farm.addToMap(message);
        }

        assertThat(farm.getBackendPoolById(aBackendPoolId).getEntities()).hasSize(10);
        testComplete();
    }

    @Test
    public void insert10NewRules() {
        setUp();

        String message = "";
        message = new MessageBus().setEntity(virtualhostJson)
                                  .setUri("/virtualhost")
                                  .make()
                                  .toString();

        farm.addToMap(message);

        for (int x=0; x<10;x++) {
            JsonObject aRuleJson =
                    new JsonObject().putString(IJsonable.ID_FIELDNAME, UUID.randomUUID().toString())
                                    .putObject(IJsonable.PROPERTIES_FIELDNAME, new JsonObject()
                                            .putString(Rule.RULETYPE_FIELDNAME, RuleFactory.DEFAULT_RULETYPE)
                                            .putString(Rule.RETURNID_FIELDNAME, "0"));

            message = new MessageBus().setEntity(aRuleJson)
                                      .setParentId(virtualhostId)
                                      .setUri("/rule")
                                      .make()
                                      .toString();

            farm.addToMap(message);
        }

        assertThat(farm.getEntityById(virtualhostId).getEntities()).hasSize(10);
        testComplete();
    }

    @Test
    public void insertNewBackendPoolToRouteMap() {
        setUp();

        String uriStr = "/backendpool";
        String message = new MessageBus()
                                .setEntity(backendPoolJson)
                                .setUri(uriStr)
                                .make()
                                .toString();

        boolean isOk = farm.addToMap(message);

        assertThat(farm.getBackendPoolById(backendPoolId)).isNotNull();
        assertThat(isOk).isTrue();
        testComplete();
    }

    @Test
    public void insertDuplicatedBackendPoolToRouteMap() {
        setUp();
        String uriStr = "/backendpool";
        String message = new MessageBus()
                                .setEntity(backendPoolJson)
                                .setUri(uriStr)
                                .make()
                                .toString();

        farm.addToMap(message);
        boolean isOk = farm.addToMap(message);

        assertThat(farm.getBackendPoolById(backendPoolId)).isNotNull();
        assertThat(isOk).isFalse();
        testComplete();
    }

    @Test
    public void removeExistingBackendPoolFromRouteMap() {
        setUp();

        String messageAdd = new MessageBus()
                                .setEntity(backendPoolJson)
                                .setUri("/backendpool")
                                .make()
                                .toString();
        String uriStr = String.format("/backendpool/%s", backendPoolId);
        String messageDel = new MessageBus()
                                .setEntity(backendPoolJson)
                                .setUri(uriStr)
                                .make()
                                .toString();

        boolean isOkAdd = farm.addToMap(messageAdd);
        boolean isOkDel = farm.delFromMap(messageDel);

        assertThat(isOkAdd).isTrue();
        assertThat(isOkDel).isTrue();
        assertThat(farm.getBackendPoolById(backendPoolId)).isNull();
        testComplete();
    }

    @Test
    public void removeAbsentBackendPoolFromRouteMap() {
        setUp();

        String uriStr = String.format("/backendpool/%s", backendPoolId);
        String message = new MessageBus()
                                .setEntity(backendPoolJson)
                                .setUri(uriStr)
                                .make()
                                .toString();

        boolean isOk = farm.delFromMap(message);

        assertThat(farm.getBackendPoolById(backendPoolId)).isNull();
        assertThat(isOk).isFalse();
        testComplete();
    }

    @Test
    public void insertNewVirtualhostToRouteMap() {
        setUp();

        String uriStr = "/virtualhost";
        String message = new MessageBus()
                                .setEntity(virtualhostJson)
                                .setUri(uriStr)
                                .make()
                                .toString();

        boolean isOk = farm.addToMap(message);

        assertThat(farm.getEntityById(virtualhostId)).isNotNull();
        assertThat(isOk).isTrue();
        testComplete();
    }

    @Test
    public void insertDuplicatedVirtualhostToRouteMap() {
        setUp();

        String uriStr = "/virtualhost";
        String message = new MessageBus()
                                .setEntity(virtualhostJson)
                                .setUri(uriStr)
                                .make()
                                .toString();

        farm.addToMap(message);
        boolean isOk = farm.addToMap(message);

        assertThat(farm.getEntityById(virtualhostId)).isNotNull();
        assertThat(isOk).isFalse();
        testComplete();
    }

    @Test
    public void removeExistingVirtualhostFromRouteMap() {
        setUp();

        String messageAdd = new MessageBus()
                                .setEntity(virtualhostJson)
                                .setUri("/virtualhost")
                                .make()
                                .toString();
        String uriStr = String.format("/virtualhost/%s", virtualhostId);
        String messageDel = new MessageBus()
                                .setEntity(virtualhostJson)
                                .setUri(uriStr)
                                .make()
                                .toString();

        boolean isOkAdd = farm.addToMap(messageAdd);
        boolean isOkDel = farm.delFromMap(messageDel);

        assertThat(isOkAdd).isTrue();
        assertThat(isOkDel).isTrue();
        assertThat(farm.getEntityById(virtualhostId)).isNull();
        testComplete();
    }

    @Test
    public void removeAbsentVirtualhostFromRouteMap() {
        setUp();

        String uriStr = String.format("/virtualhost/%s", virtualhostJson);
        String message = new MessageBus()
                                .setEntity(virtualhostJson)
                                .setUri(uriStr)
                                .make()
                                .toString();

        boolean isOk = farm.delFromMap(message);

        assertThat(farm.getEntityById(virtualhostId)).isNull();
        assertThat(isOk).isFalse();
        testComplete();
    }

    @Test
    public void insertNewBackendToExistingBackendPool() {
        setUp();

        String messageBackendPool = new MessageBus()
                                        .setEntity(backendPoolJson)
                                        .setUri("/backendpool")
                                        .make()
                                        .toString();
        String messageBackend = new MessageBus()
                                        .setParentId(backendPoolId)
                                        .setUri("/backend")
                                        .setEntity(backendJson)
                                        .make()
                                        .toString();

        boolean isOkBackendPool = farm.addToMap(messageBackendPool);
        boolean isOkBackend = farm.addToMap(messageBackend);

        assertThat(isOkBackendPool).as("isOkBackendPool").isTrue();
        assertThat(isOkBackend).as("isOkBackend").isTrue();
        assertThat(farm.getBackendPoolById(backendPoolId)).isNotNull();
        assertThat(farm.getBackendPoolById(backendPoolId).getEntityById(backendId)).isNotNull();
        testComplete();
    }

    @Test
    public void insertNewBackendToAbsentVirtualhostSet() {
        setUp();

        String messageBackend = new MessageBus()
                                    .setParentId(backendPoolId)
                                    .setUri("/backend")
                                    .setEntity(backendJson)
                                    .make()
                                    .toString();

        boolean isOkBackend = farm.addToMap(messageBackend);

        assertThat(isOkBackend).as("isOkBackend").isFalse();
        assertThat(farm.getBackendPoolById(backendPoolId)).isNull();
        testComplete();
    }

    @Test
    public void insertDuplicatedBackendToExistingBackendPool() {
        setUp();

        String messageBackendPool = new MessageBus()
                .setEntity(backendPoolJson)
                .setUri("/backendpool")
                .make()
                .toString();
        String messageBackend = new MessageBus()
                .setParentId(backendPoolId)
                .setUri("/backend")
                .setEntity(backendJson)
                .make()
                .toString();

        boolean isOkBackendPool = farm.addToMap(messageBackendPool);
        boolean isOkBackend = farm.addToMap(messageBackend);
        boolean isOkBackendAddAgain = farm.addToMap(messageBackend);

        assertThat(isOkBackendPool).as("isOkBackendPool").isTrue();
        assertThat(isOkBackend).as("isOkBackend").isTrue();
        assertThat(isOkBackendAddAgain).as("isOkBackendAddAgain").isFalse();
        assertThat(farm.getBackendPoolById(backendPoolId)).isNotNull();
        assertThat(farm.getBackendPoolById(backendPoolId).getEntityById(backendId)).isNotNull();
        testComplete();
    }

    @Test
    public void removeExistingBackendFromExistingBackendPool() throws UnsupportedEncodingException {
        setUp();

        String messageBackendPool = new MessageBus()
                .setEntity(backendPoolJson)
                .setUri("/backendpool")
                .make()
                .toString();

        String messageBackend = new MessageBus()
                .setParentId(backendPoolId)
                .setUri("/backend")
                .setEntity(backendJson)
                .make()
                .toString();

        String messageRemoveBackend = new MessageBus()
                .setParentId(backendPoolId)
                .setUri("/backend/0.0.0.0:00")
                .setEntity(backendJson)
                .make()
                .toString();

        boolean isOkBackendPool = farm.addToMap(messageBackendPool);
        boolean isOkBackend = farm.addToMap(messageBackend);
        boolean isOkBackendRemove = farm.delFromMap(messageRemoveBackend);

        assertThat(isOkBackendPool).as("isOkBackendPool").isTrue();
        assertThat(isOkBackend).as("isOkBackend").isTrue();
        assertThat(isOkBackendRemove).as("isOkBackendRemove").isTrue();
        assertThat(farm.getBackendPoolById(backendPoolId)).isNotNull();
        assertThat(farm.getBackendPoolById(backendPoolId).getEntityById(backendId)).isNull();
        testComplete();

    }

    @Test
    public void removeBackendFromAbsentBackendPool() throws UnsupportedEncodingException {
        setUp();

        String messageRemoveBackend = new MessageBus()
                .setParentId(backendPoolId)
                .setUri("/backend/0.0.0.0:00")
                .setEntity(backendJson)
                .make()
                .toString();

        boolean isOkBackendRemove = farm.delFromMap(messageRemoveBackend);

        assertThat(isOkBackendRemove).as("isOkBackendRemove").isFalse();
        assertThat(farm.getBackendPoolById(backendPoolId)).isNull();
        testComplete();
    }
}
