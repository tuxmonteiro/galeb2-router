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

import com.globo.galeb.entity.impl.backend.BackendPool;

/**
 * Class BackendPoolMap.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 16, 2014.
 */
public class BackendPoolMap extends MessageToMap<BackendPool> {

    /**
     * Instantiates a new backend pool map.
     */
    public BackendPoolMap() {
        super();
        super.uriBase = "backendpool";
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.MessageToMap#add()
     */
    @Override
    public boolean add() {
        boolean isOk = false;

        defineLoggerIfNecessary();

        if ("".equals(entityId)) {
            log.error(String.format("[%s] Inaccessible Entity Id: %s", verticleId, entity.encode()));
            return false;
        }

        if (farm.getBackendPoolById(entityId)!=null) {
            log.warn(String.format("[%s] BackendPool not created. BackendPool %s already exist", verticleId, entityId));
            return false;
        }
        farm.addBackendPool(entity);

        if (farm.getBackendPoolById(entityId)!=null) {
            log.info(String.format("[%s] BackendPool %s added", verticleId, entityId));
            isOk = true;
        } else {
            isOk = false;
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

        defineLoggerIfNecessary();

        if (!hasUriBaseOnly) {

            if (farm.getBackendPoolById(entityId)!=null) {
                farm.removeBackendPool(entityId);
                log.info(String.format("[%s] BackendPool %s removed", verticleId, entityId));
                isOk = true;
            } else {
                log.warn(String.format("[%s] BackendPool not removed. BackendPool %s not exist", verticleId, entityId));
                isOk = false;
            }
            return isOk;
        } else {
            farm.clearBackendPools();
            log.info(String.format("[%s] All BackendPools removed", verticleId));
            return true;
        }
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.MessageToMap#reset()
     */
    @Override
    public boolean reset() {
        // TODO
        return false;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.MessageToMap#change()
     */
    @Override
    public boolean change() {
        // TODO
        return false;
    }

}
