package com.globo.galeb.test.unit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.impl.LogDelegate;

import com.globo.galeb.core.Entity;
import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.core.bus.MessageBus;
import com.globo.galeb.core.bus.BackendMap;
import com.globo.galeb.core.bus.VirtualhostMap;
import com.globo.galeb.test.unit.util.FakeLogger;

public class BackendMapTest {

    private BackendMap backendMap;
    private MessageBus messageBus;
    private MessageBus messageBusParent;
    private VirtualhostMap virtualhostMap;

    @Before
    public void setUp() throws Exception {
        LogDelegate logDelegate = mock(LogDelegate.class);
        FakeLogger logger = new FakeLogger(logDelegate);
        logger.setQuiet(false);
        logger.setTestId("");

        Map<String, Virtualhost> map = new HashMap<>();

        messageBus = new MessageBus();
        messageBus.setUri("/backend")
                  .setParentId("test.localdomain")
                  .setEntity(new JsonObject().putString(Entity.jsonIdFieldName, "127.0.0.1:8080").encode());

        backendMap = new BackendMap();
        backendMap.setMessageBus(messageBus).setLogger(logger).setMap(map);

        messageBusParent = new MessageBus();
        messageBusParent.setUri("/virtualhost")
                  .setEntity(new JsonObject().putString(Entity.jsonIdFieldName, "test.localdomain").encode());

        virtualhostMap = new VirtualhostMap();
        virtualhostMap.setMessageBus(messageBusParent).setLogger(logger).setMap(map);
    }

    @Test
    public void AddReturnFalseIfParentIdNotExist() {
        assertFalse(backendMap.add());
    }

    @Test
    public void AddReturnTrueIfParentIdExist() {
        virtualhostMap.add();
        assertTrue(backendMap.add());
    }

    @Test
    public void DelReturnFalseIfNotExist() {
        assertFalse(backendMap.del());
    }

    @Test
    public void DelReturnTrueIfExist() {
        virtualhostMap.add();
        backendMap.add();
        assertTrue(backendMap.del());
    }

    @Test
    public void ResetReturnFalse() {
        assertFalse(backendMap.reset());
    }

    @Test
    public void ChangeReturnFalse() {
        assertFalse(backendMap.change());
    }

}
