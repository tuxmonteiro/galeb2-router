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

import static com.globo.galeb.test.unit.assertj.custom.FarmAssert.assertThat;
import static com.globo.galeb.test.unit.assertj.custom.VirtualHostAssert.assertThat;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LogDelegate;
import org.vertx.java.platform.Container;
import org.vertx.java.platform.Verticle;

import com.globo.galeb.core.Farm;
import com.globo.galeb.core.IJsonable;
import com.globo.galeb.core.SafeJsonObject;
import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.core.bus.MessageBus;
import com.globo.galeb.loadbalance.impl.DefaultLoadBalancePolicy;
import com.globo.galeb.test.unit.util.FakeLogger;

public class FarmTest {

    private Farm farm;
    private Verticle verticle;
    private Vertx vertx;
    private Container container;
    private Logger logger;
    private LogDelegate logDelegate;
    private SafeJsonObject virtualhostJson = new SafeJsonObject().putString(IJsonable.jsonIdFieldName, "test.virtualhost.com");
    private String virtualhostId = "test.virtualhost.com";
    private SafeJsonObject backendJson = new SafeJsonObject().putString(IJsonable.jsonIdFieldName, "0.0.0.0:00");
    private SafeJsonObject properties;

    @Before
    public void setUp() throws Exception {

        verticle = mock(Verticle.class);
        vertx = mock(Vertx.class);
        container = mock(Container.class);
        properties = new SafeJsonObject();
        properties.putString(Virtualhost.loadBalancePolicyFieldName, DefaultLoadBalancePolicy.class.getSimpleName());
        logDelegate = mock(LogDelegate.class);
        logger = new FakeLogger(logDelegate);
        ((FakeLogger)logger).setQuiet(false);
        ((FakeLogger)logger).setTestId("");

        when(verticle.getVertx()).thenReturn(vertx);
        when(verticle.getVertx().eventBus()).thenReturn(null);
        when(verticle.getContainer()).thenReturn(container);
        when(verticle.getContainer().logger()).thenReturn(logger);
        when(verticle.getContainer().config()).thenReturn(new SafeJsonObject());
        when(verticle.toString()).thenReturn(this.getClass().toString());
        when(verticle.getContainer().config()).thenReturn(new JsonObject());

        farm = new Farm(verticle);

    }

    @Test
    public void insert10NewVirtualhost() {

        for (int x=0; x<10;x++) {
            SafeJsonObject virtualhostJson =
                    new SafeJsonObject().putString(IJsonable.jsonIdFieldName, String.valueOf(x));
            String message = new MessageBus()
                                    .setEntity(virtualhostJson)
                                    .setUri("/virtualhost")
                                    .make()
                                    .toString();

            farm.addToMap(message);
        }

        assertThat(farm).hasVirtualhostsSize(10);
    }

    @Test
    public void insert10NewBackends() {

        String message = "";
        SafeJsonObject virtualhostJson =
                new SafeJsonObject().putString(IJsonable.jsonIdFieldName, "test.localdomain");
        message = new MessageBus().setEntity(virtualhostJson)
                                  .setUri("/virtualhost")
                                  .make()
                                  .toString();

        farm.addToMap(message);

        for (int x=0; x<10;x++) {
            SafeJsonObject backendJson =
                    new SafeJsonObject().putString(IJsonable.jsonIdFieldName, String.format("0:%d", x));
            message = new MessageBus().setEntity(backendJson)
                                      .setParentId("test.localdomain")
                                      .setUri("/backend")
                                      .make()
                                      .toString();

            farm.addToMap(message);
        }

        assertThat(farm).hasBackendsSize(10);
    }

    @Test
    public void insertNewVirtualhostToRouteMap() {
        ((FakeLogger)logger).setTestId("insertNewVirtualhostToRouteMap");

        String uriStr = "/virtualhost";
        String virtualhostId = virtualhostJson.getString(IJsonable.jsonIdFieldName);
        String message = new MessageBus()
                                .setEntity(virtualhostJson)
                                .setUri(uriStr)
                                .make()
                                .toString();

        boolean isOk = farm.addToMap(message);

        assertThat(farm.getVirtualhostsToMap()).containsKey(virtualhostId);
        assertThat(isOk).isTrue();
    }

    @Test
    public void insertDuplicatedVirtualhostToRouteMap() {
        ((FakeLogger)logger).setTestId("insertDuplicatedVirtualhostToRouteMap");

        String uriStr = "/virtualhost";
        String virtualhostId = virtualhostJson.getString(IJsonable.jsonIdFieldName);
        String message = new MessageBus()
                                .setEntity(virtualhostJson)
                                .setUri(uriStr)
                                .make()
                                .toString();

        farm.addToMap(message);
        boolean isOk = farm.addToMap(message);

        assertThat(farm.getVirtualhostsToMap()).containsKey(virtualhostId);
        assertThat(isOk).isFalse();
    }

    @Test
    public void removeExistingVirtualhostFromRouteMap() {
        ((FakeLogger)logger).setTestId("removeExistingVirtualhostFromRouteMap");

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
        assertThat(farm.getVirtualhostsToMap()).doesNotContainKey(virtualhostJson.encode());
    }

    @Test
    public void removeAbsentVirtualhostFromRouteMap() {
        ((FakeLogger)logger).setTestId("removeAbsentVirtualhostFromRouteMap");

        String uriStr = String.format("/virtualhost/%s", virtualhostJson);
        String message = new MessageBus()
                                .setEntity(virtualhostJson)
                                .setUri(uriStr)
                                .make()
                                .toString();

        boolean isOk = farm.delFromMap(message);

        assertThat(farm.getVirtualhostsToMap()).doesNotContainKey(virtualhostJson.encode());
        assertThat(isOk).isFalse();
    }

    @Test
    public void insertNewBackendToExistingVirtualhostSet() {
        ((FakeLogger)logger).setTestId("insertNewBackendToExistingVirtualhostSet");

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
        assertThat(farm.getVirtualhostsToMap()).containsKey(virtualhostId);

    }

    @Test
    public void insertNewBackendToAbsentVirtualhostSet() {
        ((FakeLogger)logger).setTestId("insertNewBackendToAbsentVirtualhostSet");

        String messageBackend = new MessageBus()
                                    .setParentId(virtualhostId)
                                    .setUri("/backend")
                                    .setEntity(backendJson)
                                    .make()
                                    .toString();

        boolean isOk = farm.addToMap(messageBackend);

        assertThat(farm.getVirtualhostsToMap()).doesNotContainKey(virtualhostJson.encode());
        assertThat(isOk).isFalse();
    }

    @Test
    public void insertDuplicatedBackendToExistingVirtualhostSet() {
        ((FakeLogger)logger).setTestId("insertDuplicatedBackendToExistingVirtualhostSet");

        String virtualhostId = virtualhostJson.getString(IJsonable.jsonIdFieldName);

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
        Virtualhost virtualhost = farm.getVirtualhostsToMap().get(virtualhostId);

        assertThat(farm.getVirtualhostsToMap()).containsKey(virtualhostId);
        assertThat(virtualhost).containsBackend(backendJson, true);
        assertThat(isOkVirtualhost).as("isOkVirtualhost").isTrue();
        assertThat(isOkBackendAdd).as("isOkBackendAdd").isTrue();
        assertThat(isOkBackendAddAgain).as("isOkBackendRemove").isFalse();
    }

    @Test
    public void removeExistingBackendFromExistingVirtualhostSet() throws UnsupportedEncodingException {
        ((FakeLogger)logger).setTestId("removeExistingBackendFromExistingVirtualhostSet");

        String virtualhostId = virtualhostJson.getString(IJsonable.jsonIdFieldName);

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
        Virtualhost virtualhost = farm.getVirtualhostsToMap().get(virtualhostId);

        assertThat(farm.getVirtualhostsToMap()).containsKey(virtualhostId);
        assertThat(virtualhost).doesNotContainsBackend(backendJson, true);
        assertThat(isOkVirtualhost).as("isOkVirtualhost").isTrue();
        assertThat(isOkBackendAdd).as("isOkBackendAdd").isTrue();
        assertThat(isOkBackendRemove).as("isOkBackendRemove").isTrue();
    }

    @Test
    public void removeBackendFromAbsentVirtualhostSet() throws UnsupportedEncodingException {
        ((FakeLogger)logger).setTestId("removeBackendFromAbsentVirtualhostSet");

        String messageBackend = new MessageBus()
                                        .setParentId(virtualhostId)
                                        .setUri("/backend/0.0.0.0:00")
                                        .setEntity(backendJson)
                                        .make()
                                        .toString();

        boolean isOk = farm.delFromMap(messageBackend);

        assertThat(farm.getVirtualhostsToMap()).doesNotContainKey(virtualhostJson.encode());
        assertThat(isOk).isFalse();
    }

    @Test
    public void removeAbsentBackendFromVirtualhostSet() throws UnsupportedEncodingException {
        ((FakeLogger)logger).setTestId("removeAbsentBackendFromVirtualhostSet");

        String statusStr = "";
        String virtualhostId = virtualhostJson.getString(IJsonable.jsonIdFieldName);

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
        Virtualhost virtualhost = farm.getVirtualhostsToMap().get(virtualhostId);

        assertThat(farm.getVirtualhostsToMap()).containsKey(virtualhostId);
        assertThat(virtualhost).doesNotContainsBackend(backendJson, !"0".equals(statusStr));
        assertThat(isOkVirtualhost).as("isOkVirtualhost").isTrue();
        assertThat(isOkBackendRemove).as("isOkBackendRemove").isFalse();
    }
}
