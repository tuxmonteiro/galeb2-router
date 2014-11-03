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
package com.globo.galeb.core.bus;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

/**
 * Class MessageToMap.
 *
 * @param <T> the generic type
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public abstract class MessageToMap<T> {

    /** The uri base. */
    protected String uriBase        = "";

    /** The entity. */
    protected JsonObject entity = new JsonObject();

    /** The entity id. */
    protected String entityId       = "";

    /** The parent id. */
    protected String parentId       = "";

    /** The map. */
    protected Map<String, T> map    = new HashMap<>();

    /** The message bus. */
    protected MessageBus messageBus = new MessageBus().setUri("/null");

    /** The logger. */
    protected Logger log            = null;

    /** The verticle id. */
    protected String verticleId     = "";

    /** The vertx. */
    protected Vertx vertx           = null;

    /** The static conf. */
    protected String staticConf     = "";

    /**
     * Sets the verticle id.
     *
     * @param id the id
     * @return this
     */
    public MessageToMap<T> setVerticleId(String id) {
        this.verticleId = id;
        return this;
    }

    /**
     * Sets the logger.
     *
     * @param log the logger
     * @return this
     */
    public MessageToMap<T> setLogger(final Logger log) {
        this.log = log;
        return this;
    }

    /**
     * Sets the message bus.
     *
     * @param messageBus the message bus
     * @return this
     */
    public MessageToMap<T> setMessageBus(final MessageBus messageBus) {
        this.messageBus = messageBus;

        if (messageBus!=null) {
            this.uriBase  = messageBus.getUriBase();
            this.entity   = messageBus.getEntity();
            this.entityId = messageBus.getEntityId();
            this.parentId = messageBus.getParentId();
        }

        return this;
    }

    /**
     * Sets the map.
     *
     * @param map the map
     * @return this
     */
    public MessageToMap<T> setMap(final Map<String, T> map) {
        if (map!=null) {
            this.map = map;
        }
        return this;
    }

    /**
     * Sets the vertx.
     *
     * @param vertx the vertx
     * @return this
     */
    public MessageToMap<T> setVertx(final Vertx vertx) {
        this.vertx = vertx;
        return this;
    }

    /**
     * Adds the entity.
     *
     * @return true, if successful
     */
    public boolean add() {
        return false;
    }

    /**
     * Del the entity
     *
     * @return true, if successful
     */
    public boolean del() {
        return false;
    }

    /**
     * Reset the entity
     *
     * @return true, if successful
     */
    public boolean reset() {
        return false;
    }

    /**
     * Change the entity
     *
     * @return true, if successful
     */
    public boolean change() {
        return false;
    }

    /**
     * Sets static conf (from configuration file).
     *
     * @param jsonConf the json conf
     * @return this
     */
    public MessageToMap<T> staticConf(String jsonConf) {
        this.staticConf = jsonConf;
        return this;
    }

}
