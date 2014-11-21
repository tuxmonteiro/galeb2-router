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
import static org.vertx.testtools.VertxAssert.testComplete;
import static org.mockito.Mockito.mock;

import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.core.entity.IJsonable;
import com.globo.galeb.core.entity.impl.backend.Backend;
import com.globo.galeb.core.entity.impl.backend.IBackend;
import com.globo.galeb.core.request.RemoteUser;

import org.junit.Test;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

public class BackendTest extends TestVerticle {

    private IQueueService queueService = mock(IQueueService.class);

    private JsonObject buildJsonEntity(String id) {
        return new JsonObject().putString(IJsonable.ID_FIELDNAME, id);
    }

    @Test
    public void equalsObject() {
        Backend backend1 = (Backend) new Backend(buildJsonEntity("127.0.0.1:0")).setPlataform(vertx);
        IBackend backend2 = (IBackend) new Backend(buildJsonEntity("127.0.0.1:0")).setPlataform(vertx);

        assertThat(backend1).isEqualTo(backend2);

        testComplete();
    }

    @Test
    public void notEqualsObject() {
        Backend backend1 = (Backend) new Backend(buildJsonEntity("127.0.0.1:0")).setPlataform(vertx);
        IBackend backend2 = (IBackend) new Backend(buildJsonEntity("127.0.0.2:0")).setPlataform(vertx);

        assertThat(backend1).isNotEqualTo(backend2);

        testComplete();
    }

    @Test
    public void connectReturnNotNull() {
        Backend backendTested = (Backend) new Backend(new JsonObject()).setPlataform(vertx);
        backendTested.setQueueService(queueService);

        HttpClient httpClient = backendTested.connect(new RemoteUser("127.0.0.1", 0));
        assertThat(httpClient).isNotNull();

        testComplete();
    }

    @Test
    public void connectSuccessful() {
        Backend backendTested = (Backend) new Backend(new JsonObject()).setPlataform(vertx);
        backendTested.setQueueService(queueService);

        RemoteUser remoteUser = new RemoteUser("127.0.0.1", 0);
        backendTested.connect(remoteUser);

        assertThat(backendTested.isClosed(remoteUser.toString())).isFalse();

        testComplete();
    }

    @Test
    public void closeSuccessful() {
        Backend backendTested = (Backend) new Backend(new JsonObject()).setPlataform(vertx);
        backendTested.setQueueService(queueService);

        RemoteUser remoteUser = new RemoteUser("127.0.0.1", 0);
        backendTested.connect(remoteUser);
        backendTested.close(remoteUser.toString());

        assertThat(backendTested.isClosed(remoteUser.toString())).isTrue();

        testComplete();
    }

    @Test
    public void multiplesActiveConnections() {
        Backend backendTested = (Backend) new Backend(new JsonObject()).setPlataform(vertx);
        backendTested.setQueueService(queueService);

        for (int counter=0;counter < 1000; counter++) {
            backendTested.connect(new RemoteUser(String.format("%s", counter), 0));
        }

        assertThat(backendTested.getActiveConnections()).isEqualTo(1000);

        testComplete();
    }

    @Test
    public void multiplesRequestsButOneActiveConnection() {
        Backend backendTested = (Backend) new Backend(new JsonObject()).setPlataform(vertx);
        backendTested.setQueueService(queueService);

        for (int counter=0;counter < 1000; counter++) {
            backendTested.connect(new RemoteUser("127.0.0.1", 0));
        }

        assertThat(backendTested.getActiveConnections()).isEqualTo(1);

        testComplete();
    }
}
