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

import com.globo.galeb.core.Backend;
import com.globo.galeb.core.RemoteUser;
import com.globo.galeb.core.bus.IQueueService;

import org.junit.Test;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

public class BackendTest extends TestVerticle {

    private IQueueService queueService = mock(IQueueService.class);

    @Test
    public void equalsObject() {
        Backend backend1 = new Backend("127.0.0.1:0", vertx);
        Backend backend2 = new Backend("127.0.0.1:0", vertx);

        assertThat(backend1).isEqualTo(backend2);

        testComplete();
    }

    @Test
    public void notEqualsObject() {
        Backend backend1 = new Backend("127.0.0.1:0", vertx);
        Backend backend2 = new Backend("127.0.0.2:0", vertx);

        assertThat(backend1).isNotEqualTo(backend2);

        testComplete();
    }

    @Test
    public void isKeepAliveLimitMaxRequest() {
        Backend backendTested = new Backend("127.0.0.1:0", vertx);

        backendTested.setKeepAliveMaxRequest(1L);
        backendTested.connect();
        boolean isKeepAliveLimitExceeded = backendTested.isKeepAliveLimit();

        assertThat(isKeepAliveLimitExceeded).isTrue();

        testComplete();
    }

    @Test
    public void isNotKeepAliveLimitMaxRequest() {
        Backend backendTested = new Backend("127.0.0.1:0", vertx);

        backendTested.setKeepAliveMaxRequest(2L);
        boolean isKeepAliveLimitExceeded = backendTested.isKeepAliveLimit();

        assertThat(isKeepAliveLimitExceeded).isFalse();

        testComplete();
    }

    @Test
    public void isKeepAliveLimitTimeOut() {
        Backend backendTested = new Backend("127.0.0.1:0", vertx);

        backendTested.setKeepAliveTimeOut(-1L);
        backendTested.connect();
        boolean isKeepAliveLimitExceeded = backendTested.isKeepAliveLimit();

        assertThat(isKeepAliveLimitExceeded).isTrue();

        testComplete();
    }

    @Test
    public void isNotKeepAliveLimitTimeOut() {
        Backend backendTested = new Backend("127.0.0.1:0", vertx);

        backendTested.setKeepAliveTimeOut(86400000L);
        boolean isKeepAliveLimitExceeded = backendTested.isKeepAliveLimit();

        assertThat(isKeepAliveLimitExceeded).isFalse();

        testComplete();
    }

    @Test
    public void connectReturnNotNull() {
        Backend backendTested = new Backend(new JsonObject(), vertx);
        backendTested.setQueueService(queueService);

        backendTested.setRemoteUser(new RemoteUser("127.0.0.1", 0));
        HttpClient httpClient = backendTested.connect();
        assertThat(httpClient).isNotNull();

        testComplete();
    }

    @Test
    public void connectSuccessful() {
        Backend backendTested = new Backend(new JsonObject(), vertx);
        backendTested.setQueueService(queueService);

        backendTested.setRemoteUser(new RemoteUser("127.0.0.1", 0));
        backendTested.connect();

        assertThat(backendTested.isClosed()).isFalse();

        testComplete();
    }

    @Test
    public void closeSuccessful() {
        Backend backendTested = new Backend(new JsonObject(), vertx);
        backendTested.setQueueService(queueService);

        backendTested.setRemoteUser(new RemoteUser("127.0.0.1", 0));
        backendTested.connect();
        backendTested.close();

        assertThat(backendTested.isClosed()).isTrue();

        testComplete();
    }

    @Test
    public void multiplesActiveConnections() {
        Backend backendTested = new Backend(new JsonObject(), vertx);
        backendTested.setQueueService(queueService);

        for (int counter=0;counter < 1000; counter++) {
            backendTested.setRemoteUser(new RemoteUser(String.format("%s", counter), 0));
            backendTested.connect();
        }

        assertThat(backendTested.getActiveConnections()).isEqualTo(1000);

        testComplete();
    }

    @Test
    public void multiplesRequestsButOneActiveConnection() {
        Backend backendTested = new Backend(new JsonObject(), vertx);
        backendTested.setQueueService(queueService);

        for (int counter=0;counter < 1000; counter++) {
            backendTested.setRemoteUser(new RemoteUser("127.0.0.1", 0));
            backendTested.connect();
        }

        assertThat(backendTested.getActiveConnections()).isEqualTo(1);

        testComplete();
    }

    @Test
    public void zeroActiveConnectionsBecauseMaxRequestsExceeded() {
        Backend backendTested = new Backend("127.0.0.1:0", vertx);
        backendTested.setQueueService(queueService);

        backendTested.setKeepAliveMaxRequest(1000L);
        for (int counter=0;counter < 1000; counter++) {
            backendTested.setRemoteUser(new RemoteUser(String.format("%s", counter), 0));
            backendTested.connect();
            boolean isKeepAliveLimitExceeded = backendTested.isKeepAliveLimit();
            if (isKeepAliveLimitExceeded) {
                backendTested.close();
                break;
            }
        }

        assertThat(backendTested.getActiveConnections()).isEqualTo(0);
        testComplete();
    }

    @Test
    public void multiplesActiveConnectionsBecauseMaxRequestsNotExceeded() {
        Backend backendTested = new Backend("127.0.0.1:0", vertx);
        backendTested.setQueueService(queueService);

        backendTested.setKeepAliveMaxRequest(1001L);
        for (int counter=0;counter < 1000; counter++) {
            backendTested.setRemoteUser(new RemoteUser(String.format("%s", counter), 0));
            backendTested.connect();
            boolean isKeepAliveLimitExceeded = backendTested.isKeepAliveLimit();
            if (isKeepAliveLimitExceeded) {
                backendTested.close();
                break;
            }
        }

        assertThat(backendTested.getActiveConnections()).isNotEqualTo(0);

        testComplete();
    }

    @Test
    public void zeroActiveConnectionsBecauseTimeoutExceeded() {
        Backend backendTested = new Backend("127.0.0.1:0", vertx);
        backendTested.setKeepAliveTimeOut(-1L);
        backendTested.setQueueService(queueService);

        for (int counter=0;counter < 1000; counter++) {
            backendTested.setRemoteUser(new RemoteUser(String.format("%s", counter), 0));
            backendTested.connect();
            if (backendTested.isKeepAliveLimit()) {
                backendTested.close();
                break;
            }
        }

        assertThat(backendTested.getActiveConnections()).isEqualTo(0);

        testComplete();
    }

    @Test
    public void multiplesActiveConnectionsBecauseTimeoutNotExceeded() {
        Backend backendTested = new Backend("127.0.0.1:0", vertx);
        backendTested.setQueueService(queueService);
        backendTested.setKeepAliveTimeOut(86400000L); // one day

        for (int counter=0;counter < 1000; counter++) {
            backendTested.setRemoteUser(new RemoteUser(String.format("%s", counter), 0));
            backendTested.connect();
            if (backendTested.isKeepAliveLimit()) {
                backendTested.close();
                break;
            }
        }

        assertThat(backendTested.getActiveConnections()).isNotEqualTo(0);

        testComplete();
    }
}
