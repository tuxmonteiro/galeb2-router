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
package com.globo.galeb.core;

import java.util.List;
import java.util.Map;

import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class SafeJsonObject extends JsonObject {

    private static final long serialVersionUID = 528808294183451684L;

    private JsonArray array = new JsonArray();

    public SafeJsonObject() {
        super();
    }

    public SafeJsonObject(String jsonStr) {
        this();
        mergeIn(new JsonObject(safeExtractJson(jsonStr)));
    }

    public SafeJsonObject(Map<String, Object> map) {
        super(map);
    }

    public SafeJsonObject(JsonObject json) {
        super(json.encode());
    }

    @Override
    public SafeJsonObject getObject(String fieldName) {
        return new SafeJsonObject(super.getObject(fieldName));
    }

    @Override
    public SafeJsonObject getObject(String fieldName, JsonObject def) {
        return new SafeJsonObject(super.getObject(fieldName, def));
    }

    public boolean isJson(String jsonStr) {
        try {
            new SafeJsonObject(jsonStr);
        } catch (DecodeException ignore) {
            return false;
        }
        return true;
    }

    private String safeExtractJson(String jsonStr) {
        try {
            new JsonObject(jsonStr);
            return jsonStr;
        } catch (DecodeException ignore) {
            return "{}";
        }
    }

    public SafeJsonObject putJsonArray(String fieldName, List<Object> list) {
        return new SafeJsonObject(super.putArray(fieldName, new JsonArray(list)));
    }

    public SafeJsonObject getJsonArray(String key) {
        this.array = super.getArray(key);
        return this;
    }

    public SafeJsonObject makeArray(JsonArray array) {
        this.array = new JsonArray().addArray(array);
        return this;
    }

    @SuppressWarnings("unchecked")
    public List<Object> toList() {
        return array.toList();
    }
}
