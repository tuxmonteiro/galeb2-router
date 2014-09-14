package com.globo.galeb.test.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LogDelegate;

import com.globo.galeb.core.MessageBus;
import com.globo.galeb.core.Serializable;
import com.globo.galeb.test.unit.util.FakeLogger;

public class MessageBusTest {

    private LogDelegate logDelegate;
    private Logger logger;
    private String virtualhostStr;
    private String backendJson;

    @Before
    public void setUp() throws Exception {
        logDelegate = mock(LogDelegate.class);
        logger = new FakeLogger(logDelegate);
        ((FakeLogger)logger).setQuiet(false);
        ((FakeLogger)logger).setTestId("");

        virtualhostStr = new JsonObject().putString(Serializable.jsonIdFieldName, "test.virtualhost.com").encode();
        backendJson = new JsonObject().putString(Serializable.jsonIdFieldName, "0.0.0.0:00").encode();
    }

    @Test
    public void checkMessage() {
        ((FakeLogger)logger).setTestId("validateBuildMessage");
        String uriStr = "/test";
        JsonObject properties = new JsonObject();

        String message = new MessageBus()
                                .setVirtualhost(virtualhostStr)
                                .setUri(uriStr)
                                .setBackend(backendJson)
                                .make()
                                .toString();

        JsonObject messageJsonOrig = new JsonObject(message);
        JsonObject messageJson = new JsonObject();
        JsonObject virtualhostObj = new JsonObject(virtualhostStr);

        virtualhostObj.putObject(Serializable.jsonPropertiesFieldName, properties);
        messageJson.putString(MessageBus.virtualhostFieldName, virtualhostObj.encode());
        messageJson.putString(MessageBus.backendFieldName, backendJson);
        messageJson.putString(MessageBus.uriFieldName, uriStr);

        assertThat(messageJsonOrig).isEqualTo(messageJson);
    }

}
