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
package com.globo.galeb.bus;

import com.globo.galeb.entity.impl.backend.Backend;
import com.globo.galeb.entity.impl.backend.BackendPool;

/**
 * Class BackendMap.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class BackendMap extends MessageToMap<BackendPool> {

    /**
     * Instantiates a new backend map.
     */
    public BackendMap() {
        super();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.MessageToMap#add()
     */
    @Override
    public boolean add() {
        boolean isOk = false;

        if ("".equals(parentId)) {
            log.error(String.format("[%s] Inaccessible ParentId: %s", verticleId, entity.encode()));
            return false;
        }

        if (farm.getBackendPoolById(parentId)==null) {
            log.warn(String.format("[%s] Backend not created, because BackendPool %s not exist", verticleId, parentId));
            return false;
        } else {

            boolean status = entity.getBoolean(Backend.ELEGIBLE_FIELDNAME, true);

            final BackendPool backendPool = farm.getBackendPoolById(parentId);
            isOk  = status ? backendPool.addEntity(new Backend(entity)) :
                             backendPool.addBadBackend(new Backend(entity));

            if (isOk) {
                log.info(String.format("[%s] Backend %s (%s) added", verticleId, entityId, parentId));
            } else {
                log.warn(String.format("[%s] Backend %s (%s) already exist", verticleId, entityId, parentId));
            }
        }
        return isOk;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.MessageToMap#del()
     */
    @Override
    public boolean del() {
        boolean isOk = false;
        boolean hasUriBaseOnly = ("/"+messageBus.getUriBase()).equals(messageBus.getUri()) ||
                messageBus.getUri().endsWith("/");

        if (!hasUriBaseOnly) {

            if ("".equals(parentId)) {
                log.error(String.format("[%s] Inaccessible ParentId: %s", verticleId, entity.encode()));
                return false;
            }
            boolean status = entity.getBoolean(Backend.ELEGIBLE_FIELDNAME, true);

            if ("".equals(entityId)) {
                log.warn(String.format("[%s] Backend UNDEF", verticleId));
                return false;
            } else if (farm.getBackendPoolById(parentId)==null) {
                log.warn(String.format("[%s] Backend not removed. BackendPool %s not exist", verticleId, parentId));
                return false;
            }
            final BackendPool backendPool = farm.getBackendPoolById(parentId);
            isOk = backendPool!=null;

            if (isOk) {
                isOk = status ? backendPool.removeEntity(entity) :
                                backendPool.removeBadBackend(entity);
                log.info(String.format("[%s] Backend %s (%s) removed", verticleId, entityId, parentId));
            } else {
                log.warn(String.format("[%s] Backend not removed. Backend %s (%s) not exist", verticleId, entityId, parentId));
            }

            return isOk;

        } else {
            for (BackendPool backendPool: farm.getBackendPools().getEntities().values()) {
                backendPool.clearEntities();
                backendPool.clearBadBackend();
            }
            log.info(String.format("[%s] All Backends removed", verticleId));
            return true;
        }
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.MessageToMap#reset()
     */
    @Override
    public boolean reset()  {
        // TODO
        return false;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.MessageToMap#change()
     */
    @Override
    public boolean change()  {
        // TODO
        return false;
    }

}
