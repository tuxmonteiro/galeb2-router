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

import java.util.LinkedList;

import com.globo.galeb.criteria.LoadBalanceCriterionFactory;
import com.globo.galeb.criteria.impl.LoadBalanceCriterion;
import com.globo.galeb.criteria.impl.RoundRobinCriterion;
import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.entity.impl.backend.Backend;
import com.globo.galeb.entity.impl.backend.BackendPool;
import com.globo.galeb.entity.impl.backend.IBackend;
import com.globo.galeb.entity.impl.backend.NullBackend;
import com.globo.galeb.request.RequestData;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.impl.DefaultVertx;
import org.vertx.java.core.json.JsonObject;

public class RoundRobinCriterionTest {

    private BackendPool backendPool;
    private int numBackends = 100;
    private Vertx vertx;
    private RequestData requestData;
    private final String httpHeaderHost = HttpHeaders.HOST.toString();

    @Before
    public void setUp() throws Exception {
        vertx = mock(DefaultVertx.class);
        HttpClient httpClient = mock(HttpClient.class);
        when(vertx.createHttpClient()).thenReturn(httpClient);

        JsonObject backendPoolProperties = new JsonObject()
            .putString(LoadBalanceCriterion.LOADBALANCE_POLICY_FIELDNAME,
                    RoundRobinCriterion.class.getSimpleName().replaceAll(LoadBalanceCriterionFactory.CLASS_SUFFIX, ""));

        JsonObject backendPoolJson = new JsonObject()
                                            .putString(IJsonable.ID_FIELDNAME, "pool0")
                                            .putObject(IJsonable.PROPERTIES_FIELDNAME, backendPoolProperties);

        backendPool = (BackendPool) new BackendPool(backendPoolJson).setPlataform(vertx);

        for (int x=0; x<numBackends; x++) {
            backendPool.addEntity(new Backend(new JsonObject().putString(IJsonable.ID_FIELDNAME, String.format("0:%s", x))));
        }

        MultiMap headers = new CaseInsensitiveMultiMap();
        headers.add(httpHeaderHost, "test.localdomain");

        requestData = new RequestData().setHeaders(headers);
    }

    @Test
    public void backendsChosenInSequence() {

        LinkedList<IBackend> controlList = new LinkedList<>();
        for (int counter=0; counter<numBackends*99; counter++) {
            controlList.add(backendPool.getChoice(requestData));
        }

        backendPool.resetLoadBalance();
        IBackend lastBackend = new NullBackend();
        IBackend currentBackend = new NullBackend();

        for (int counter=0; counter<numBackends*99; counter++) {
            currentBackend = controlList.poll();
            assertThat(currentBackend).isNotEqualTo(lastBackend);
            assertThat(backendPool.getChoice(requestData)).isEqualTo(currentBackend);
            lastBackend = currentBackend;
        }
    }

}
