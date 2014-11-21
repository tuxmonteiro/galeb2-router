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

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.bus.BackendMap;
import com.globo.galeb.bus.MessageBus;
import com.globo.galeb.bus.MessageToMapBuilder;
import com.globo.galeb.bus.NullMap;
import com.globo.galeb.bus.VirtualhostMap;
import com.globo.galeb.entity.impl.Farm;
import com.globo.galeb.entity.impl.backend.BackendPools;
import com.globo.galeb.entity.impl.frontend.Virtualhost;

public class MessageToMapBuilderTest {

    private Farm farm;
    private String message;

    @Before
    public void setUp() throws Exception {
        farm=mock(Farm.class);
        when(farm.getEntities()).thenReturn(new HashMap<String, Virtualhost>());
        when(farm.getBackendPools()).thenReturn(new BackendPools("backendpools"));
    }

    @Test
    public void instanceIsVirtualhostMap() {
        message = new JsonObject()
                        .putString(MessageBus.URI_FIELDNAME, "/virtualhost")
                        .encode();
        assertThat(MessageToMapBuilder.getInstance(message, farm).getClass())
            .as("instanceIsVirtualhostMap").isEqualTo(VirtualhostMap.class);
    }

    @Test
    public void instanceIsBackendMap() {
        message = new JsonObject()
                        .putString(MessageBus.URI_FIELDNAME, "/backend")
                        .encode();
        assertThat(MessageToMapBuilder.getInstance(message, farm).getClass())
            .as("instanceIsBackendMap").isEqualTo(BackendMap.class);
    }

    @Test
    public void messageWithoutUri() {
        message = new JsonObject().encode();
        assertThat(MessageToMapBuilder.getInstance(message, farm).getClass())
            .as("messageWithoutUri").isEqualTo(NullMap.class);

    }

    @Test
    public void messageWithUriInvalid() {
        message = new JsonObject()
            .putString(MessageBus.URI_FIELDNAME, "/invalid")
            .encode();
        assertThat(MessageToMapBuilder.getInstance(message, farm).getClass())
            .as("messageWithUriInvalid").isEqualTo(NullMap.class);
    }

    @Test
    public void farmIsNull() {
        message = new JsonObject()
            .putString(MessageBus.URI_FIELDNAME, "/virtualhost")
            .encode();
        assertThat(MessageToMapBuilder.getInstance(message, null).getClass())
        .as("farmIsNull").isEqualTo(NullMap.class);

    }
}
