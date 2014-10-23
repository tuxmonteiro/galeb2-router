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
package com.globo.galeb.core.bus;

import com.globo.galeb.core.Backend;
import com.globo.galeb.core.Virtualhost;

/**
 * Class BackendMap.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class BackendMap extends MessageToMap<Virtualhost> {

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
        if (!map.containsKey(parentId)) {
            log.warn(String.format("[%s] Backend not created, because Virtualhost %s not exist", verticleId, parentId));
            return false;
        } else {

            boolean status = entity.getBoolean(Backend.ELEGIBLE_FIELDNAME, true);

            final Virtualhost vhost = map.get(parentId);
            if (vhost.addBackend(entity, status)) {
                log.info(String.format("[%s] Backend %s (%s) added", verticleId, entityId, parentId));
                isOk = true;
            } else {
                log.warn(String.format("[%s] Backend %s (%s) already exist", verticleId, entityId, parentId));
                isOk = false;
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
            } else if (!map.containsKey(parentId)) {
                log.warn(String.format("[%s] Backend not removed. Virtualhost %s not exist", verticleId, parentId));
                return false;
            }
            final Virtualhost virtualhostObj = map.get(parentId);
            if (virtualhostObj!=null && virtualhostObj.removeBackend(entityId, status)) {
                log.info(String.format("[%s] Backend %s (%s) removed", verticleId, entityId, parentId));
                isOk = true;
            } else {
                log.warn(String.format("[%s] Backend not removed. Backend %s (%s) not exist", verticleId, entityId, parentId));
                isOk = false;
            }

            return isOk;
        } else {
            for (Virtualhost virtualhost: map.values()) {
                virtualhost.clearAll();
                log.info(String.format("[%s] All Backends removed", verticleId));
            }
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
