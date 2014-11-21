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

import com.globo.galeb.criteria.LoadBalanceCriterionFactory;
import com.globo.galeb.criteria.impl.RoundRobinCriterion;
import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.entity.impl.backend.Backend;
import com.globo.galeb.entity.impl.backend.BackendPool;
import com.globo.galeb.request.RequestData;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.impl.DefaultVertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.shareddata.SharedData;

public class RoundRobinCriterionTest {

    private BackendPool backendPool;
    private int numBackends = 10;
    private Vertx vertx;
    private RequestData requestData;
    private final String httpHeaderHost = HttpHeaders.HOST.toString();

    @Before
    public void setUp() throws Exception {
        vertx = mock(DefaultVertx.class);
        HttpClient httpClient = mock(HttpClient.class);
        when(vertx.createHttpClient()).thenReturn(httpClient);

        SharedData sharedData = new SharedData();
        when(vertx.sharedData()).thenReturn(sharedData);

        JsonObject backendPoolProperties = new JsonObject()
            .putString(BackendPool.LOADBALANCE_POLICY_FIELDNAME,
                    RoundRobinCriterion.class.getSimpleName().replaceAll(LoadBalanceCriterionFactory.CLASS_SUFFIX, ""));
        JsonObject backendPoolJson = new JsonObject()
            .putString(IJsonable.ID_FIELDNAME, "test.localdomain")
            .putObject(IJsonable.PROPERTIES_FIELDNAME, backendPoolProperties);
        backendPool = (BackendPool) new BackendPool(backendPoolJson).setPlataform(vertx);

        for (int x=0; x<numBackends; x++) {
            backendPool.addEntity(new Backend(new JsonObject().putString(IJsonable.ID_FIELDNAME, String.format("0:%s", x))));
        }

        MultiMap headers = new CaseInsensitiveMultiMap();
        headers.add(httpHeaderHost, "test.localdomain");

        requestData = new RequestData().setHeaders(headers);
    }

    @Ignore
    @Test
    public void backendsChosenInSequence() {
        int lastBackendChosenPort = numBackends-1;
        for (int counter=0; counter<100000; counter++) {
             int backendChosenPort = backendPool.getChoice(requestData).getPort();
             if (backendChosenPort==0) {
                 assertThat(lastBackendChosenPort).isEqualTo(numBackends-1);
             } else {
                 assertThat(backendChosenPort).isEqualTo(lastBackendChosenPort+1);
             }
             lastBackendChosenPort = backendChosenPort;
        }
    }

}
