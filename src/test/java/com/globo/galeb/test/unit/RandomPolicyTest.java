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

import com.globo.galeb.core.IJsonable;
import com.globo.galeb.core.RequestData;
import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.loadbalance.impl.RandomPolicy;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.impl.DefaultVertx;
import org.vertx.java.core.json.JsonObject;

public class RandomPolicyTest {

    Virtualhost virtualhost;
    int numBackends = 10;

    @Before
    public void setUp() throws Exception {

        Vertx vertx = mock(DefaultVertx.class);

        JsonObject virtualhostProperties = new JsonObject()
            .putString(Virtualhost.loadBalancePolicyFieldName, RandomPolicy.class.getSimpleName());
        JsonObject virtualhostJson = new JsonObject()
            .putString(IJsonable.jsonIdFieldName, "test.localdomain")
            .putObject(IJsonable.jsonPropertiesFieldName, virtualhostProperties);
        virtualhost = new Virtualhost(virtualhostJson, vertx);

        for (int x=0; x<numBackends; x++) {
            virtualhost.addBackend(String.format("0:%s", x), true);
        }
    }

    @Test
    public void checkUniformDistribution() {
        long sum = 0;
        double percentMarginOfError = 0.01;
        long samples = 100000L;

        long initialTime = System.currentTimeMillis();
        for (int x=0; x<samples; x++) {
            RequestData requestData = new RequestData("127.0.0.1", null);
            sum += virtualhost.getChoice(requestData).getPort();
        }
        long finishTime = System.currentTimeMillis();

        double result = (numBackends*(numBackends-1)/2.0) * (1.0*samples/numBackends);

        System.out.println(String.format("TestRandomPolicy.checkUniformDistribution: %d samples. Total time (ms): %d. NonUniformDistRatio%%: %.10f",
                    samples, finishTime-initialTime, Math.abs(100.0*(result-sum)/result)));

        double topLimit = sum*(1.0+percentMarginOfError);
        double bottomLimit = sum*(1.0-percentMarginOfError);

        assertThat(result).isGreaterThanOrEqualTo(bottomLimit)
                          .isLessThanOrEqualTo(topLimit);
    }

}
