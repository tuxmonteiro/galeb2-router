/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.globo.galeb.core;

import java.util.List;
import java.util.Map;

import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Json;

/**
 * Class SafeJsonObject.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class SafeJsonObject extends JsonObject implements Cloneable {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 528808294183451684L;

    /** The jsonArray. */
    private JsonArray array = new JsonArray();

    /**
     * Instantiates a new safe json object.
     */
    public SafeJsonObject() {
        super();
    }

    /**
     * Instantiates a new safe json object.
     *
     * @param jsonStr the json str
     */
    public SafeJsonObject(String jsonStr) {
        this();
        if (isJson(jsonStr)) mergeIn(new JsonObject(jsonStr));
    }

    /**
     * Instantiates a new safeJson object.
     *
     * @param map the map
     */
    public SafeJsonObject(Map<String, Object> map) {
        super(map);
    }

    /**
     * Instantiates a new safeJson object.
     *
     * @param json the json
     */
    public SafeJsonObject(JsonObject json) {
        this(json.encode());
    }

    /**
     * Instantiates a new safeJson object.
     *
     * @param json the json
     */
    public SafeJsonObject(SafeJsonObject json) {
        this(json.encode());
    }

    /* (non-Javadoc)
     * @see org.vertx.java.core.json.JsonObject#getObject(java.lang.String)
     */
    @Override
    @SuppressWarnings("unchecked")
    public SafeJsonObject getObject(String fieldName) {
        Map<String, Object> m = (Map<String, Object>) map.get(fieldName);
        return m == null ? null : new SafeJsonObject(m);
    }

    /* (non-Javadoc)
     * @see org.vertx.java.core.json.JsonObject#putString(java.lang.String, java.lang.String)
     */
    @Override
    public SafeJsonObject putString(String fieldName, String value) {
        map.put(fieldName, value);
        return this;
    }

    /**
     * Checks if is json.
     *
     * @param jsonStr the json str
     * @return true, if is json
     */
    public boolean isJson(String jsonStr) {
        try {
            Json.decodeValue(jsonStr, Map.class);
        } catch (DecodeException e) {
            return false;
        }
        return true;
    }

    /**
     * Gets the safeJson array.
     *
     * @param key the key
     * @return this
     */
    public SafeJsonObject getJsonArray(String key) {
        this.array = super.getArray(key);
        return this;
    }

    /**
     * Make safeJson array.
     *
     * @param array the array
     * @return this
     */
    public SafeJsonObject makeArray(JsonArray array) {
        this.array = new JsonArray().addArray(array);
        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    @Override
    public SafeJsonObject clone() {
        try {
            super.clone();
        } catch (CloneNotSupportedException ignore) {}

        return new SafeJsonObject(this.encode());
    }

    /**
     * Gets list representation of the jsonArray
     *
     * @return the list
     */
    @SuppressWarnings("unchecked")
    public List<Object> toList() {
        return array.toList();
    }
}
