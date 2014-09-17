package com.globo.galeb.test.integration;
import io.netty.handler.codec.http.HttpResponseStatus;

import com.globo.galeb.core.HttpCode;
import com.globo.galeb.core.IJsonable;
import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.loadbalance.impl.DefaultLoadBalancePolicy;
import com.globo.galeb.test.integration.util.Action;
import com.globo.galeb.test.integration.util.UtilTestVerticle;

import org.junit.Test;
//import org.vertx.java.core.Handler;
//import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class RouteManagerTest extends UtilTestVerticle {

    @Test
    public void testWhenEmptyGetUnknownURI() {
        // Test GET unknown URI
        // Expected: { "status_message" : "Not Found" }
        newGet().onPort(9090).atUri("/unknownuri").expectCode(HttpCode.NotFound).expectBodyJson("{\"status_message\": \"Not Found\"}").run();
    }

    @Test
    public void testWhenEmptyGetVHost() {
        // Test GET /virtualhost
        // Expected: { "status_message" : "Not Found" }
        newGet().onPort(9090).atUri("/virtualhost").expectCode(HttpCode.NotFound).expectBodyJson("{\"status_message\": \"Not Found\"}").run();
    }

    @Test
    public void testWhenEmptyGetVHostId() {
        // Test GET /virtualhost/id
        // Expected: { "status_message" : "Not Found" }
        newGet().onPort(9090).atUri("/virtualhost/1234").expectCode(HttpCode.NotFound).expectBodyJson("{\"status_message\": \"Not Found\"}").run();;
    }

    @Test
    public void testWhenEmptyGetRoute() {
        // Test GET /route
        // Expected: { "version" : 0, "routes" : [ ] }
        JsonObject expectedJson = new JsonObject()
            .putString("id", "")
            .putObject("properties", new JsonObject())
            .putNumber("version", 0)
            .putArray("virtualhosts", new JsonArray());
        newGet().onPort(9090).atUri("/farm").expectBodyJson(expectedJson).run();;
    }

    @Test
    public void testWhenEmptyGetVersion() {
        // Test GET /version
        // Expected: { "version" : 0 }
        newGet().onPort(9090).atUri("/version").expectBodyJson("{ \"version\" : 0 }").run();
    }

    // Test POST /virtualhost
    @Test
    public void testWhenEmptyPostVHost() {
        int expectedStatusCode = HttpCode.Ok;
        String expectedStatusMessage = HttpResponseStatus.valueOf(expectedStatusCode).reasonPhrase();
        String vhostId = "test.localdomain";
        JsonObject vhostJson = new JsonObject()
                                    .putNumber("version", 1L)
                                    .putString(IJsonable.jsonIdFieldName, vhostId);

        JsonObject expectedJson = new JsonObject().putString("status_message", expectedStatusMessage);


        Action action1 = newPost().onPort(9090).setBodyJson(vhostJson).atUri("/virtualhost").expectBodyJson(expectedJson);

        JsonObject getExpectedJson = new JsonObject()
            .putString(IJsonable.jsonIdFieldName, "test.localdomain")
            .putObject(IJsonable.jsonPropertiesFieldName,
                    new JsonObject().putString(Virtualhost.loadBalancePolicyFieldName, new DefaultLoadBalancePolicy().toString()))
            .putObject(Virtualhost.backendsFieldName, new JsonObject()
                    .putArray(Virtualhost.backendsElegibleFieldName, new JsonArray())
                    .putArray(Virtualhost.backendsFailedFieldName, new JsonArray()));

        newGet().onPort(9090).atUri("/virtualhost/test.localdomain").expectBodyJson(getExpectedJson).after(action1);

        action1.run();

    }

}
