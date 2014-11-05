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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.globo.galeb.test.unit.assertj.custom.VirtualHostAssert.*;

import com.globo.galeb.core.IJsonable;
import com.globo.galeb.core.RequestData;
import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.loadbalance.ILoadBalancePolicy;
import com.globo.galeb.loadbalance.impl.DefaultLoadBalancePolicy;
import com.globo.galeb.loadbalance.impl.RandomPolicy;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.impl.DefaultVertx;
import org.vertx.java.core.json.JsonObject;

public class VirtualhostTest {

    String virtualhostName;
    Virtualhost virtualhost;
    String backend;
    RequestData requestData;

    @Before
    public void setUp(){
        Vertx vertx = mock(DefaultVertx.class);
        HttpClient httpClient = mock(HttpClient.class);
        when(vertx.createHttpClient()).thenReturn(httpClient);

        virtualhostName = "virtualhost1";
        requestData = new RequestData();

        JsonObject virtualhostProperties = new JsonObject()
            .putString(Virtualhost.LOADBALANCE_POLICY_FIELDNAME, RandomPolicy.class.getSimpleName());
        JsonObject virtualhostJson = new JsonObject()
            .putString(IJsonable.ID_FIELDNAME, virtualhostName)
            .putObject(IJsonable.PROPERTIES_FIELDNAME, virtualhostProperties);
        virtualhost = new Virtualhost(virtualhostJson, vertx);

        backend = "0.0.0.0:0";
    }

    @Test
    public void insertNewBackendInSet() {
        boolean backendOk = true;

        boolean notFail = virtualhost.addBackend(backend, backendOk);

        assertThat(virtualhost).hasActionOk(notFail).hasSize(1, backendOk);
    }

    @Test
    public void insertNewBadBackendInSet() {
        boolean backendOk = false;

        boolean notExist = virtualhost.addBackend(backend, backendOk);

        assertThat(virtualhost).hasActionOk(notExist).hasSize(1, backendOk);
    }

    @Test
    public void insertDuplicatedBackendInSet() {
        boolean backendOk = true;

        virtualhost.addBackend(backend, backendOk);
        boolean notFail = virtualhost.addBackend(backend, backendOk);

        assertThat(virtualhost).hasActionFail(notFail).hasSize(1, backendOk);

    }

    @Test
    public void insertDuplicatedBadBackendInSet() {
        boolean backendOk = false;

        virtualhost.addBackend(backend, backendOk);
        boolean notFail = virtualhost.addBackend(backend, backendOk);

        assertThat(virtualhost).hasActionFail(notFail).hasSize(1, backendOk);

    }

    @Test
    public void removeExistingBackendInSet() {
        boolean backendOk = true;

        virtualhost.addBackend(backend, backendOk);
        boolean notFail = virtualhost.removeBackend(backend, backendOk);

        assertThat(virtualhost).hasActionOk(notFail).hasSize(0, backendOk);
    }

    @Test
    public void removeExistingBadBackendInSet() {
        boolean backendOk = false;

        virtualhost.addBackend(backend, backendOk);
        boolean notFail = virtualhost.removeBackend(backend, backendOk);

        assertThat(virtualhost).hasActionOk(notFail).hasSize(0, backendOk);
    }

    @Test
    public void removeAbsentBackendInSet() {
        boolean backendOk = true;

        boolean notFail = virtualhost.removeBackend(backend, backendOk);

        assertThat(virtualhost).hasActionFail(notFail).hasSize(0, backendOk);

    }

    @Test
    public void removeAbsentBadBackendInSet() {
        boolean backendOk = false;

        boolean notFail = virtualhost.removeBackend(backend, backendOk);

        assertThat(virtualhost).hasActionFail(notFail).hasSize(0, backendOk);

    }

    @Test
    public void loadBalancePolicyClassFound() {
        virtualhost.setLoadBalancePolicy(RandomPolicy.class.getSimpleName());

        ILoadBalancePolicy loadBalance = virtualhost.getLoadBalancePolicy();

        assertThat(loadBalance.isDefault()).isFalse();
    }

    @Test
    public void loadBalancePolicyClassNotFound() {
        String loadBalancePolicyStr = "ClassNotExist";
        virtualhost.setLoadBalancePolicy(loadBalancePolicyStr);

        ILoadBalancePolicy loadBalance = virtualhost.getLoadBalancePolicy();

        assertThat(loadBalance.isDefault()).isTrue();
    }

    @Test
    public void getBackendWithLoadBalancePolicy() {
        virtualhost.setLoadBalancePolicy(DefaultLoadBalancePolicy.class.getSimpleName());

        virtualhost.addBackend(backend, true);

        assertThat(virtualhost.getChoice(requestData).toString()).isEqualTo(backend);
    }

}
