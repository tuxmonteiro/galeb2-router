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
 * Class BackendPool.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 6, 2014.
 */
public class BackendPool extends EntitiesMap<Backend> {

    /** The bad backends. */
    private final EntitiesMap<Backend> badBackends       = new BadBackendPool("badbackends");

    /** The load balance policy. */
    private String                     loadBalancePolicy = "";

    /**
     * Instantiates a new backend pool.
     */
    public BackendPool() {
        this("UNDEF");
    }

    /**
     * Instantiates a new backend pool.
     *
     * @param id the id
     */
    public BackendPool(String id) {
        this(new JsonObject().putString(IJsonable.ID_FIELDNAME, id));
    }

    /**
     * Instantiates a new backend pool.
     *
     * @param json the json
     */
    public BackendPool(JsonObject json) {
        super(json);

        setCriterion(new RandomCriterion<Backend>());
    }

    /**
     * Gets the load balance policy.
     *
     * @return the load balance policy
     */
    public String getLoadBalancePolicy() {
        return loadBalancePolicy;
    }

    /**
     * Sets the load balance policy.
     *
     * @param loadBalancePolicy the load balance policy
     * @return the backend pool
     */
    public BackendPool setLoadBalancePolicy(String loadBalancePolicy) {
        this.loadBalancePolicy = loadBalancePolicy;
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.entity.EntitiesMap#clearEntities()
     */
    @Override
    public void clearEntities() {
        for (Backend backend: getEntities().values()) {
            backend.closeAllForced();
        }
        super.clearEntities();
    }

    /**
     * Gets the bad backends map.
     *
     * @return the bad backends map
     */
    public EntitiesMap<Backend> getBadBackends() {
        return badBackends;
    }

    /**
     * Gets the length of bad backend pool.
     *
     * @return the length of bad backend pool
     */
    public int getNumBadBackend() {
        return badBackends.getNumEntities();
    }

    /**
     * Gets the bad backend by id.
     *
     * @param entityId the entity id
     * @return the bad backend by id
     */
    public Backend getBadBackendById(String entityId) {
        return badBackends.getEntityById(entityId);
    }

    /**
     * Clear bad backend.
     */
    public void clearBadBackend() {
        for (Backend backend: badBackends.getEntities().values()) {
            backend.closeAllForced();
        }
        badBackends.clearEntities();
    }

    /**
     * Adds the bad backend.
     *
     * @param entity the entity
     * @return true, if successful
     */
    public boolean addBadBackend(Backend entity) {
        return badBackends.addEntity(entity);
    }

    /**
     * Removes the bad backend.
     *
     * @param entity the entity
     * @return true, if successful
     */
    public boolean removeBadBackend(Backend entity) {
        return badBackends.removeEntity(entity);
    }

    /**
     * Clear all.
     */
    public void clearAll() {
        clearEntities();
        clearBadBackend();
    }

}

