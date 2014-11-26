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
package com.globo.galeb.entity.impl;

import static com.globo.galeb.verticles.ConfVerticleDictionary.CONF_STARTER_CONF;
import static com.globo.galeb.verticles.ConfVerticleDictionary.CONF_ROOT_ROUTER;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import com.globo.galeb.bus.ICallbackQueueAction;
import com.globo.galeb.bus.MessageToMap;
import com.globo.galeb.bus.MessageToMapBuilder;
import com.globo.galeb.criteria.impl.HostHeaderCriterion;
import com.globo.galeb.entity.EntitiesMap;
import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.entity.impl.backend.Backend;
import com.globo.galeb.entity.impl.backend.BackendPool;
import com.globo.galeb.entity.impl.backend.BackendPools;
import com.globo.galeb.entity.impl.backend.IBackend;
import com.globo.galeb.entity.impl.frontend.Rule;
import com.globo.galeb.entity.impl.frontend.Virtualhost;
import com.globo.galeb.verticles.ConfVerticleDictionary;

/**
 * Class Farm.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class Farm extends EntitiesMap<Virtualhost> implements ICallbackQueueAction {

    /** The Constant FARM_MAP. */
    public static final String FARM_MAP                    = "farm";

    /** The Constant FARM_BACKENDS_FIELDNAME. */
    public static final String FARM_BACKENDPOOLS_FIELDNAME = "backendpools";

    /** The Constant FARM_VIRTUALHOSTS_FIELDNAME. */
    public static final String FARM_VIRTUALHOSTS_FIELDNAME = "virtualhosts";

    /** The Constant FARM_VERSION_FIELDNAME. */
    public static final String FARM_VERSION_FIELDNAME      = "version";

    /** The Constant REQUEST_TIMEOUT_FIELDNAME. */
    public static final String REQUEST_TIMEOUT_FIELDNAME   = "requestTimeOut";

    /** The verticle. */
    private final Verticle verticle;

    /** The version. */
    private Long version = 0L;

    /** The backend pools. */
    private EntitiesMap<BackendPool> backendPools          = new BackendPools(BackendPools.class.getSimpleName());

    /** The messageToMapBuilder instance */
    private MessageToMapBuilder messageToMapBuilder        = new MessageToMapBuilder();


    /**
     * Instantiates a new farm.
     *
     * @param verticle the verticle
     */
    public Farm(final Verticle verticle) {
        super("farm");
        setFarm(this);
        this.verticle = verticle;
    }

    /**
     * Prepare backend pools.
     *
     * @return the farm
     */
    private Farm prepareBackendPools() {
        backendPools.setFarm(farm)
                    .setLogger(logger)
                    .setPlataform(plataform)
                    .setQueueService(queueService)
                    .setStaticConf(staticConf);
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.entity.Entity#start()
     */
    @Override
    public void start() {
        prepareBackendPools();
        registerQueueAction();
        properties.mergeIn(staticConf.getObject(ConfVerticleDictionary.CONF_STARTER_CONF, new JsonObject()));
        setCriterion(new HostHeaderCriterion<Virtualhost>().setLog(logger));
    }

    /**
     * Gets the version.
     *
     * @return the version
     */
    public Long getVersion() {
        return version;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.ICallbackQueueAction#setVersion(long)
     */
    @Override
    public void setVersion(long version) {
        this.version = version;
        String infoMessage = String.format("Version changed to %d", version);
        if (verticle!=null) {
            logger.info(infoMessage);
        } else {
            System.out.println(infoMessage);
        }
    }

    /**
     * Gets the verticle id.
     *
     * @return the verticle id
     */
    public String getVerticleId() {
        return (verticle!=null) ? verticle.toString() : "";
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.Entity#toJson()
     */
    @Override
    public JsonObject toJson() {
        prepareJson();
        idObj.putNumber(FARM_VERSION_FIELDNAME, version);
        idObj.putArray(FARM_VIRTUALHOSTS_FIELDNAME, getEntitiesJson());
        idObj.putArray(FARM_BACKENDPOOLS_FIELDNAME, getBackendPoolJson());

        return super.toJson();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.ICallbackQueueAction#addToMap(java.lang.String)
     */
    @Override
    public boolean addToMap(String message) {
        @SuppressWarnings("rawtypes")
        MessageToMap messageToMap = messageToMapBuilder.setFarm(this).getMessageToMap(message);
        if (properties.containsField(CONF_STARTER_CONF)) {
            JsonObject starterConf = properties.getObject(CONF_STARTER_CONF);
            if (starterConf.containsField(CONF_ROOT_ROUTER)) {
                JsonObject staticConf = starterConf.getObject(CONF_ROOT_ROUTER);
                return messageToMap.staticConf(staticConf.encode()).add();
            }
        }
        return messageToMap.add();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.ICallbackQueueAction#delFromMap(java.lang.String)
     */
    @Override
    public boolean delFromMap(String message) {
        return messageToMapBuilder.setFarm(this).getMessageToMap(message).del();
    }

    /**
     * Register queue action.
     */
    private void registerQueueAction() {
        queueService.registerQueueAdd(verticle, this);
        queueService.registerQueueDel(verticle, this);
        queueService.registerQueueVersion(verticle, this);
    }

    /**
     * Collection to json.
     *
     * @param clazz the clazz
     * @param collection the collection
     * @return the string
     */
    private String collectionToJson(String clazz, final Collection<?> collection) {
        if ("".equals(clazz)||collection==null) {
            return "{}";
        }

        String arrayName = clazz.toLowerCase()+"s";
        JsonArray jsonArray = new JsonArray();

        for (Object obj: collection) {
            if (obj instanceof IJsonable) {
                IJsonable entity = (IJsonable)obj;
                jsonArray.add(entity.toJson());
            } else {
                return "{}";
            }
        }

        return new JsonObject().putArray(arrayName, jsonArray).encodePrettily();
    }

    /**
     * Gets the backend pools.
     *
     * @return the backend pools
     */
    public EntitiesMap<BackendPool> getBackendPools() {
        return backendPools;
    }

    /**
     * Adds the backend pool.
     *
     * @param backendPool the backend pool
     */
    public void addBackendPool(JsonObject backendPool) {
        BackendPool backendPoolInstance = new BackendPool(backendPool);
        backendPools.addEntity(backendPoolInstance);
    }

    /**
     * Removes the backend pool.
     *
     * @param backendPoolId the backend pool id
     */
    public void removeBackendPool(String backendPoolId) {
        backendPools.removeEntity(new BackendPool(backendPoolId));
    }

    /**
     * Clear backend pools map.
     */
    public void clearBackendPools() {
        backendPools.clearEntities();
    }

    /**
     * Gets the backend pool by id.
     *
     * @param id the id
     * @return the backend pool by id
     */
    public BackendPool getBackendPoolById(String id) {
        return backendPools.getEntityById(id);
    }

    /**
     * Gets the backend pool json.
     *
     * @return the backend pool json
     */
    public JsonArray getBackendPoolJson() {
        return getBackendPools().getEntitiesJson();
    }

    /**
     * Gets the backend pool json.
     *
     * @param id the id
     * @return the backend pool json
     */
    public String getBackendPoolJson(String id) {
        if ("".equals(id)||id==null) {
            return new JsonObject().putArray(FARM_BACKENDPOOLS_FIELDNAME, getBackendPools().getEntitiesJson()).encodePrettily();
        }
        BackendPool backendPool = getBackendPoolById(id);
        if (backendPool!=null) {
            return backendPool.toJson().encodePrettily();
        }
        return "{}";
    }

    /**
     * Gets the virtualhost json.
     *
     * @return the virtualhost json
     */
    public JsonArray getVirtualhostJson() {
        return new JsonArray(getVirtualhostJson(""));
    }

    /**
     * Gets the virtualhost json.
     *
     * @param id the id
     * @return the virtualhost json
     */
    public String getVirtualhostJson(String id) {
        if ("".equals(id)||id==null) {
            return new JsonObject().putArray(FARM_VIRTUALHOSTS_FIELDNAME, getEntitiesJson()).encodePrettily();
        }
        Virtualhost virtualhost = getEntityById(id);
        if (virtualhost!=null) {
            return virtualhost.toJson().encodePrettily();
        }
        return "{}";
    }

    /**
     * Gets the rule json.
     *
     * @return the rule json
     */
    public JsonArray getRuleJson() {
        return new JsonArray(getRuleJson(""));
    }

    /**
     * Gets the rules.
     *
     * @return the rules
     */
    public Set<Rule> getRules() {
        Set<Rule> rules = new HashSet<>();
        for (Virtualhost virtualhost: getEntities().values()) {
            rules.addAll(virtualhost.getEntities().values());
        }
        return rules;
    }

    /**
     * Gets the rule json.
     *
     * @param id the id
     * @return the rule json
     */
    public String getRuleJson(String id) {
        if ("".equals(id)||id==null) {
            return collectionToJson(Rule.class.getSimpleName(), getRules());
        }
        for (Virtualhost virtualhost : getEntities().values()) {
            Rule rule = virtualhost.getEntityById(id);
            if (rule!=null) {
                return rule.toJson().encodePrettily();
            }
        }
        return "{}";
    }

    /**
     * Gets the backends.
     *
     * @return the backends
     */
    public Set<IBackend> getBackends() {
        Set<IBackend> backends = new HashSet<>();
        for (BackendPool backendpool: backendPools.getEntities().values()) {
            backends.addAll(backendpool.getEntities().values());
        }
        return backends;
    }

    /**
     * Gets the backend json.
     *
     * @return the backend json
     */
    public JsonArray getBackendJson() {
        return new JsonArray(getBackendJson(""));
    }

    /**
     * Gets the backend json.
     *
     * @param id the id
     * @return the backend json
     */
    public String getBackendJson(String id) {
        if ("".equals(id)||id==null) {
            return collectionToJson(Backend.class.getSimpleName(), getBackends());
        }
        for (BackendPool backendPool : getBackendPools().getEntities().values()) {
            IBackend backend = backendPool.getEntityById(id);
            if (backend!=null) {
                return backend.toJson().encodePrettily();
            }
        }
        return "{}";
    }

}
