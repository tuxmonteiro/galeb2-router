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

    /**
     * Gets the bad backends.
     *
     * @return the bad backends
     */
    public EntitiesMap<Backend> getBadBackends() {
        return badBackends;
    }

    public int getNumBadBackend() {
        return badBackends.getNumEntities();
    }

    public Backend getBadBackendById(String entityId) {
        return badBackends.getEntityById(entityId);
    }

    public void clearBadBackend() {
        badBackends.clearEntities();
    }

    public boolean addBadBackend(Backend entity) {
        return badBackends.addEntity(entity);
    }

    public boolean removeBadBackend(Backend entity) {
        return badBackends.removeEntity(entity);
    }

}

