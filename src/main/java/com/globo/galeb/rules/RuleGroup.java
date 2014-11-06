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
package com.globo.galeb.rules;

import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.core.entity.EntitiesMap;

/**
 * Class RuleGroup.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 6, 2014.
 */
public class RuleGroup extends EntitiesMap<Rule> {

    /**
     * Instantiates a new rule group.
     */
    public RuleGroup() {
        super("UNDEF");
    }

    /**
     * Instantiates a new rule group.
     *
     * @param json the json
     */
    public RuleGroup(JsonObject json) {
        super(json);
    }

    /**
     * Instantiates a new rule group.
     *
     * @param id the id
     */
    public RuleGroup(String id) {
        super(id);
    }

}
