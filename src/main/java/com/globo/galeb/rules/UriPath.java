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

import java.util.UUID;

import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.criteria.impl.RequestMatch;

/**
 * Class UriPathRule.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 10, 2014.
 */
public class UriPath extends Rule {

    public UriPath() {
        this(new JsonObject().putString(ID_FIELDNAME, UUID.randomUUID().toString()));
    }

    public UriPath(JsonObject json) {
        super(json);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.rules.Rule#isMatchWith(com.globo.galeb.criteria.impl.RequestMatch)
     */
    @Override
    public boolean isMatchWith(RequestMatch requestMatch) {
        String uriPath = requestMatch.getUriPath();
        return uriPath.startsWith(match.toString());
    }

}
