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
package com.globo.galeb.core.entity;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.criteria.ICriterion;
import com.globo.galeb.criteria.impl.NullCriterion;

/**
 * Class EntitiesMap.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 7, 2014.
 * @param <T> the generic type
 */
public abstract class EntitiesMap<T extends Entity> extends Entity {

    /** The entities. */
    private Map<String, T> entities        = new HashMap<>();

    /** The criterion. */
    private ICriterion<T>  criterion       = new NullCriterion<T>();

    /**
     * Instantiates a new entities map.
     *
     * @param id the id
     */
    public EntitiesMap(String id) {
        super(id);
    }

    /**
     * Instantiates a new entities map.
     *
     * @param json the json
     */
    public EntitiesMap(JsonObject json) {
        super(json);
    }

    /**
     * Adds the entity.
     *
     * @param entity the entity
     * @return true, if successful
     */
    public boolean addEntity(T entity) {
        String id = entity.getId();

        if (entities.containsKey(id)) {
            return false;
        }

        entity.setPlataform(plataform);
        entity.setQueueService(queueService);
        entities.put(id, entity);
        if (entities.containsKey(id)) {
            updateModifiedTimestamp();
            return true;
        }
        return false;
    }

    /**
     * Removes the entity.
     *
     * @param entity the entity
     * @return true, if successful
     */
    public boolean removeEntity(T entity) {
        String id = entity.getId();

        if (!entities.containsKey(id)) {
            return false;
        }

        entities.remove(id);
        if (!entities.containsKey(id)) {
            updateModifiedTimestamp();
            return true;
        }
        return false;
    }

    /**
     * Clear entities.
     */
    public void clearEntities() {
        entities.clear();
    }

    /**
     * Gets the entities.
     *
     * @return the entities
     */
    public Map<String, T> getEntities() {
        return this.entities;
    }

    /**
     * Gets the entity by id.
     *
     * @param entityId the entity id
     * @return the entity by id
     */
    public T getEntityById(String entityId) {
        return entities.get(entityId);
    }

    /**
     * Gets the num entities.
     *
     * @return the num entities
     */
    public int getNumEntities() {
        return entities.size();
    }

    /**
     * Gets the entity by criterion.
     *
     * @return the entity by criterion
     */
    public T getEntityByCriterion() {
        return criterion.thenGetResult();
    }

    /**
     * Gets the criterion.
     *
     * @return the criterion
     */
    public ICriterion<T> getCriterion() {
        return criterion;
    }

    /**
     * Sets the criterion.
     *
     * @param criterion the criterion
     * @return the entities map
     */
    public EntitiesMap<T> setCriterion(final ICriterion<T> criterion) {
        this.criterion = criterion.given(entities);
        return this;
    }

}
