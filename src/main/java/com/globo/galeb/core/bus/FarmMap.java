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

import java.util.Iterator;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.core.Farm;
import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.core.entity.IJsonable;

/**
 * Class FarmMap.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class FarmMap extends MessageToMap<Farm> {

    /**
     * Instantiates a new farm map.
     */
    public FarmMap() {
        super();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.MessageToMap#add()
     */
    @Override
    public boolean add() {
        boolean isOk = false;
        Farm farm = map.get("farm");

        JsonArray virtualhosts = entity.getArray("virtualhosts", new JsonArray());

        Iterator<Object> virtualhostIterator = virtualhosts.iterator();
        while (virtualhostIterator.hasNext()) {
            Object virtualhostObj = virtualhostIterator.next();
            JsonObject virtualhostJson = (JsonObject) virtualhostObj;

            VirtualhostMap virtualhostMap = new VirtualhostMap();
            virtualhostMap.staticConf(staticConf);

            MessageBus virtualhostMessageBus = new MessageBus()
                                                .setEntity(virtualhostJson.encode())
                                                .setUri("/virtualhost")
                                                .make();

            virtualhostMap.setMessageBus(virtualhostMessageBus)
                          .setLogger(log)
                          .setVertx(vertx)
                          .setMap(farm.getEntities())
                          .setVerticleId(verticleId);

            virtualhostMap.add();

            JsonArray backends = virtualhostJson.getObject(Virtualhost.BACKENDS_FIELDNAME, new JsonObject())
                                                .getArray(Virtualhost.BACKENDS_ELIGIBLE_FIELDNAME);
            if (backends==null) {
                continue;
            }

            Iterator<Object> backendIterator = backends.iterator();
            while (backendIterator.hasNext()) {
                Object backendObj = backendIterator.next();
                JsonObject backendJson = (JsonObject) backendObj;

                BackendMap backendMap = new BackendMap();
                backendMap.staticConf(staticConf);

                MessageBus backendMessageBus = new MessageBus()
                                                    .setEntity(backendJson.encode())
                                                    .setParentId(virtualhostJson.getString(IJsonable.ID_FIELDNAME))
                                                    .setUri("/backend")
                                                    .make();

                backendMap.setMessageBus(backendMessageBus)
                          .setLogger(log)
                          .setVertx(vertx)
                          .setMap(farm.getEntities())
                          .setVerticleId(verticleId);
                backendMap.add();
            }
            isOk = true;
        }

        return isOk;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.MessageToMap#del()
     */
    @Override
    public boolean del() {
        boolean isOk = false;
        Farm farm = map.get("farm");

        farm.clearEntities();

        return isOk;
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
