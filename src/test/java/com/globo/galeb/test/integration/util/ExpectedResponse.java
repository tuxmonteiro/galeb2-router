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
package com.globo.galeb.test.integration.util;

import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.rulereturn.HttpCode;

public class ExpectedResponse {
    private int code = HttpCode.OK;
    private String body;
    private JsonObject bodyJson;
    private int bodySize = -1;


    public int code() {
        return code;
    }
    public String body() {
        return body;
    }
    public JsonObject bodyJson() {
        return bodyJson;
    }
    public int bodySize() {
        return bodySize;
    }

    public ExpectedResponse setCode(int code) {
        this.code = code;
        return this;
    }
    public ExpectedResponse setBody(String body) {
        this.body = body;
        return this;
    }
    public ExpectedResponse setBodyJson(JsonObject body) {
        this.bodyJson = body;
        return this;
    }
    public ExpectedResponse setBodyJson(String body) {
        this.bodyJson = Util.safeExtractJson(body);;
        return this;
    }
    public ExpectedResponse setBodySize(int bytes) {
        this.bodySize = bytes;
        return this;
    }

}
