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
package com.globo.galeb.core.entity;

import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import com.globo.galeb.core.Farm;
import com.globo.galeb.core.Server;
import com.globo.galeb.core.bus.IQueueService;

/**
 * Class Entity.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public abstract class Entity implements IJsonable {

    /** The id. */
    protected String               id            = "";

    /** The parent id. */
    protected String               parentId      = "";

    /** The created at. */
    protected final Long           createdAt     = System.currentTimeMillis();

    /** The modified at. */
    protected Long                 modifiedAt    = System.currentTimeMillis();

    /** The properties. */
    protected JsonObject           properties    = new JsonObject();

    /** The id obj. */
    protected final JsonObject     idObj         = new JsonObject();

    /** The entity type. */
    protected String               entityType    = this.getClass().getSimpleName().toLowerCase();

    /** The plataform */
    protected Object               plataform     = null;

    /** The farm instance */
    protected Farm                 farm          = null;

    /** The queue service */
    protected IQueueService        queueService  = null;

    /** The Logger */
    protected Logger               logger        = null;

    /** The static conf. */
    protected JsonObject           staticConf    = new JsonObject();

    /**
     * Instantiates a new entity.
     */
    protected Entity() {
        this("UNDEF");
    }

    /**
     * Instantiates a new entity.
     *
     * @param id the string id
     */
    protected Entity(String id) {
        this.id = id;
    }

    /**
     * Instantiates a new entity.
     *
     * @param json the json
     */
    protected Entity(JsonObject json) {
        this(json.getString(IJsonable.ID_FIELDNAME, "UNDEF"));
        this.parentId = json.getString(IJsonable.PARENT_ID_FIELDNAME, "");
        this.properties.mergeIn(json.getObject(IJsonable.PROPERTIES_FIELDNAME, new JsonObject()));
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the properties.
     *
     * @return the properties
     */
    public JsonObject getProperties() {
        return properties;
    }

    /**
     * Gets the plataform.
     *
     * @return the plataform
     */
    public Object getPlataform() {
        return plataform;
    }

    /**
     * Sets the plataform.
     *
     * @param plataform plataform instance
     * @return this
     */
    public Entity setPlataform(final Object plataform) {
        this.plataform = plataform;
        updateModifiedTimestamp();
        return this;
    }

    /**
     * Gets the farm.
     *
     * @return the farm
     */
    public Farm getFarm() {
        return farm;
    }

    /**
     * Sets the farm.
     *
     * @param farm the farm
     * @return this
     */
    public Entity setFarm(Farm farm) {
        this.farm = farm;
        return this;
    }

    /**
     * Sets the queue service.
     *
     * @param queueService the queue service
     * @return this
     */
    public Entity setQueueService(final IQueueService queueService) {
        this.queueService = queueService;
        updateModifiedTimestamp();
        return this;
    }

    /**
     * Sets the logger instance.
     *
     * @param logger the logger instance
     * @return this
     */
    public Entity setLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    /**
     * Gets the logger.
     *
     * @return the logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Prepare json.
     *
     * @return this
     */
    protected Entity prepareJson() {
        idObj.putString(IJsonable.ID_FIELDNAME, id);
        idObj.putObject(LINKS_FIELDNAME, new JsonObject()
            .putString(LINKS_REL_FIELDNAME, "self")
            .putString(LINKS_HREF_FIELDNAME, String.format("http://%s/%s/%s", Server.getHttpServerName(), entityType, id))
        );
        idObj.putString(STATUS_FIELDNAME, "created");
        idObj.putNumber(IJsonable.CREATED_AT_FIELDNAME, createdAt);
        idObj.putNumber(IJsonable.MODIFIED_AT_FIELDNAME, modifiedAt);
        idObj.putObject(IJsonable.PROPERTIES_FIELDNAME, properties);
        return this;
    }

    /**
     * Sets the static conf.
     *
     * @param staticConf the static conf
     * @return this
     */
    public Entity setStaticConf(String staticConf) {
        if (!"".equals(staticConf)) {
            try {
                this.staticConf = new JsonObject(staticConf);
            } catch (DecodeException ignore) {}
        }
        updateModifiedTimestamp();
        return this;
    }

    /**
     * Sets the static conf.
     *
     * @param staticConf the static conf
     * @return this
     */
    public Entity setStaticConf(JsonObject staticConf) {
        setStaticConf(staticConf.encode());
        return this;
    }

    /**
     * Update modified timestamp.
     */
    protected void updateModifiedTimestamp() {
        modifiedAt = System.currentTimeMillis();
    }

    /**
     * Reset and Update properties.
     *
     * @param json the json with new properties
     */
    public void resetAndUpdateProperties(JsonObject json) {
        this.properties = json;
    }

    /**
     * Merge new properties.
     *
     * @param json the json with properties to merge
     */
    public void mergeNewProperties(JsonObject json) {
        this.properties.mergeIn(json);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IJsonable#toJson()
     */
    @Override
    public JsonObject toJson() {
        prepareJson();
        return idObj;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.id;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        return this.id.equals(obj.toString());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }
}
