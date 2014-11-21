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
package com.globo.galeb.entity.impl.backend;

import java.util.Iterator;

import com.globo.galeb.entity.EntitiesMap;

/**
 * Class BackendPools.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 10, 2014.
 */
public class BackendPools extends EntitiesMap<BackendPool> {

    /**
     * Instantiates a new backend pools.
     *
     * @param id the id
     */
    public BackendPools(String id) {
        super(id);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.entity.Entity#start()
     */
    @Override
    public void start() {
        // unnecessary
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.entity.EntitiesMap#clearEntities()
     */
    @Override
    public void clearEntities() {
        Iterator<BackendPool> iterator = getEntities().values().iterator();
        while (iterator.hasNext()) {
            BackendPool backendPool = iterator.next();
            backendPool.clearAll();
        }
        super.clearEntities();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.entity.EntitiesMap#removeEntity(com.globo.galeb.core.entity.Entity)
     */
    @Override
    public boolean removeEntity(BackendPool entity) {
        entity.clearAll();
        return super.removeEntity(entity);
    }

}
