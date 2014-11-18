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
import com.globo.galeb.core.entity.IJsonable;
import com.globo.galeb.criteria.impl.RandomCriterion;

/**
 * Class BadBackendPool.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 10, 2014.
 */
public class BadBackendPool extends EntitiesMap<IBackend> {

    /**
     * Instantiates a new bad backend pool.
     */
    public BadBackendPool() {
        this("UNDEF");
    }

    /**
     * Instantiates a new bad backend pool.
     *
     * @param id the id
     */
    public BadBackendPool(String id) {
        this(new JsonObject().putString(IJsonable.ID_FIELDNAME, id));
    }

    /**
     * Instantiates a new bad backend pool.
     *
     * @param json the json
     */
    public BadBackendPool(JsonObject json) {
        super(json);

    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.entity.Entity#start()
     */
    @Override
    public void start() {
        setCriterion(new RandomCriterion<IBackend>().setLog(logger));
    }

}

