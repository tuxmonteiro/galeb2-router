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

import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.entity.impl.frontend.NullRule;
import com.globo.galeb.entity.impl.frontend.Rule;
import com.globo.galeb.entity.impl.frontend.RuleFactory;
import com.globo.galeb.entity.impl.frontend.Virtualhost;

/**
 * Class RuleMap.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 23, 2014.
 */
public class RuleMap extends MessageToMap<Virtualhost> {

    /**
     * Instantiates a new rule map.
     */
    public RuleMap() {
        super();
        super.uriBase = "rule";
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.bus.MessageToMap#add()
     */
    @Override
    public boolean add() {
        boolean isOk = false;

        defineLoggerIfNecessary();

        if ("".equals(parentId)) {
            log.error(String.format("[%s] Inaccessible ParentId: %s", verticleId, entity.encode()));
            return false;
        }
        if (farm.getEntityById(parentId)==null) {
            log.warn(String.format("[%s] Rule not created, because Virtualhost %s not exist", verticleId, parentId));
            return false;
        } else {
            final Virtualhost virtualhost = farm.getEntityById(parentId);

            if (virtualhost!=null) {
                if (entity.containsField(IJsonable.PROPERTIES_FIELDNAME)) {
                    JsonObject properties = entity.getObject(IJsonable.PROPERTIES_FIELDNAME);
                    boolean hasOrderNum = properties.containsField(Rule.ORDERNUM_FIELDNAME);
                    if (!hasOrderNum) {
                        properties.putNumber(Rule.ORDERNUM_FIELDNAME, 99);
                        entity.getObject(IJsonable.PROPERTIES_FIELDNAME).mergeIn(properties);
                    }
                    boolean hasMatch = properties.containsField(Rule.MATCH_FIELDNAME);
                    if (!hasMatch) {
                        properties.putString(Rule.MATCH_FIELDNAME, "-UNDEFINED-");
                        entity.getObject(IJsonable.PROPERTIES_FIELDNAME).mergeIn(properties);
                    }

                }
                Rule rule = new RuleFactory().setLogger(log).createRule(entity);
                if (!(rule instanceof NullRule)) {
                    rule.setFarm(farm).start();
                    isOk  = virtualhost.addEntity(rule);
                    log.info(String.format("[%s] Rule %s (%s) added", verticleId, entityId, parentId));
                } else {
                    log.error(String.format("[%s] Rule %s (%s) wrong format", verticleId, entityId, parentId));
                }
            } else {
                log.warn(String.format("[%s] Rule %s (%s) already exist", verticleId, entityId, parentId));
            }

        }
        return isOk;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.bus.MessageToMap#del()
     */
    @Override
    public boolean del() {
        boolean isOk = false;
        boolean hasUriBaseOnly = ("/"+messageBus.getUriBase()).equals(messageBus.getUri()) ||
                messageBus.getUri().endsWith("/");

        defineLoggerIfNecessary();

        if (!hasUriBaseOnly) {

            if ("".equals(parentId)) {
                log.error(String.format("[%s] Inaccessible ParentId: %s", verticleId, entity.encode()));
                return false;
            }

            if ("".equals(entityId)) {
                log.warn(String.format("[%s] Rule UNDEF", verticleId));
                return false;
            } else if (farm.getEntityById(parentId)==null) {
                log.warn(String.format("[%s] Rule not removed. Virtualhost %s not exist", verticleId, parentId));
                return false;
            }
            final Virtualhost virtualhost = farm.getEntityById(parentId);
            isOk = virtualhost!=null;

            if (isOk) {
                isOk = virtualhost.removeEntity(entity);
                log.info(String.format("[%s] Rule %s (%s) removed", verticleId, entityId, parentId));
            } else {
                log.warn(String.format("[%s] Rule not removed. Rule %s (%s) not exist", verticleId, entityId, parentId));
            }

            return isOk;

        } else {
            for (Virtualhost virtualhost: farm.getEntities().values()) {
                virtualhost.clearEntities();
            }
            log.info(String.format("[%s] All Rules removed", verticleId));
            return true;
        }
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.bus.MessageToMap#reset()
     */
    @Override
    public boolean reset() {
        // TODO
        return false;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.bus.MessageToMap#change()
     */
    @Override
    public boolean change() {
        // TODO
        return false;
    }

}
