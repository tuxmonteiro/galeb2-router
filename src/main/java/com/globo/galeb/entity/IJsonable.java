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

import org.vertx.java.core.json.JsonObject;

/**
 * Interface IJsonable.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public interface IJsonable {

    /** The Constant PK_FIELDNAME. */
    public static final String PK_FIELDNAME          = "pk";

    /** The Constant ID_FIELDNAME. */
    public static final String ID_FIELDNAME          = "id";

    /** The Constant PARENT_ID_FIELDNAME. */
    public static final String PARENT_ID_FIELDNAME   = "parentId";

    /** The Constant PROPERTIES_FIELDNAME. */
    public static final String PROPERTIES_FIELDNAME  = "properties";

    /** The Constant STATUS_FIELDNAME. */
    public static final String STATUS_FIELDNAME      = "_status";

    /** The Constant CREATED_AT_FIELDNAME. */
    public static final String CREATED_AT_FIELDNAME  = "_created_at";

    /** The Constant MODIFIED_AT_FIELDNAME. */
    public static final String MODIFIED_AT_FIELDNAME = "_modified_at";

    /** The Constant LINKS_FIELDNAME. */
    public static final String LINKS_FIELDNAME       = "_links";

    /** The Constant HASH_FIELDNAME. */
    public static final String HASH_FIELDNAME        = "_hash";

    /** The Constant LINKS_REL_FIELDNAME. */
    public static final String LINKS_REL_FIELDNAME   = "rel";

    /** The Constant LINKS_HREF_FIELDNAME. */
    public static final String LINKS_HREF_FIELDNAME  = "href";

    /** The Constant UNDEF. */
    public static final String UNDEF                 = "UNDEF";

    public static enum StatusType {

        ACCEPTED_STATUS("accepted"),
        FAILED_STATUS("failed"),
        RUNNING_STATUS("running"),
        CREATED("created");

        final String statusType;
        private StatusType(String statusType) {
            this.statusType = statusType;
        }

        @Override
        public String toString() {
            return this.statusType;
        }

    }

    /**
     * json representation of the Entity.
     *
     * @return the json object
     */
    public JsonObject toJson();

}
