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

import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.core.entity.EntitiesMap;
import com.globo.galeb.criteria.impl.RulesCriterion;
import com.globo.galeb.rules.Rule;

/**
 * Class VirtualServer.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 10, 2014.
 */
public class VirtualServer extends EntitiesMap<Rule> {

    /**
     * Instantiates a new virtual server.
     */
    public VirtualServer() {
        this("UNDEF");
    }

    /**
     * Instantiates a new virtual server.
     *
     * @param id the id
     */
    public VirtualServer(String id) {
        this(new JsonObject().putString(ID_FIELDNAME, id));
    }

    /**
     * Instantiates a new virtual server.
     *
     * @param json the json
     */
    public VirtualServer(JsonObject json) {
        super(json);
        setCriterion(new RulesCriterion().setLog(logger));
    }

}
