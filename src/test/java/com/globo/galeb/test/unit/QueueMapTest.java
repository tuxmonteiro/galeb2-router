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
import static org.mockito.Mockito.*;
import static com.globo.galeb.test.unit.assertj.custom.VirtualHostAssert.*;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.globo.galeb.core.IJsonable;
import com.globo.galeb.core.MessageBus;
import com.globo.galeb.core.QueueMap;
import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.loadbalance.impl.DefaultLoadBalancePolicy;
import com.globo.galeb.test.unit.util.FakeLogger;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LogDelegate;
import org.vertx.java.platform.Container;
import org.vertx.java.platform.Verticle;

public class QueueMapTest {

    private Verticle verticle;
    private Vertx vertx;
    private Container container;
    private Logger logger;
    private LogDelegate logDelegate;
    private JsonObject virtualhostJson = new JsonObject().putString(IJsonable.jsonIdFieldName, "test.virtualhost.com");
    private String virtualhostId = "test.virtualhost.com";
    private JsonObject backendJson = new JsonObject().putString(IJsonable.jsonIdFieldName, "0.0.0.0:00");
    private JsonObject properties;

    private Map<String, Virtualhost> virtualhosts = new HashMap<String, Virtualhost>();

    @Before
    public void setUp() {
        verticle = mock(Verticle.class);
        vertx = mock(Vertx.class);
        container = mock(Container.class);
        properties = new JsonObject();
        properties.putString(Virtualhost.loadBalancePolicyFieldName, DefaultLoadBalancePolicy.class.getSimpleName());
        logDelegate = mock(LogDelegate.class);
        logger = new FakeLogger(logDelegate);
        ((FakeLogger)logger).setQuiet(false);
        ((FakeLogger)logger).setTestId("");

        when(verticle.getVertx()).thenReturn(vertx);
        when(verticle.getVertx().eventBus()).thenReturn(null);
        when(verticle.getContainer()).thenReturn(container);
        when(verticle.getContainer().logger()).thenReturn(logger);

        virtualhosts.clear();
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
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOk = queueMap.processAddMessage(message);

        assertThat(virtualhosts).containsKey(virtualhostId);
        assertThat(virtualhosts.get(virtualhostId)).hasProperty(Virtualhost.loadBalancePolicyFieldName);
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

        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        queueMap.processAddMessage(message);
        boolean isOk = queueMap.processAddMessage(message);

        assertThat(virtualhosts).containsKey(virtualhostId);
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

        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOkAdd = queueMap.processAddMessage(messageAdd);
        boolean isOkDel = queueMap.processDelMessage(messageDel);

        assertThat(isOkAdd).isTrue();
        assertThat(isOkDel).isTrue();
        assertThat(virtualhosts).doesNotContainKey(virtualhostJson.encode());
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
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOk = queueMap.processDelMessage(message);

        assertThat(virtualhosts).doesNotContainKey(virtualhostJson.encode());
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

        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOkVirtualhost = queueMap.processAddMessage(messageVirtualhost);
        boolean isOkBackend = queueMap.processAddMessage(messageBackend);
        Virtualhost virtualhost = virtualhosts.get(virtualhostId);

        assertThat(isOkVirtualhost).as("isOkVirtualhost").isTrue();
        assertThat(isOkBackend).as("isOkBackend").isTrue();
        assertThat(virtualhosts).containsKey(virtualhostId);
        assertThat(virtualhost).containsBackend(backendJson, true);

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

        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOk = queueMap.processAddMessage(messageBackend);

        assertThat(virtualhosts).doesNotContainKey(virtualhostJson.encode());
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

        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOkVirtualhost = queueMap.processAddMessage(messageVirtualhost);
        boolean isOkBackendAdd = queueMap.processAddMessage(messageBackend);
        boolean isOkBackendAddAgain = queueMap.processAddMessage(messageBackend);
        Virtualhost virtualhost = virtualhosts.get(virtualhostId);

        assertThat(virtualhosts).containsKey(virtualhostId);
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
                                        .setUri("/backend")
                                        .setEntity(backendJson)
                                        .make()
                                        .toString();

        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOkVirtualhost = queueMap.processAddMessage(messageVirtualhost);
        boolean isOkBackendAdd = queueMap.processAddMessage(messageBackend);
        boolean isOkBackendRemove = queueMap.processDelMessage(messageBackend);
        Virtualhost virtualhost = virtualhosts.get(virtualhostId);

        assertThat(virtualhosts).containsKey(virtualhostId);
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
                                        .setUri("/backend")
                                        .setEntity(backendJson)
                                        .make()
                                        .toString();
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOk = queueMap.processDelMessage(messageBackend);

        assertThat(virtualhosts).doesNotContainKey(virtualhostJson.encode());
        assertThat(isOk).isFalse();
    }

    @Test
    public void removeAbsentBackendFromVirtualhostSet() throws UnsupportedEncodingException {
        ((FakeLogger)logger).setTestId("removeAbsentBackendFromVirtualhostSet");

        String statusStr = "";
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

        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOkVirtualhost = queueMap.processAddMessage(messageVirtualhost);
        boolean isOkBackendRemove = queueMap.processDelMessage(messageBackend);
        Virtualhost virtualhost = virtualhosts.get(virtualhostId);

        assertThat(virtualhosts).containsKey(virtualhostId);
        assertThat(virtualhost).doesNotContainsBackend(backendJson, !"0".equals(statusStr));
        assertThat(isOkVirtualhost).as("isOkVirtualhost").isTrue();
        assertThat(isOkBackendRemove).as("isOkBackendRemove").isFalse();
    }

    // TODO: Implement DEL /route
    @Ignore
    @Test
    public void removeAllRoutes() {
        ((FakeLogger)logger).setTestId("removeAllRoutes");
        ((FakeLogger)logger).setQuiet(true);

        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        for (int idVirtualhost=0; idVirtualhost<10; idVirtualhost++) {

            String aVirtualhostStr = String.format("%d%s", idVirtualhost, virtualhostJson);
            String messageVirtualhost = new MessageBus()
                                                .setEntity(aVirtualhostStr)
                                                .setUri("/virtualhost")
                                                .make()
                                                .toString();

            queueMap.processAddMessage(messageVirtualhost);

            for (int idBackend=0; idBackend<10; idBackend++) {
                String newBackendStr = String.format("%s:%d", backendJson.getString(IJsonable.jsonIdFieldName).split(":")[0], idBackend);
                JsonObject newBackendJson = new JsonObject().putString(IJsonable.jsonIdFieldName, newBackendStr);
                String messageBackend = new MessageBus()
                                                .setParentId(virtualhostId)
                                                .setUri("/backend")
                                                .setEntity(newBackendJson)
                                                .make()
                                                .toString();
                queueMap.processAddMessage(messageBackend);
            }
        }
        String messageDelRoutes = new MessageBus().make().toString();
        queueMap.processDelMessage(messageDelRoutes);

        assertThat(virtualhosts).hasSize(0);
    }

}
