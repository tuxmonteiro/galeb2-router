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
import static org.mockito.Mockito.when;

import com.globo.galeb.core.entity.IJsonable;
import com.globo.galeb.core.entity.impl.backend.Backend;
import com.globo.galeb.core.entity.impl.backend.BackendPool;
import com.globo.galeb.core.request.RequestData;
import com.globo.galeb.criteria.LoadBalanceCriterionFactory;
import com.globo.galeb.criteria.impl.RandomCriterion;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.impl.DefaultVertx;
import org.vertx.java.core.json.JsonObject;

public class RandomCriterionTest {

    BackendPool backendPool;
    int numBackends = 10;

    @Before
    public void setUp() throws Exception {

        Vertx vertx = mock(DefaultVertx.class);
        HttpClient httpClient = mock(HttpClient.class);
        when(vertx.createHttpClient()).thenReturn(httpClient);

        JsonObject backendPoolProperties = new JsonObject()
            .putString(BackendPool.LOADBALANCE_POLICY_FIELDNAME,
                    RandomCriterion.class.getSimpleName().replaceAll(LoadBalanceCriterionFactory.CLASS_SUFFIX, ""));
        JsonObject backendPoolJson = new JsonObject()
            .putString(IJsonable.ID_FIELDNAME, "test.localdomain")
            .putObject(IJsonable.PROPERTIES_FIELDNAME, backendPoolProperties);
        backendPool = (BackendPool) new BackendPool(backendPoolJson).setPlataform(vertx);

        for (int x=0; x<numBackends; x++) {
            backendPool.addEntity(new Backend(new JsonObject().putString(IJsonable.ID_FIELDNAME, String.format("0:%s", x))));
        }
    }

    @Test
    public void checkUniformDistribution() {
        long sum = 0;
        double percentMarginOfError = 0.01;
        long samples = 100000L;

        long initialTime = System.currentTimeMillis();
        for (int x=0; x<samples; x++) {
            RequestData requestData = new RequestData().setRemoteAddress("127.0.0.1");
            sum += backendPool.getChoice(requestData).getPort();
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
