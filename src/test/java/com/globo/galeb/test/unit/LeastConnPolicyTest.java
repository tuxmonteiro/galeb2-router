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
import static org.vertx.testtools.VertxAssert.testComplete;
import static org.mockito.Mockito.mock;

import com.globo.galeb.core.Backend;
import com.globo.galeb.core.RemoteUser;
import com.globo.galeb.core.RequestData;
import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.core.entity.IJsonable;
import com.globo.galeb.list.UniqueArrayList;
import com.globo.galeb.loadbalance.impl.LeastConnPolicy;

import org.junit.Test;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

public class LeastConnPolicyTest extends TestVerticle {

    private Virtualhost virtualhost;
    private int numBackends = 10;

    @Test
    public void leastConnection() {

        JsonObject virtualhostProperties = new JsonObject()
            .putString(Virtualhost.LOADBALANCE_POLICY_FIELDNAME, LeastConnPolicy.class.getSimpleName());
        JsonObject virtualhostJson = new JsonObject()
            .putString(IJsonable.ID_FIELDNAME, "test.localdomain")
            .putObject(IJsonable.PROPERTIES_FIELDNAME, virtualhostProperties);
        virtualhost = new Virtualhost(virtualhostJson, vertx);

        for (int x=0; x<numBackends; x++) {
            virtualhost.addBackend(String.format("0:%s", x), true);
            virtualhost.setQueue(mock(IQueueService.class));
            Backend backend = virtualhost.getBackends(true).get(x);
            for (int c = 1; c <= x+1; c++) {
                backend.connect(new RemoteUser("0",c));
            }
        }

        for (int c=1 ; c<=1000; c++) {

            Backend backendWithLeastConn = virtualhost.getChoice(new RequestData());
            int numConnectionsInBackendWithLeastConn = backendWithLeastConn.getActiveConnections();

            UniqueArrayList<Backend> backends = virtualhost.getBackends(true);
            for (Backend backendSample: backends) {

                int numConnectionsInBackendSample = backendSample.getActiveConnections();
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
