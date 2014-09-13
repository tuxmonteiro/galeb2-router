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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.vertx.testtools.VertxAssert.testComplete;
import com.globo.galeb.core.Backend;
import com.globo.galeb.core.RequestData;
import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.list.UniqueArrayList;
import com.globo.galeb.loadbalance.impl.LeastConnPolicy;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.impl.DefaultVertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

public class LeastConnPolicyTest extends TestVerticle {

    private Virtualhost virtualhost;
    private int numBackends = 10;
    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = mock(DefaultVertx.class);
    }

    @Test
    public void leastConnection() {

        JsonObject virtualhostProperties = new JsonObject()
            .putString(Virtualhost.loadBalancePolicyFieldName, LeastConnPolicy.class.getSimpleName());
        JsonObject virtualhostJson = new JsonObject()
            .putString("virtualhost", "test.localdomain")
            .putObject("properties", virtualhostProperties);
        virtualhost = new Virtualhost(virtualhostJson, vertx);

        for (int x=0; x<numBackends; x++) {
            virtualhost.addBackend(String.format("0:%s", x), true);
            Backend backend = virtualhost.getBackends(true).get(x);
            for (int c = 1; c <= x+1; c++) {
                backend.connect("0", String.format("%s", c));
            }
        }

        for (int c=1 ; c<=1000; c++) {

            Backend backendWithLeastConn = virtualhost.getChoice(new RequestData());
            int numConnectionsInBackendWithLeastConn = backendWithLeastConn.getSessionController().getActiveConnections();

            UniqueArrayList<Backend> backends = virtualhost.getBackends(true);
            for (Backend backendSample: backends) {

                int numConnectionsInBackendSample = backendSample.getSessionController().getActiveConnections();
                if (backendSample!=backendWithLeastConn) {
                    assertThat(numConnectionsInBackendWithLeastConn).isEqualTo(1);
                    assertThat(numConnectionsInBackendWithLeastConn)
                                .isLessThan(numConnectionsInBackendSample);

                }

            }
        }

        testComplete();


    }

}
