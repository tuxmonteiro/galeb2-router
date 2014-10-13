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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.impl.LogDelegate;

import com.globo.galeb.core.Entity;
import com.globo.galeb.core.bus.MessageBus;
import com.globo.galeb.core.bus.VirtualhostMap;
import com.globo.galeb.test.unit.util.FakeLogger;

public class VirtualhostMapTest {

    private VirtualhostMap virtualhostMap;
    private MessageBus messageBus;

    @Before
    public void setUp() throws Exception {
        LogDelegate logDelegate = mock(LogDelegate.class);
        FakeLogger logger = new FakeLogger(logDelegate);
        logger.setQuiet(false);
        logger.setTestId("");

        messageBus = new MessageBus();
        messageBus.setUri("/virtualhost/test.localdomain")
                  .setEntity(new JsonObject().putString(Entity.ID_FIELDNAME, "test.localdomain").encode());

        virtualhostMap = new VirtualhostMap();
        virtualhostMap.setMessageBus(messageBus).setLogger(logger);
    }

    @Test
    public void addReturnTrue() {
        assertTrue(virtualhostMap.add());
    }

    @Test
    public void delReturnFalseIfNotExist() {
        assertFalse(virtualhostMap.del());
    }

    @Test
    public void delReturnTrueIfExist() {
        virtualhostMap.add();
        assertTrue(virtualhostMap.del());
    }

    @Test
    public void resetReturnFalse() {
        assertFalse(virtualhostMap.reset());
    }

    @Test
    public void changeReturnFalse() {
        assertFalse(virtualhostMap.change());
    }

}
