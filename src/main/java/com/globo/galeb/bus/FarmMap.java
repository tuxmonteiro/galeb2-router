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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.net.URLDecoder;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.entity.Entity;
import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.entity.impl.Farm;
import com.globo.galeb.entity.impl.backend.BackendPool;
import com.globo.galeb.entity.impl.backend.IBackend;
import com.globo.galeb.entity.impl.frontend.Rule;
import com.globo.galeb.entity.impl.frontend.Virtualhost;

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
        super.uriBase = "farm";
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.MessageToMap#add()
     */
    @Override
    public boolean add() {
        boolean isOk = false;

        defineLoggerIfNecessary();

        JsonArray backendPools = entity.getArray("backendpools", new JsonArray());

        Iterator<Object> backendPoolsIterator = backendPools.iterator();
        while (backendPoolsIterator.hasNext()) {

            Object backendPoolObj = backendPoolsIterator.next();
            JsonObject backendPoolJson = (JsonObject) backendPoolObj;

            BackendPoolMap backendPoolMap = new BackendPoolMap();
            backendPoolMap.staticConf(staticConf);

            MessageBus backendPoolMessageBus = new MessageBus()
                                                .setEntity(backendPoolJson.encode())
                                                .setUri("/backendpool")
                                                .make();

            backendPoolMap.setMessageBus(backendPoolMessageBus).setFarm(farm);

            backendPoolMap.add();

            JsonArray backends = backendPoolJson.getArray(BackendPool.BACKENDS_FIELDNAME);
            if (backends==null) {
                return isOk;
            }

            Iterator<Object> backendIterator = backends.iterator();
            while (backendIterator.hasNext()) {
                Object backendObj = backendIterator.next();
                JsonObject backendJson = (JsonObject) backendObj;

                BackendMap backendMap = new BackendMap();
                backendMap.staticConf(staticConf);

                String backendPoolId = backendPoolJson.getString(IJsonable.ID_FIELDNAME);
                MessageBus backendMessageBus = new MessageBus()
                                                    .setEntity(backendJson.encode())
                                                    .setParentId(backendPoolId)
                                                    .setUri("/backend")
                                                    .make();

                backendMap.setMessageBus(backendMessageBus).setFarm(farm);

                backendMap.add();
            }
            isOk = true;
        }

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

            virtualhostMap.setMessageBus(virtualhostMessageBus).setFarm(farm);

            virtualhostMap.add();

            JsonArray rules = virtualhostJson.getArray("rules");
            if (rules==null) {
                return isOk;
            }

            Iterator<Object> ruleIterator = rules.iterator();
            while (ruleIterator.hasNext()) {
                Object ruleObj = ruleIterator.next();
                JsonObject ruleJson = (JsonObject) ruleObj;
                String ruleParentId = ruleJson.getString(IJsonable.PARENT_ID_FIELDNAME);

                RuleMap ruleMap = new RuleMap();
                ruleMap.staticConf(staticConf);

                MessageBus ruleMessageBus = new MessageBus()
                                                    .setEntity(ruleJson.encode())
                                                    .setParentId(ruleParentId)
                                                    .setUri("/rule")
                                                    .make();

                ruleMap.setMessageBus(ruleMessageBus).setFarm(farm);

                ruleMap.add();
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

        defineLoggerIfNecessary();

        boolean isOk = true;

        VirtualhostMap virtualhostMap = new VirtualhostMap();
        RuleMap ruleMap = new RuleMap();
        BackendPoolMap backendPoolMap = new BackendPoolMap();
        BackendMap backendMap = new BackendMap();

        List<Virtualhost> virtualhosts = new ArrayList<>();
        virtualhosts.addAll(farm.getEntities().values());

        for (Virtualhost virtualhost: virtualhosts) {

            List<Rule> rules = new ArrayList<>();
            rules.addAll(virtualhost.getEntities().values());

            for (Rule rule: rules) {
                MessageBus ruleMessageBus = null;
                try {
                    ruleMessageBus = new MessageBus()
                                        .setEntity(rule.toJson())
                                        .setParentId(rule.getParentId())
                                        .setUri("/rule/"+URLDecoder.decode(rule.getId(), "UTF-8"))
                                        .make();
                } catch (UnsupportedEncodingException e) {
                    log.error(e.getMessage());
                }
                isOk = isOk && ruleMap.setMessageBus(ruleMessageBus).setFarm(farm).del();
            }

            MessageBus virtualhostMessageBus = null;
            try {
                virtualhostMessageBus = new MessageBus()
                                            .setEntity(virtualhost.toJson())
                                            .setUri("/virtualhost/"+URLDecoder.decode(virtualhost.getId(), "UTF-8"))
                                            .make();
            } catch (UnsupportedEncodingException e) {
                log.error(e.getMessage());
            }

            isOk = isOk && virtualhostMap.setMessageBus(virtualhostMessageBus).setFarm(farm).del();
        }

        List<BackendPool> backendPools = new ArrayList<>();
        backendPools.addAll(farm.getBackendPools().getEntities().values());

        for (BackendPool backendPool: backendPools) {

            List<IBackend> backends = new ArrayList<>();
            backends.addAll(backendPool.getEntities().values());

            for (IBackend backend: backends) {
                MessageBus backendMessageBus = null;
                try {
                    backendMessageBus = new MessageBus()
                                            .setEntity(backend.toJson())
                                            .setParentId(((Entity) backend).getParentId())
                                            .setUri("/backend/"+URLDecoder.decode(((Entity) backend).getId(), "UTF-8"))
                                            .make();
                } catch (UnsupportedEncodingException e) {
                    log.error(e.getMessage());
                }
                isOk = isOk && backendMap.setMessageBus(backendMessageBus).setFarm(farm).del();
            }

            MessageBus backendPoolMessageBus = null;
            try {
                backendPoolMessageBus = new MessageBus()
                                            .setEntity(backendPool.toJson())
                                            .setUri("/backendpool/"+URLDecoder.decode(backendPool.getId(), "UTF-8"))
                                            .make();
            } catch (UnsupportedEncodingException e) {
                log.error(e.getMessage());
            }

            isOk = isOk && backendPoolMap.setMessageBus(backendPoolMessageBus).setFarm(farm).del();
        }

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
