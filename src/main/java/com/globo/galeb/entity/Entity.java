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
package com.globo.galeb.entity;

import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import com.globo.galeb.bus.IQueueService;
import com.globo.galeb.entity.impl.Farm;
import com.globo.galeb.metrics.CounterConsoleOut;
import com.globo.galeb.metrics.ICounter;
import com.globo.galeb.server.Server;

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

    /** The counter. */
    protected ICounter             counter       = new CounterConsoleOut();

    /** The entity status */
    protected String               status        = UNDEF;

    /**
     * Instantiates a new entity.
     */
    protected Entity() {
        this(UNDEF);
    }

    /**
     * Instantiates a new entity.
     *
     * @param id the string id
     */
    protected Entity(String id) {
        this(new JsonObject().putString(ID_FIELDNAME, id));
    }

    /**
     * Instantiates a new entity.
     *
     * @param json the json
     */
    protected Entity(JsonObject json) {
        this.id = json.getString(IJsonable.ID_FIELDNAME, UNDEF);
        this.parentId = json.getString(IJsonable.PARENT_ID_FIELDNAME, "");
        this.properties.mergeIn(json.getObject(IJsonable.PROPERTIES_FIELDNAME, new JsonObject()));
        idObj.mergeIn(json);
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
    public Entity setFarm(final Farm farm) {
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
    public Entity setLogger(final Logger logger) {
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
        idObj.putString(ID_FIELDNAME, id);
        idObj.putString(STATUS_FIELDNAME, status);
        idObj.putObject(LINKS_FIELDNAME, new JsonObject()
            .putString(LINKS_REL_FIELDNAME, "self")
            .putString(LINKS_HREF_FIELDNAME,
                    String.format("http://%s/%s/%s", Server.getHttpServerName(), entityType, id))
        );
        idObj.putString(STATUS_FIELDNAME, "created");
        idObj.putNumber(CREATED_AT_FIELDNAME, createdAt);
        idObj.putNumber(MODIFIED_AT_FIELDNAME, modifiedAt);
        idObj.putObject(PROPERTIES_FIELDNAME, properties);
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
     * Sets the counter.
     *
     * @param counter the counter
     * @return this
     */
    public Entity setCounter(ICounter counter) {
        this.counter = counter;
        return this;
    }

    /**
     * Gets the status.
     *
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status
     * @return the entity
     */
    public Entity setStatus(String status) {
        this.status = status;
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

    /**
     * Gets the or create json property.
     *
     * @param fieldName the field name
     * @param defaultData the default data
     * @return the or create json property
     */
    public Object getOrCreateProperty(String fieldName, Object defaultData) {
        if (!properties.containsField(fieldName)) {
            properties.putValue(fieldName, defaultData);
            updateModifiedTimestamp();
        }
        return properties.getField(fieldName);
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
        if (obj==null) {
            return false;
        }
        return this.id.equals(obj.toString());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    /**
     * Start entity.
     */
    public void start() {
        //
    }
}
