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
package com.globo.galeb.test.integration;
import io.netty.handler.codec.http.HttpResponseStatus;

import com.globo.galeb.core.HttpCode;
import com.globo.galeb.core.entity.IJsonable;
import com.globo.galeb.test.integration.util.Action;
import com.globo.galeb.test.integration.util.UtilTestVerticle;

import org.junit.Ignore;
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
        newGet().onPort(9000).atUri("/unknownuri").expectCode(HttpCode.NOT_FOUND).expectBodyJson("{\"status_message\": \"Not Found\"}").run();
    }

    @Test
    public void testWhenEmptyGetVHost() {
        // Test GET /virtualhost
        // Expected: { "status_message" : "Not Found" }
        newGet().onPort(9000).atUri("/virtualhost").expectCode(HttpCode.NOT_FOUND).expectBodyJson("{\"status_message\": \"Not Found\"}").run();
    }

    @Test
    public void testWhenEmptyGetVHostId() {
        // Test GET /virtualhost/id
        // Expected: { "status_message" : "Not Found" }
        newGet().onPort(9000).atUri("/virtualhost/1234").expectCode(HttpCode.NOT_FOUND).expectBodyJson("{\"status_message\": \"Not Found\"}").run();
    }

    @Ignore // TODO: Ignore "properties"
    @Test
    public void testWhenEmptyGetRoute() {
        // Test GET /route
        // Expected: { "version" : 0, "routes" : [ ] }
        JsonObject expectedJson = new JsonObject()
            .putString("id", "")
            .putObject("properties", new JsonObject())
            .putNumber("version", 0)
            .putArray("virtualhosts", new JsonArray());
        newGet().onPort(9000).atUri("/farm").expectBodyJson(expectedJson).run();;
    }

    @Test
    public void testWhenEmptyGetVersion() {
        // Test GET /version
        // Expected: { "version" : 0 }
        newGet().onPort(9000).atUri("/version").expectBodyJson("{ \"version\" : 0 }").run();
    }

    // Test POST /virtualhost
    @Test
    public void testWhenEmptyPostVHost() {
        int expectedStatusCode = HttpCode.OK;
        String expectedStatusMessage = HttpResponseStatus.valueOf(expectedStatusCode).reasonPhrase();
        String vhostId = "test.localdomain";
        JsonObject vhostJson = new JsonObject()
                                    .putNumber("version", 1L)
                                    .putString(IJsonable.ID_FIELDNAME, vhostId);

        JsonObject expectedJson = new JsonObject().putString("status_message", expectedStatusMessage);


        Action action1 = newPost().onPort(9000).setBodyJson(vhostJson).atUri("/virtualhost").expectBodyJson(expectedJson);

        JsonObject getExpectedJson = new JsonObject()
            .putString(IJsonable.ID_FIELDNAME, "test.localdomain")
//            .putString(IJsonable.STATUS_FIELDNAME, IJsonable.UNDEF)
            .putObject(IJsonable.PROPERTIES_FIELDNAME,
                    new JsonObject());

        newGet().onPort(9000).atUri("/virtualhost/"+vhostId).expectBodyJson(getExpectedJson).after(action1);

        action1.run();

    }

}
