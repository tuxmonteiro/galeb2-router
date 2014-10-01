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
import org.vertx.java.core.json.impl.Json;

public class SafeJsonObject extends JsonObject {

    private static final long serialVersionUID = 528808294183451684L;

    private JsonArray array = new JsonArray();

    public SafeJsonObject() {
        super();
    }

    public SafeJsonObject(String jsonStr) {
        this();
        if (isJson(jsonStr)) mergeIn(new JsonObject(jsonStr));
    }

    public SafeJsonObject(Map<String, Object> map) {
        super(map);
    }

    public SafeJsonObject(JsonObject json) {
        super(json.encode());
    }

    public SafeJsonObject(SafeJsonObject json) {
        super(json.encode());
    }

    @Override
    @SuppressWarnings("unchecked")
    public SafeJsonObject getObject(String fieldName) {
        Map<String, Object> m = (Map<String, Object>) map.get(fieldName);
        return m == null ? null : new SafeJsonObject(m);
    }

    @Override
    public SafeJsonObject putString(String fieldName, String value) {
        map.put(fieldName, value);
        return this;
    }

    public boolean isJson(String jsonStr) {
        try {
            Json.decodeValue(jsonStr, Map.class);
        } catch (DecodeException e) {
            return false;
        }
        return true;
    }

    public SafeJsonObject getJsonArray(String key) {
        this.array = super.getArray(key);
        return this;
    }

    public SafeJsonObject makeArray(JsonArray array) {
        this.array = new JsonArray().addArray(array);
        return this;
    }

    @Override
    public SafeJsonObject clone() {
        return new SafeJsonObject(this.encode());
    }

    @SuppressWarnings("unchecked")
    public List<Object> toList() {
        return array.toList();
    }
}
