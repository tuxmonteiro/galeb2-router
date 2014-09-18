package com.globo.galeb.test.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;

import com.globo.galeb.core.Farm;
import com.globo.galeb.core.SafeJsonObject;
import com.globo.galeb.core.bus.BackendMap;
import com.globo.galeb.core.bus.MessageBus;
import com.globo.galeb.core.bus.MessageToMapBuilder;
import com.globo.galeb.core.bus.NullMap;
import com.globo.galeb.core.bus.VirtualhostMap;

public class MessageToMapBuilderTest {

    private Farm farm;
    private String message;

    @Before
    public void setUp() throws Exception {
        farm=mock(Farm.class);
    }

    @Test
    public void instanceIsVirtualhostMap() {
        message = new SafeJsonObject()
                        .putString(MessageBus.uriFieldName, "/virtualhost")
                        .encode();
        assertThat(MessageToMapBuilder.getInstance(message, farm).getClass())
            .as("instanceIsVirtualhostMap").isEqualTo(VirtualhostMap.class);
    }

    @Test
    public void instanceIsBackendMap() {
        message = new SafeJsonObject()
                        .putString(MessageBus.uriFieldName, "/backend")
                        .encode();
        assertThat(MessageToMapBuilder.getInstance(message, farm).getClass())
            .as("instanceIsBackendMap").isEqualTo(BackendMap.class);
    }

    @Test
    public void messageWithoutUri() {
        message = new SafeJsonObject().encode();
        assertThat(MessageToMapBuilder.getInstance(message, farm).getClass())
            .as("messageWithoutUri").isEqualTo(NullMap.class);

    }

    @Test
    public void messageWithUriInvalid() {
        message = new SafeJsonObject()
            .putString(MessageBus.uriFieldName, "/invalid")
            .encode();
        assertThat(MessageToMapBuilder.getInstance(message, farm).getClass())
            .as("messageWithUriInvalid").isEqualTo(NullMap.class);
    }

    @Test
    public void farmIsNull() {
        message = new SafeJsonObject()
            .putString(MessageBus.uriFieldName, "/virtualhost")
            .encode();
        assertThat(MessageToMapBuilder.getInstance(message, null).getClass())
        .as("farmIsNull").isEqualTo(NullMap.class);

    }
}
