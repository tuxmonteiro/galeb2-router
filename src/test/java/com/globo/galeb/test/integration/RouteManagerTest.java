package com.globo.galeb.test.integration;
import io.netty.handler.codec.http.HttpResponseStatus;

import com.globo.galeb.core.HttpCode;
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
        // Expected: { "status_message" : "Bad Request" }
        newGet().onPort(9090).atUri("/unknownuri").expectCode(HttpCode.BadRequest).expectBodyJson("{\"status_message\": \"Bad Request\"}").run();
    }

    @Test
    public void testWhenEmptyGetVHost() {
        // Test GET /virtualhost
        // Expected: { "version" : 0, "routes" : [ ] }
        JsonObject expectedJson = new JsonObject().putNumber("version", 0).putArray("routes", new JsonArray());
        newGet().onPort(9090).atUri("/virtualhost").expectBodyJson(expectedJson).run();
    }

    @Test
    public void testWhenEmptyGetVHostId() {
        // Test GET /virtualhost/id
        // Expected: { }
        newGet().onPort(9090).atUri("/virtualhost/1234").expectBodyJson(new JsonObject()).run();;
    }

    @Test
    public void testWhenEmptyGetRoute() {
        // Test GET /route
        // Expected: { "version" : 0, "routes" : [ ] }
        JsonObject expectedJson = new JsonObject().putNumber("version", 0).putArray("routes", new JsonArray());
        newGet().onPort(9090).atUri("/route").expectBodyJson(expectedJson).run();;
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
        JsonObject vhostJson = new JsonObject().putString("id", "test.localdomain");
        JsonObject expectedJson = new JsonObject().putString("status_message", expectedStatusMessage);
        JsonArray routesJson = new JsonArray().add(vhostJson);
        JsonObject postJson = new JsonObject().putNumber("version", 1L).putArray("routes", routesJson);

        Action action1 = newPost().onPort(9090).setBodyJson(postJson).atUri("/virtualhost").expectBodyJson(expectedJson);

        JsonObject getExpectedJson = new JsonObject()
            .putString("id", "test.localdomain")
            .putObject("properties", new JsonObject())
            .putArray("backends", new JsonArray())
            .putArray("badBackends", new JsonArray());

        newGet().onPort(9090).atUri("/virtualhost/test.localdomain").expectBodyJson(getExpectedJson).after(action1);

//        action2.setDontStop(true);
//    	getVertx().eventBus().registerHandler("ended.action", new Handler<Message<String>>() {
//    		@Override
//    		public void handle(Message<String> message) {
//    			if (message.body().equals(action2.id())) {
//    				System.out.println("Testing other things after action2");
//    				testCompleteWrapper();
//    			}
//    		};
//		});

        action1.run();

    }

}
