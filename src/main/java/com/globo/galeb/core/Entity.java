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

import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;

/**
 * Class Entity.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public abstract class Entity implements IJsonable {

    /** The static conf. */
    protected JsonObject           staticConf    = new JsonObject();

    /** The id. */
    protected String               id            = "";

    /** The created at. */
    protected final Long           createdAt     = System.currentTimeMillis();

    /** The modified at. */
    protected Long                 modifiedAt    = System.currentTimeMillis();

    /** The properties. */
    protected final JsonObject     properties    = new JsonObject();

    /** The id obj. */
    protected final JsonObject     idObj         = new JsonObject();

    /** The entity type. */
    protected String               entityType    = this.getClass().getSimpleName().toLowerCase();

    /**
     * Gets the properties.
     *
     * @return the properties
     */
    public JsonObject getProperties() {
        return properties;
    }

    /**
     * Prepare json.
     *
     * @return the entity
     */
    protected Entity prepareJson() {
        idObj.putString(IJsonable.ID_FIELDNAME, id);
        idObj.putObject(LINKS_FIELDNAME, new SafeJsonObject()
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
     * @return the entity
     */
    public Entity setStaticConf(String staticConf) {
        if (!"".equals(staticConf)) {
            try {
                this.staticConf = new JsonObject(staticConf);
            } catch (DecodeException ignore) {}
        }
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.IJsonable#toJson()
     */
    @Override
    public JsonObject toJson() {
        return idObj;
    }
}
