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

import java.util.Map;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.collection.IndexedMap;
import com.globo.galeb.criteria.ICriterion;
import com.globo.galeb.criteria.impl.NullCriterion;

/**
 * Class EntitiesMap.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 7, 2014.
 * @param <T> the generic type
 */
public abstract class EntitiesMap<T> extends Entity {

    /** The entities. */
    private Map<String, T> entities        = new IndexedMap<>();

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
     * Instantiates a new entities map.
     */
    public EntitiesMap() {
        super(UNDEF);
    }

    /**
     * Adds the entity.
     *
     * @param entity the entity
     * @return true, if successful
     */
    public boolean addEntity(final T entity) {
        String id = ((Entity) entity).getId();

        if (entities.containsKey(id)) {
            return false;
        }

        ((Entity) entity).setFarm(farm)
              .setLogger(logger)
              .setQueueService(queueService)
              .setPlataform(plataform)
              .setStaticConf(staticConf)
              .setCounter(counter)
              .setStatus(StatusType.RUNNING_STATUS);

        ((Entity) entity).start();

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
     * @param entityId the entity id
     * @return true, if successful
     */
    public boolean removeEntity(String entityId) {
        if (!entities.containsKey(entityId)) {
            return false;
        }

        entities.remove(entityId);
        if (!entities.containsKey(entityId)) {
            updateModifiedTimestamp();
            return true;
        }
        return false;
    }

    /**
     * Removes the entity.
     *
     * @param json the json
     * @return true, if successful
     */
    public boolean removeEntity(JsonObject json) {
        String entityId = json.getString(ID_FIELDNAME, UNDEF);
        return removeEntity(entityId);
    }


    /**
     * Removes the entity.
     *
     * @param entity the entity
     * @return true, if successful
     */
    public boolean removeEntity(final T entity) {
        String entityId = ((Entity) entity).getId();
        return removeEntity(entityId);
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

    /**
     * Gets the entities json.
     *
     * @return the entities json
     */
    public JsonArray getEntitiesJson() {
        JsonArray jsonArray = new JsonArray();

        for (T t: getEntities().values()) {
            jsonArray.add(((IJsonable) t).toJson());
        }
        return jsonArray;
    }
}
