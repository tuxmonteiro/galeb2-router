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
package com.globo.galeb.bus;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.entity.Entity;
import com.globo.galeb.entity.impl.Farm;
import com.globo.galeb.logger.SafeLogger;

/**
 * Class MessageToMap.
 *
 * @param <T> the generic type
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public abstract class MessageToMap<T extends Entity> {

    /** The uri base. */
    protected String uriBase        = Entity.UNDEF;

    /** The entity. */
    protected JsonObject entity     = new JsonObject();

    /** The entity id. */
    protected String entityId       = "";

    /** The parent id. */
    protected String parentId       = "";

    /** The farm. */
    protected Farm farm             = null;

    /** The message bus. */
    protected MessageBus messageBus = new MessageBus().setUri("/null");

    /** The logger. */
    protected SafeLogger log        = null;

    /** The verticle id. */
    protected String verticleId     = "";

    /** The vertx. */
    protected Vertx vertx           = null;

    /** The static conf. */
    protected String staticConf     = "";

    /**
     * Sets the message bus.
     *
     * @param messageBus the message bus
     * @return this
     */
    public MessageToMap<T> setMessageBus(final MessageBus messageBus) {
        this.messageBus = messageBus;

        if (messageBus!=null) {
            this.entity   = messageBus.getEntity();
            this.entityId = messageBus.getEntityId();
            this.parentId = messageBus.getParentId();
        }

        return this;
    }

    /**
     * Sets the farm.
     *
     * @param farm the farm
     * @return the message to map
     */
    public MessageToMap<T> setFarm(Farm farm) {
        this.farm = farm;
        this.log = farm.getLogger();
        Object plataform = farm.getPlataform();
        this.vertx = (plataform instanceof Vertx) ? (Vertx) plataform : null;
        this.verticleId = farm.getVerticleId();
        return this;
    }

    /**
     * Gets the uri base.
     *
     * @return the uri base
     */
    public String getUriBase() {
        return uriBase;
    }

    /**
     * Adds the entity.
     *
     * @return true, if successful
     */
    public abstract boolean add();

    /**
     * Del the entity
     *
     * @return true, if successful
     */
    public abstract boolean del();

    /**
     * Reset the entity
     *
     * @return true, if successful
     */
    public abstract boolean reset();

    /**
     * Change the entity
     *
     * @return true, if successful
     */
    public abstract boolean change();

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

    /**
     * Define logger if necessary.
     */
    protected void defineLoggerIfNecessary() {
        if (log==null) {
            log = new SafeLogger();
        }
    }

}
