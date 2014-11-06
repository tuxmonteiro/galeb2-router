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

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LogDelegate;

import com.globo.galeb.core.bus.MessageBus;
import com.globo.galeb.core.entity.IJsonable;
import com.globo.galeb.test.unit.util.FakeLogger;

public class MessageBusTest {

    private LogDelegate logDelegate;
    private Logger logger;
    private String virtualhostStr;
    private String backendStr;

    @Before
    public void setUp() throws Exception {
        logDelegate = mock(LogDelegate.class);
        logger = new FakeLogger(logDelegate);
        ((FakeLogger)logger).setQuiet(false);
        ((FakeLogger)logger).setTestId("");

        virtualhostStr = new JsonObject().putString(IJsonable.ID_FIELDNAME, "test.virtualhost.com").encode();
        backendStr = new JsonObject().putString(IJsonable.ID_FIELDNAME, "0.0.0.0:00").encode();
    }

    @Test
    public void checkMessage() {
        ((FakeLogger)logger).setTestId("validateBuildMessage");
        String uriStr = "/test";
        JsonObject virtualhostJson = new JsonObject(virtualhostStr);
        JsonObject backendJson = new JsonObject(backendStr);

        String virtualhostId = virtualhostJson.getString(IJsonable.ID_FIELDNAME);


        String messageWithParentId = new MessageBus()
                                            .setParentId(virtualhostId)
                                            .setUri(uriStr)
                                            .setEntity(backendStr)
                                            .make()
                                            .toString();

        JsonObject messageJsonOrig = new JsonObject(messageWithParentId);
        JsonObject messageJson = new JsonObject();

        messageJson.putString(MessageBus.URI_FIELDNAME, uriStr);
        messageJson.putString(MessageBus.PARENT_ID_FIELDNAME, virtualhostId);
        messageJson.putString(MessageBus.ENTITY_FIELDNAME, backendStr);

        assertThat(messageJsonOrig.getString(MessageBus.URI_FIELDNAME)).isEqualTo(uriStr);
        assertThat(messageJsonOrig.getString(MessageBus.PARENT_ID_FIELDNAME)).isEqualTo(virtualhostId);
        assertThat(messageJsonOrig.getString(MessageBus.ENTITY_FIELDNAME)).isEqualTo(backendJson.encode());

    }

}
