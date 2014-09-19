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
        messageBus.setUri("/virtualhost")
                  .setEntity(new JsonObject().putString(Entity.jsonIdFieldName, "test.localdomain").encode());

        virtualhostMap = new VirtualhostMap();
        virtualhostMap.setMessageBus(messageBus).setLogger(logger);
    }

    @Test
    public void AddReturnTrue() {
        assertTrue(virtualhostMap.add());
    }

    @Test
    public void DelReturnFalseIfNotExist() {
        assertFalse(virtualhostMap.del());
    }

    @Test
    public void DelReturnTrueIfExist() {
        virtualhostMap.add();
        assertTrue(virtualhostMap.del());
    }

    @Test
    public void ResetReturnFalse() {
        assertFalse(virtualhostMap.reset());
    }

    @Test
    public void ChangeReturnFalse() {
        assertFalse(virtualhostMap.change());
    }

}
