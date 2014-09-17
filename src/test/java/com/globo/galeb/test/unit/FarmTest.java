package com.globo.galeb.test.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LogDelegate;
import org.vertx.java.platform.Container;
import org.vertx.java.platform.Verticle;

import com.globo.galeb.core.Farm;
import com.globo.galeb.core.IJsonable;
import com.globo.galeb.core.MessageBus;
import com.globo.galeb.core.QueueMap;
import com.globo.galeb.core.SafeJsonObject;
import com.globo.galeb.test.unit.util.FakeLogger;

public class FarmTest {

    private Farm farm;
    private QueueMap queueMap;

    private Verticle verticle;
    private Vertx vertx;
    private Container container;
    private Logger logger;
    private LogDelegate logDelegate;

    @Before
    public void setUp() throws Exception {

        verticle = mock(Verticle.class);
        vertx = mock(Vertx.class);
        container = mock(Container.class);
        logDelegate = mock(LogDelegate.class);
        logger = new FakeLogger(logDelegate);
        ((FakeLogger)logger).setQuiet(false);
        ((FakeLogger)logger).setTestId("");

        when(verticle.getVertx()).thenReturn(vertx);
        when(verticle.getVertx().eventBus()).thenReturn(null);
        when(verticle.getContainer()).thenReturn(container);
        when(verticle.getContainer().logger()).thenReturn(logger);
        when(verticle.getContainer().config()).thenReturn(new SafeJsonObject());

        farm = new Farm(verticle);
        queueMap = farm.getQueueMap();

    }

    @Test
    public void insert10NewVirtualhost() {

        for (int x=0; x<10;x++) {
            SafeJsonObject virtualhostJson =
                    new SafeJsonObject().putString(IJsonable.jsonIdFieldName, String.valueOf(x));
            String message = new MessageBus()
                                    .setEntity(virtualhostJson)
                                    .setUri("/virtualhost")
                                    .make()
                                    .toString();

            queueMap.processAddMessage(message);
        }

        assertThat(farm.getVirtualhosts()).hasSize(10);
    }

    @Test
    public void insert10NewBackends() {

        String message = "";
        SafeJsonObject virtualhostJson =
                new SafeJsonObject().putString(IJsonable.jsonIdFieldName, "test.localdomain");
        message = new MessageBus().setEntity(virtualhostJson)
                                  .setUri("/virtualhost")
                                  .make()
                                  .toString();

        queueMap.processAddMessage(message);

        for (int x=0; x<10;x++) {
            SafeJsonObject backendJson =
                    new SafeJsonObject().putString(IJsonable.jsonIdFieldName, String.format("0:%d", x));
            message = new MessageBus().setEntity(backendJson)
                                      .setParentId("test.localdomain")
                                      .setUri("/backend")
                                      .make()
                                      .toString();

            queueMap.processAddMessage(message);
        }

        assertThat(farm.getBackends()).hasSize(10);
    }

    @Test
    public void isVersionEqual10() {

    }
}
