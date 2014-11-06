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

public abstract class EntitiesMap<T extends Entity> extends Entity {

    private Map<String, T> entities        = new HashMap<>();
    private ICriterion<T>  criterion       = new NullCriterion<T>();

    public EntitiesMap(String id) {
        super(id);
    }

    public EntitiesMap(JsonObject json) {
        super(json);
    }

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

    public void clearEntities() {
        entities.clear();
    }

    public T getEntityById(String entityId) {
        return entities.get(entityId);
    }

    public int getNumEntities() {
        return entities.size();
    }

    public T getEntityByCriterion() {
        return criterion.thenResult();
    }

    public EntitiesMap<T> setCriterion(final ICriterion<T> criterion) {
        this.criterion = criterion.given(entities);
        return this;
    }

}
