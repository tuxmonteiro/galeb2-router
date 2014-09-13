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
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

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
    private String virtualhostStr = "test.virtualhost.com";
    private String backendJson = new JsonObject().putString("id", "0.0.0.0:00").encode();
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
        String message = QueueMap.buildMessage(virtualhostStr, backendJson, uriStr, properties.toString());
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOk = queueMap.processAddMessage(message);

        assertThat(virtualhosts).containsKey(virtualhostStr);
        assertThat(virtualhosts.get(virtualhostStr)).hasProperty(Virtualhost.loadBalancePolicyFieldName);
        assertThat(isOk).isTrue();
    }

    @Test
    public void insertDuplicatedVirtualhostToRouteMap() {
        ((FakeLogger)logger).setTestId("insertDuplicatedVirtualhostToRouteMap");

        String uriStr = "/virtualhost";
        String message = QueueMap.buildMessage(virtualhostStr, backendJson, uriStr, properties.toString());
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        queueMap.processAddMessage(message);
        boolean isOk = queueMap.processAddMessage(message);

        assertThat(virtualhosts).containsKey(virtualhostStr);
        assertThat(isOk).isFalse();
    }

    @Test
    public void removeExistingVirtualhostFromRouteMap() {
        ((FakeLogger)logger).setTestId("removeExistingVirtualhostFromRouteMap");

        String uriStr = String.format("/virtualhost/%s", virtualhostStr);
        String backendStr = "{}";
        String messageAdd = QueueMap.buildMessage(virtualhostStr, backendStr, uriStr, properties.toString());
        String messageDel = QueueMap.buildMessage(virtualhostStr, backendStr, uriStr, "");

        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOkAdd = queueMap.processAddMessage(messageAdd);
        boolean isOkDel = queueMap.processDelMessage(messageDel);

        assertThat(isOkAdd).isTrue();
        assertThat(isOkDel).isTrue();
        assertThat(virtualhosts).doesNotContainKey(virtualhostStr);
    }

    @Test
    public void removeAbsentVirtualhostFromRouteMap() {
        ((FakeLogger)logger).setTestId("removeAbsentVirtualhostFromRouteMap");

        String uriStr = String.format("/virtualhost/%s", virtualhostStr);
        String backendStr = "{}";
        String properties = "";
        String message = QueueMap.buildMessage(virtualhostStr, backendStr, uriStr, properties);
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOk = queueMap.processDelMessage(message);

        assertThat(virtualhosts).doesNotContainKey(virtualhostStr);
        assertThat(isOk).isFalse();
    }

    @Test
    public void insertNewBackendToExistingVirtualhostSet() {
        ((FakeLogger)logger).setTestId("insertNewBackendToExistingVirtualhostSet");

        String messageVirtualhost = QueueMap.buildMessage(virtualhostStr, "{}", "/virtualhost", "{}");
        String messageBackend = QueueMap.buildMessage(virtualhostStr, backendJson, "/backend", "{}");
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOkVirtualhost = queueMap.processAddMessage(messageVirtualhost);
        boolean isOkBackend = queueMap.processAddMessage(messageBackend);
        Virtualhost virtualhost = virtualhosts.get(virtualhostStr);
        JsonObject backend = new JsonObject(backendJson);

        assertThat(isOkVirtualhost).as("isOkVirtualhost").isTrue();
        assertThat(isOkBackend).as("isOkBackend").isTrue();
        assertThat(virtualhosts).containsKey(virtualhostStr);
        assertThat(virtualhost).containsBackend(backend.getString("id"), true);

    }

    @Test
    public void insertNewBackendToAbsentVirtualhostSet() {
        ((FakeLogger)logger).setTestId("insertNewBackendToAbsentVirtualhostSet");

        String messageBackend = QueueMap.buildMessage(virtualhostStr, backendJson, "/backend", "{}");
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOk = queueMap.processAddMessage(messageBackend);

        assertThat(virtualhosts).doesNotContainKey(virtualhostStr);
        assertThat(isOk).isFalse();
    }

    @Test
    public void insertDuplicatedBackendToExistingVirtualhostSet() {
        ((FakeLogger)logger).setTestId("insertDuplicatedBackendToExistingVirtualhostSet");

        String messageVirtualhost = QueueMap.buildMessage(virtualhostStr, "{}","/virtualhost", "{}");
        String messageBackend = QueueMap.buildMessage(virtualhostStr, backendJson, "/backend", "{}");
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOkVirtualhost = queueMap.processAddMessage(messageVirtualhost);
        boolean isOkBackendAdd = queueMap.processAddMessage(messageBackend);
        boolean isOkBackendAddAgain = queueMap.processAddMessage(messageBackend);
       Virtualhost virtualhost = virtualhosts.get(virtualhostStr);
       JsonObject backend = new JsonObject(backendJson);

        assertThat(virtualhosts).containsKey(virtualhostStr);
        assertThat(virtualhost).containsBackend(backend.getString("id"), true);
        assertThat(isOkVirtualhost).as("isOkVirtualhost").isTrue();
        assertThat(isOkBackendAdd).as("isOkBackendAdd").isTrue();
        assertThat(isOkBackendAddAgain).as("isOkBackendRemove").isFalse();
    }

    @Test
    public void removeExistingBackendFromExistingVirtualhostSet() throws UnsupportedEncodingException {
        ((FakeLogger)logger).setTestId("removeExistingBackendFromExistingVirtualhostSet");

        String messageVirtualhost = QueueMap.buildMessage(virtualhostStr, "{}","/virtualhost", "{}");
        String messageBackend = QueueMap.buildMessage(virtualhostStr,
                                                   backendJson,
                                                   String.format("/backend/%s", URLEncoder.encode(backendJson,"UTF-8")),
                                                   "{}");
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOkVirtualhost = queueMap.processAddMessage(messageVirtualhost);
        boolean isOkBackendAdd = queueMap.processAddMessage(messageBackend);
        boolean isOkBackendRemove = queueMap.processDelMessage(messageBackend);
        Virtualhost virtualhost = virtualhosts.get(virtualhostStr);
        JsonObject backend = new JsonObject(backendJson);

        assertThat(virtualhosts).containsKey(virtualhostStr);
        assertThat(virtualhost).doesNotContainsBackend(backend.getString("id"), true);
        assertThat(isOkVirtualhost).as("isOkVirtualhost").isTrue();
        assertThat(isOkBackendAdd).as("isOkBackendAdd").isTrue();
        assertThat(isOkBackendRemove).as("isOkBackendRemove").isTrue();
    }

    @Test
    public void removeBackendFromAbsentVirtualhostSet() throws UnsupportedEncodingException {
        ((FakeLogger)logger).setTestId("removeBackendFromAbsentVirtualhostSet");

        String messageBackend = QueueMap.buildMessage(virtualhostStr,
                                                    backendJson,
                                                    String.format("/backend/%s", URLEncoder.encode(backendJson,"UTF-8")),
                                                    "{}");
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOk = queueMap.processDelMessage(messageBackend);

        assertThat(virtualhosts).doesNotContainKey(virtualhostStr);
        assertThat(isOk).isFalse();
    }

    @Test
    public void removeAbsentBackendFromVirtualhostSet() throws UnsupportedEncodingException {
        ((FakeLogger)logger).setTestId("removeAbsentBackendFromVirtualhostSet");

        String statusStr = "";
        String messageVirtualhost = QueueMap.buildMessage(virtualhostStr, "{}", "/virtualhost", "{}");
        String messageBackend = QueueMap.buildMessage(virtualhostStr,
                                                   backendJson,
                                                   String.format("/backend/%s", URLEncoder.encode(backendJson,"UTF-8")),
                                                   "{}");
        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        boolean isOkVirtualhost = queueMap.processAddMessage(messageVirtualhost);
        boolean isOkBackendRemove = queueMap.processDelMessage(messageBackend);
        Virtualhost virtualhost = virtualhosts.get(virtualhostStr);

        assertThat(virtualhosts).containsKey(virtualhostStr);
        assertThat(virtualhost).doesNotContainsBackend(backendJson, !"0".equals(statusStr));
        assertThat(isOkVirtualhost).as("isOkVirtualhost").isTrue();
        assertThat(isOkBackendRemove).as("isOkBackendRemove").isFalse();
    }

    @Ignore
    @Test
    public void removeAllRoutes() {
        ((FakeLogger)logger).setTestId("removeAllRoutes");
        ((FakeLogger)logger).setQuiet(true);

        QueueMap queueMap = new QueueMap(verticle, virtualhosts);

        for (int idVirtualhost=0; idVirtualhost<10; idVirtualhost++) {

            String aVirtualhostStr = String.format("%d%s", idVirtualhost, virtualhostStr);
            String messageVirtualhost = QueueMap.buildMessage(
                    aVirtualhostStr, "{}", "/virtualhost", "{}");

            queueMap.processAddMessage(messageVirtualhost);

            for (int idBackend=0; idBackend<10; idBackend++) {
                JsonObject backend = new JsonObject(backendJson);
                String newBackendStr = String.format("%s:%d", backend.getString("id").split(":")[0], idBackend);
                JsonObject newBackendJson = new JsonObject().putString("id", newBackendStr);
                String messageBackend = QueueMap.buildMessage(
                        aVirtualhostStr, newBackendJson.encode(), "/backend","{}");
                queueMap.processAddMessage(messageBackend);
            }
        }
        String messageDelRoutes = QueueMap.buildMessage("", "{}", "/route", "{}");
        queueMap.processDelMessage(messageDelRoutes);

        assertThat(virtualhosts).hasSize(0);
    }

    @Test
    public void validateBuildMessage() {
        ((FakeLogger)logger).setTestId("validateBuildMessage");
        String uriStr = "/test";
        JsonObject properties = new JsonObject();

        String message = QueueMap.buildMessage(virtualhostStr,
                                               backendJson,
                                               uriStr,
                                               properties.toString());

        JsonObject messageJsonOrig = new JsonObject(message);
        JsonObject messageJson = new JsonObject();
        JsonObject virtualhostObj = new JsonObject().putString("id", virtualhostStr);

        virtualhostObj.putObject("properties", properties);
        messageJson.putString("virtualhost", virtualhostObj.encode());
        messageJson.putString("backend", backendJson);
        messageJson.putString("uri", uriStr);

        assertThat(messageJsonOrig).isEqualTo(messageJson);
    }
}
