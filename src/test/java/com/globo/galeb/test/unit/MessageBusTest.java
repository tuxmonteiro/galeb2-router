package com.globo.galeb.test.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LogDelegate;

import com.globo.galeb.core.IJsonable;
import com.globo.galeb.core.MessageBus;
import com.globo.galeb.test.unit.util.FakeLogger;

public class MessageBusTest {

    private LogDelegate logDelegate;
    private Logger logger;
    private String virtualhostStr;
    private String virtualhostId;
    private String backendStr;

    @Before
    public void setUp() throws Exception {
        logDelegate = mock(LogDelegate.class);
        logger = new FakeLogger(logDelegate);
        ((FakeLogger)logger).setQuiet(false);
        ((FakeLogger)logger).setTestId("");

        virtualhostId = "test.virtualhost.com";
        virtualhostStr = new JsonObject().putString(IJsonable.jsonIdFieldName, virtualhostId).encode();
        backendStr = new JsonObject().putString(IJsonable.jsonIdFieldName, "0.0.0.0:00").encode();
    }

    @Test
    public void checkMessage() {
        ((FakeLogger)logger).setTestId("validateBuildMessage");
        String uriStr = "/test";
        JsonObject properties = new JsonObject();
        JsonObject virtualhostJson = new JsonObject(virtualhostStr);
        JsonObject backendJson = new JsonObject(backendStr);

        virtualhostJson.putObject(IJsonable.jsonPropertiesFieldName, properties);
        backendJson.putObject(IJsonable.jsonPropertiesFieldName, properties);

        String virtualhostId = virtualhostJson.getString(IJsonable.jsonIdFieldName);


        String messageWithParentId = new MessageBus()
                                            .setParentId(virtualhostId)
                                            .setUri(uriStr)
                                            .setEntity(backendStr)
                                            .make()
                                            .toString();

        JsonObject messageJsonOrig = new JsonObject(messageWithParentId);
        JsonObject messageJson = new JsonObject();

        messageJson.putString(MessageBus.uriFieldName, uriStr);
        messageJson.putString(MessageBus.parentIdFieldName, virtualhostId);
        messageJson.putString(MessageBus.entityFieldName, backendStr);

        assertThat(messageJsonOrig.getString(MessageBus.uriFieldName)).isEqualTo(uriStr);
        assertThat(messageJsonOrig.getString(MessageBus.parentIdFieldName)).isEqualTo(virtualhostId);
        assertThat(messageJsonOrig.getString(MessageBus.entityFieldName)).isEqualTo(backendJson.encode());

    }

}
