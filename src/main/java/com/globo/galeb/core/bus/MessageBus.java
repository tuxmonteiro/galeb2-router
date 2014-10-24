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

import com.globo.galeb.core.IJsonable;
import com.globo.galeb.core.SafeJsonObject;

/**
 * Class MessageBus.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class MessageBus {

    /** The Constant ENTITY_FIELDNAME. */
    public static final String ENTITY_FIELDNAME    = "entity";

    /** The Constant PARENT_ID_FIELDNAME. */
    public static final String PARENT_ID_FIELDNAME = "parentId";

    /** The Constant URI_FIELDNAME. */
    public static final String URI_FIELDNAME       = "uri";

    /** The entity str. */
    private String entityStr  = "{}";

    /** The parent id. */
    private String parentId   = "";

    /** The uri str. */
    private String uriStr     = "";

    /** The message bus. */
    private String messageBus = "{}";

    /**
     * Instantiates a new message bus.
     */
    public MessageBus() {
        this("{}");
    }

    /**
     * Instantiates a new message bus.
     *
     * @param message the message
     */
    public MessageBus(String message) {
        SafeJsonObject json = new SafeJsonObject(message);
        setEntity(json.getString(ENTITY_FIELDNAME,"{}"));
        setParentId(json.getString(PARENT_ID_FIELDNAME, ""));
        setUri(json.getString(URI_FIELDNAME, ""));
        make();
    }

    /**
     * Gets the parent id.
     *
     * @return the parent id
     */
    public String getParentId() {
        return parentId;
    }

    /**
     * Sets the parent id.
     *
     * @param parentId the parent id
     * @return this
     */
    public MessageBus setParentId(String parentId) {
        if (parentId!=null) {
            this.parentId = parentId;
        }
        return this;
    }

    /**
     * Gets the entity.
     *
     * @return the entity
     */
    public SafeJsonObject getEntity() {
        return new SafeJsonObject(entityStr);
    }

    /**
     * Gets the entity id.
     *
     * @return the entity id
     */
    public String getEntityId() {
        return getEntity().getString(IJsonable.ID_FIELDNAME, "");
    }

    /**
     * Sets the entity.
     *
     * @param entityStr the entity str
     * @return this
     */
    public MessageBus setEntity(String entityStr) {
        this.entityStr = new SafeJsonObject(entityStr).encode();
        return this;
    }

    /**
     * Sets the entity.
     *
     * @param entityJson the entity json
     * @return this
     */
    public MessageBus setEntity(SafeJsonObject entityJson) {
        if (entityJson!=null) {
            this.entityStr = entityJson.encode();
        } else {
            this.entityStr = "{}";
        }
        return this;
    }

    /**
     * Gets the uri.
     *
     * @return the uri
     */
    public String getUri() {
        return uriStr;
    }

    /**
     * Sets the uri.
     *
     * @param uriStr the uri str
     * @return this
     */
    public MessageBus setUri(String uriStr) {
        if (uriStr!=null) {
            this.uriStr = uriStr;
        }
        return this;
    }

    /**
     * Gets the uri base.
     *
     * @return the uri base
     */
    public String getUriBase() {
        String[] uriStrArray = uriStr.split("/");
        return uriStrArray.length > 1 ? uriStrArray[1] : "";
    }

    /**
     * Make messageBus message.
     *
     * @return this
     */
    public MessageBus make() {

        messageBus = new SafeJsonObject()
                            .putString(URI_FIELDNAME, uriStr)
                            .putString(PARENT_ID_FIELDNAME, parentId)
                            .putString(ENTITY_FIELDNAME, getEntity().encode())
                            .encode();
        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return messageBus;
    }

    /**
     * Json representation of the messageBus.
     *
     * @return the safe json object
     */
    public SafeJsonObject toJson() {
        return new SafeJsonObject(messageBus);
    }

}
