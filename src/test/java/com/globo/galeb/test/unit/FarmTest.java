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

import org.junit.Test;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.impl.LogDelegate;
import org.vertx.testtools.TestVerticle;

import com.globo.galeb.core.Backend;
import com.globo.galeb.core.Farm;
import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.core.bus.MessageBus;
import com.globo.galeb.core.entity.IJsonable;
import com.globo.galeb.test.unit.util.FakeLogger;

public class FarmTest extends TestVerticle {

    private JsonObject virtualhostJson = new JsonObject().putString(IJsonable.ID_FIELDNAME, "test.virtualhost.com");
    private String virtualhostId = "test.virtualhost.com";
    private JsonObject backendJson = new JsonObject().putString(IJsonable.ID_FIELDNAME, "0.0.0.0:00");
    private IQueueService queueService = mock(IQueueService.class);
    private LogDelegate logDelegate;
    private FakeLogger logger;
    private Farm farm;

    private void setUp() {
        logDelegate = mock(LogDelegate.class);
        logger = new FakeLogger(logDelegate);
        ((FakeLogger)logger).setQuiet(false);
        ((FakeLogger)logger).setTestId("");

        farm = new Farm(this);
        farm.setQueueService(queueService).setLogger(logger).start();

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
    public void insert10NewBackends() {
        setUp();

        String message = "";
        JsonObject virtualhostJson =
                new JsonObject().putString(IJsonable.ID_FIELDNAME, "test.localdomain");
        message = new MessageBus().setEntity(virtualhostJson)
                                  .setUri("/virtualhost")
                                  .make()
                                  .toString();

        farm.addToMap(message);

        for (int x=0; x<10;x++) {
            JsonObject backendJson =
                    new JsonObject().putString(IJsonable.ID_FIELDNAME, String.format("0:%d", x));
            message = new MessageBus().setEntity(backendJson)
                                      .setParentId("test.localdomain")
                                      .setUri("/backend")
                                      .make()
                                      .toString();

            farm.addToMap(message);
        }

        assertThat(farm.getBackends()).hasSize(10);
        testComplete();
    }

    @Test
    public void insertNewVirtualhostToRouteMap() {
        setUp();

        String uriStr = "/virtualhost";
        String virtualhostId = virtualhostJson.getString(IJsonable.ID_FIELDNAME);
        String message = new MessageBus()
                                .setEntity(virtualhostJson)
                                .setUri(uriStr)
                                .make()
                                .toString();

        boolean isOk = farm.addToMap(message);

        assertThat(farm.getEntities()).containsKey(virtualhostId);
        assertThat(isOk).isTrue();
        testComplete();
    }

    @Test
    public void insertDuplicatedVirtualhostToRouteMap() {
        setUp();

        String uriStr = "/virtualhost";
        String virtualhostId = virtualhostJson.getString(IJsonable.ID_FIELDNAME);
        String message = new MessageBus()
                                .setEntity(virtualhostJson)
                                .setUri(uriStr)
                                .make()
                                .toString();

        farm.addToMap(message);
        boolean isOk = farm.addToMap(message);

        assertThat(farm.getEntities()).containsKey(virtualhostId);
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
        String uriStr = String.format("/virtualhost/%s", virtualhostJson);
        String messageDel = new MessageBus()
                                .setEntity(virtualhostJson)
                                .setUri(uriStr)
                                .make()
                                .toString();

        boolean isOkAdd = farm.addToMap(messageAdd);
        boolean isOkDel = farm.delFromMap(messageDel);

        assertThat(isOkAdd).isTrue();
        assertThat(isOkDel).isTrue();
        assertThat(farm.getEntities()).doesNotContainKey(virtualhostJson.encode());
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

        assertThat(farm.getEntities()).doesNotContainKey(virtualhostJson.encode());
        assertThat(isOk).isFalse();
        testComplete();
    }

    @Test
    public void insertNewBackendToExistingVirtualhostSet() {
        setUp();

        String messageVirtualhost = new MessageBus()
                                        .setEntity(virtualhostJson)
                                        .setUri("/virtualhost")
                                        .make()
                                        .toString();
        String messageBackend = new MessageBus()
                                        .setParentId(virtualhostId)
                                        .setUri("/backend")
                                        .setEntity(backendJson)
                                        .make()
                                        .toString();

        boolean isOkVirtualhost = farm.addToMap(messageVirtualhost);
        boolean isOkBackend = farm.addToMap(messageBackend);

        assertThat(isOkVirtualhost).as("isOkVirtualhost").isTrue();
        assertThat(isOkBackend).as("isOkBackend").isTrue();
        assertThat(farm.getEntities()).containsKey(virtualhostId);
        testComplete();
    }

    @Test
    public void insertNewBackendToAbsentVirtualhostSet() {
        setUp();

        String messageBackend = new MessageBus()
                                    .setParentId(virtualhostId)
                                    .setUri("/backend")
                                    .setEntity(backendJson)
                                    .make()
                                    .toString();

        boolean isOk = farm.addToMap(messageBackend);

        assertThat(farm.getEntities()).doesNotContainKey(virtualhostJson.encode());
        assertThat(isOk).isFalse();
        testComplete();
    }

    @Test
    public void insertDuplicatedBackendToExistingVirtualhostSet() {
        setUp();

        String virtualhostId = virtualhostJson.getString(IJsonable.ID_FIELDNAME);

        String messageVirtualhost = new MessageBus()
                                        .setEntity(virtualhostJson)
                                        .setUri("/virtualhost")
                                        .make()
                                        .toString();
        String messageBackend = new MessageBus()
                                        .setParentId(virtualhostId)
                                        .setUri("/backend")
                                        .setEntity(backendJson)
                                        .make()
                                        .toString();

        boolean isOkVirtualhost = farm.addToMap(messageVirtualhost);
        boolean isOkBackendAdd = farm.addToMap(messageBackend);
        boolean isOkBackendAddAgain = farm.addToMap(messageBackend);
        Virtualhost virtualhost = farm.getEntities().get(virtualhostId);
        Backend backendExpected = (Backend) new Backend(backendJson).setPlataform(vertx);

        assertThat(farm.getEntities()).containsKey(virtualhostId);
        assertThat(virtualhost.getBackends(true).contains(backendExpected)).isTrue();
        assertThat(isOkVirtualhost).as("isOkVirtualhost").isTrue();
        assertThat(isOkBackendAdd).as("isOkBackendAdd").isTrue();
        assertThat(isOkBackendAddAgain).as("isOkBackendRemove").isFalse();
        testComplete();
    }

    @Test
    public void removeExistingBackendFromExistingVirtualhostSet() throws UnsupportedEncodingException {
        setUp();

        String virtualhostId = virtualhostJson.getString(IJsonable.ID_FIELDNAME);

        String messageVirtualhost = new MessageBus()
                                        .setEntity(virtualhostJson)
                                        .setUri("/virtualhost")
                                        .make()
                                        .toString();
        String messageBackend = new MessageBus()
                                        .setParentId(virtualhostId)
                                        .setUri("/backend/0.0.0.0:00")
                                        .setEntity(backendJson)
                                        .make()
                                        .toString();

        boolean isOkVirtualhost = farm.addToMap(messageVirtualhost);
        boolean isOkBackendAdd = farm.addToMap(messageBackend);
        boolean isOkBackendRemove = farm.delFromMap(messageBackend);
        Virtualhost virtualhost = farm.getEntities().get(virtualhostId);
        Backend backendNotExpected = (Backend) new Backend(backendJson).setPlataform(vertx);

        assertThat(farm.getEntities()).containsKey(virtualhostId);
        assertThat(virtualhost.getBackends(true).contains(backendNotExpected)).isFalse();
        assertThat(isOkVirtualhost).as("isOkVirtualhost").isTrue();
        assertThat(isOkBackendAdd).as("isOkBackendAdd").isTrue();
        assertThat(isOkBackendRemove).as("isOkBackendRemove").isTrue();
        testComplete();
    }

    @Test
    public void removeBackendFromAbsentVirtualhostSet() throws UnsupportedEncodingException {
        setUp();

        String messageBackend = new MessageBus()
                                        .setParentId(virtualhostId)
                                        .setUri("/backend/0.0.0.0:00")
                                        .setEntity(backendJson)
                                        .make()
                                        .toString();

        boolean isOk = farm.delFromMap(messageBackend);

        assertThat(farm.getEntities()).doesNotContainKey(virtualhostJson.encode());
        assertThat(isOk).isFalse();
        testComplete();
    }

    @Test
    public void removeAbsentBackendFromVirtualhostSet() throws UnsupportedEncodingException {
        setUp();

        String statusStr = "";
        String virtualhostId = virtualhostJson.getString(IJsonable.ID_FIELDNAME);

        String messageVirtualhost = new MessageBus()
                                            .setEntity(virtualhostJson)
                                            .setUri("/virtualhost/test.localdomain")
                                            .make()
                                            .toString();
        String messageBackend = new MessageBus()
                                            .setParentId(virtualhostId)
                                            .setUri("/backend/0.0.0.0:00")
                                            .setEntity(backendJson)
                                            .make()
                                            .toString();

        boolean isOkVirtualhost = farm.addToMap(messageVirtualhost);
        boolean isOkBackendRemove = farm.delFromMap(messageBackend);
        Virtualhost virtualhost = farm.getEntities().get(virtualhostId);
        Backend backendNotExpected = (Backend) new Backend(backendJson).setPlataform(vertx);

        assertThat(farm.getEntities()).containsKey(virtualhostId);
        assertThat(virtualhost.getBackends(!"0".equals(statusStr)).contains(backendNotExpected)).isFalse();
        assertThat(isOkVirtualhost).as("isOkVirtualhost").isTrue();
        assertThat(isOkBackendRemove).as("isOkBackendRemove").isFalse();
        testComplete();
    }
}
