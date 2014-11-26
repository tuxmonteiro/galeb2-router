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

import com.globo.galeb.bus.IQueueService;
import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.entity.impl.backend.BackendWithoutPool;
import com.globo.galeb.entity.impl.backend.IBackend;
import com.globo.galeb.request.RemoteUser;

import org.junit.Test;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

public class BackendWithoutPoolTest extends TestVerticle {

    private IQueueService queueService = mock(IQueueService.class);

    private JsonObject buildJsonEntity(String id) {
        return new JsonObject().putString(IJsonable.ID_FIELDNAME, id);
    }

    @Test
    public void equalsObject() {
        BackendWithoutPool backend1 = (BackendWithoutPool) new BackendWithoutPool(buildJsonEntity("127.0.0.1:0")).setPlataform(vertx);
        IBackend backend2 = (IBackend) new BackendWithoutPool(buildJsonEntity("127.0.0.1:0")).setPlataform(vertx);

        assertThat(backend1).isEqualTo(backend2);

        testComplete();
    }

    @Test
    public void notEqualsObject() {
        BackendWithoutPool backend1 = (BackendWithoutPool) new BackendWithoutPool(buildJsonEntity("127.0.0.1:0")).setPlataform(vertx);
        IBackend backend2 = (IBackend) new BackendWithoutPool(buildJsonEntity("127.0.0.2:0")).setPlataform(vertx);

        assertThat(backend1).isNotEqualTo(backend2);

        testComplete();
    }

    @Test
    public void connectReturnNotNull() {
        BackendWithoutPool backendTested = (BackendWithoutPool) new BackendWithoutPool(new JsonObject()).setPlataform(vertx);
        backendTested.setQueueService(queueService);

        HttpClient httpClient = backendTested.connect(new RemoteUser("127.0.0.1", 0));
        assertThat(httpClient).isNotNull();

        testComplete();
    }

    @Test
    public void connectSuccessful() {
        BackendWithoutPool backendTested = (BackendWithoutPool) new BackendWithoutPool(new JsonObject()).setPlataform(vertx);
        backendTested.setQueueService(queueService);

        RemoteUser remoteUser = new RemoteUser("127.0.0.1", 0);
        backendTested.connect(remoteUser);

        assertThat(backendTested.isClosed(remoteUser.toString())).isFalse();

        testComplete();
    }

    @Test
    public void closeSuccessful() {
        BackendWithoutPool backendTested = (BackendWithoutPool) new BackendWithoutPool(new JsonObject()).setPlataform(vertx);
        backendTested.setQueueService(queueService);

        RemoteUser remoteUser = new RemoteUser("127.0.0.1", 0);
        backendTested.connect(remoteUser);
        backendTested.close(remoteUser.toString());

        assertThat(backendTested.isClosed(remoteUser.toString())).isTrue();

        testComplete();
    }

    @Test
    public void multiplesActiveConnections() {
        BackendWithoutPool backendTested = (BackendWithoutPool) new BackendWithoutPool(new JsonObject()).setPlataform(vertx);
        backendTested.setQueueService(queueService);

        for (int counter=0;counter < 1000; counter++) {
            backendTested.connect(new RemoteUser(String.format("%s", counter), 0));
        }

        assertThat(backendTested.getActiveConnections()).isEqualTo(1000);

        testComplete();
    }

    @Test
    public void multiplesRequestsButOneActiveConnection() {
        BackendWithoutPool backendTested = (BackendWithoutPool) new BackendWithoutPool(new JsonObject()).setPlataform(vertx);
        backendTested.setQueueService(queueService);

        for (int counter=0;counter < 1000; counter++) {
            backendTested.connect(new RemoteUser("127.0.0.1", 0));
        }

        assertThat(backendTested.getActiveConnections()).isEqualTo(1);

        testComplete();
    }
}
