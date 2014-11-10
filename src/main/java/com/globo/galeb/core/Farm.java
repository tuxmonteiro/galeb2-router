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
package com.globo.galeb.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import com.globo.galeb.core.bus.ICallbackQueueAction;
import com.globo.galeb.core.bus.ICallbackSharedData;
import com.globo.galeb.core.bus.MessageToMap;
import com.globo.galeb.core.bus.MessageToMapBuilder;
import com.globo.galeb.core.entity.EntitiesMap;
import com.globo.galeb.core.entity.Entity;
import com.globo.galeb.core.entity.IJsonable;
import com.globo.galeb.criteria.impl.HostHeaderCriterion;
import com.globo.galeb.verticles.RouterVerticle;

/**
 * Class Farm.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class Farm extends EntitiesMap<Virtualhost> implements ICallbackQueueAction, ICallbackSharedData {

    /** The Constant FARM_MAP. */
    public static final String FARM_MAP                    = "farm";

    /** The Constant FARM_BACKENDS_FIELDNAME. */
    public static final String FARM_BACKENDS_FIELDNAME     = "backends";

    /** The Constant FARM_VIRTUALHOSTS_FIELDNAME. */
    public static final String FARM_VIRTUALHOSTS_FIELDNAME = "virtualhosts";

    /** The Constant FARM_SHAREDDATA_ID. */
    public static final String FARM_SHAREDDATA_ID          = "farm.sharedData";

    /** The Constant FARM_VERSION_FIELDNAME. */
    public static final String FARM_VERSION_FIELDNAME      = "version";

    /** The verticle. */
    private final Verticle verticle;

    /** The version. */
    private Long version = 0L;

    /** The shared map. */
    private ConcurrentMap<String, String> sharedMap = new ConcurrentHashMap<String, String>();

    /** The backend pools. */
    private EntitiesMap<BackendPool> backendPools;


    /**
     * Instantiates a new farm.
     *
     * @param verticle the verticle
     * @param queueService the queue service
     */
    public Farm(final Verticle verticle) {
        super("farm");
        setFarm(this);
        this.verticle = verticle;
    }

    private Farm prepareSharedMap() {
        if (plataform instanceof Vertx) {
            this.sharedMap = ((Vertx)plataform).sharedData().getMap(FARM_SHAREDDATA_ID);
            this.sharedMap.put(FARM_MAP, toJson().encodePrettily());
            this.sharedMap.put(FARM_BACKENDS_FIELDNAME, "{}");
        }
        return this;
    }

    private Farm prepareBackendPools() {
        backendPools = new BackendPools("backendpools");
        backendPools.setFarm(farm)
                    .setLogger(logger)
                    .setPlataform(plataform)
                    .setQueueService(queueService)
                    .setStaticConf(staticConf);
        return this;
    }

    @Override
    public void start() {
        if (verticle!=null) {
            properties.mergeIn(verticle.getContainer().config());
        }
        prepareSharedMap();
        prepareBackendPools();
        registerQueueAction();
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

    /**
     * Gets the backends.
     *
     * @return the backends
     */
    public Set<Backend> getBackends() {
        Set<Backend> backends = new HashSet<>();
        for (Virtualhost virtualhost: getEntities().values()) {
            backends.addAll(virtualhost.getBackends(true));
            backends.addAll(virtualhost.getBackends(false));
        }
        return backends;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.Entity#toJson()
     */
    @Override
    public JsonObject toJson() {
        prepareJson();

        idObj.removeField(STATUS_FIELDNAME);
        idObj.putNumber(FARM_VERSION_FIELDNAME, version);
        JsonArray virtualhostArray = new JsonArray();

        for (String vhost : getEntities().keySet()) {
            Virtualhost virtualhost = getEntityById(vhost);
            if (virtualhost==null) {
                continue;
            }
            virtualhostArray.add(virtualhost.toJson());
        }

        idObj.putArray(FARM_VIRTUALHOSTS_FIELDNAME, virtualhostArray);
        return super.toJson();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.ICallbackQueueAction#addToMap(java.lang.String)
     */
    @Override
    public boolean addToMap(String message) {
        @SuppressWarnings("rawtypes")
        MessageToMap messageToMap = MessageToMapBuilder.getInstance(message, this);
        if (properties.containsField(Constants.CONF_STARTER_CONF)) {
            JsonObject starterConf = properties.getObject(Constants.CONF_STARTER_CONF);
            if (starterConf.containsField(Constants.CONF_ROOT_ROUTER)) {
                JsonObject staticConf = starterConf.getObject(Constants.CONF_ROOT_ROUTER);
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
        return MessageToMapBuilder.getInstance(message, this).del();
    }

    /**
     * Register queue action.
     */
    private void registerQueueAction() {
        if (queueService!=null) {
            queueService.registerQueueAdd(verticle, this);
            queueService.registerQueueDel(verticle, this);
            queueService.registerQueueVersion(verticle, this);
            queueService.registerUpdateSharedData(verticle, this);
        }
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.ICallbackSharedData#updateSharedData()
     */
    @Override
    public void updateSharedData() {
        if (verticle instanceof RouterVerticle) {
            this.sharedMap.put(FARM_MAP, toJson().encodePrettily());
            String backendClassName = Backend.class.getSimpleName().toLowerCase();
            this.sharedMap.put(FARM_BACKENDS_FIELDNAME, collectionToJson("", getBackends(), backendClassName));
        }
    }

    /**
     * Gets the farm json.
     *
     * @return the farm json
     */
    public String getFarmJson() {
        queueService.updateSharedData();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignore) {}
        return this.sharedMap.get(FARM_MAP);
    }

    /**
     * Gets the virtualhost json by virtualhostId.
     *
     * @param id the id
     * @return the virtualhost json
     */
    public String getVirtualhostJson(String id) {
        queueService.updateSharedData();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignore) {}
        return getJsonObject(id, this.sharedMap.get(FARM_MAP), Virtualhost.class.getSimpleName().toLowerCase());
    }

    /**
     * Gets the backend json by backendId.
     *
     * @param id the id
     * @return the backend json
     */
    public String getBackendJson(String id) {
        queueService.updateSharedData();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignore) {}
        return getJsonObject(id, this.sharedMap.get(FARM_BACKENDS_FIELDNAME), Backend.class.getSimpleName().toLowerCase());
    }

    /**
     * Gets the json object.
     *
     * @param key the key
     * @param jsonCollection the json collection
     * @param clazz the clazz
     * @return the json object
     */
    private String getJsonObject(String key, String jsonCollection, String clazz) {
        if ("".equals(jsonCollection)) {
            return "{}";
        }
        JsonObject json = new JsonObject(jsonCollection);
        JsonArray jsonArray = json.getArray(String.format("%ss", clazz));
        if (jsonArray!=null) {
            if ("".equals(key)) {
                return jsonArray.encodePrettily();
            }
            Iterator<Object> jsonArrayIterator = jsonArray.iterator();
            while (jsonArrayIterator.hasNext()) {
                JsonObject entity = (JsonObject)jsonArrayIterator.next();
                String id = entity.getString(IJsonable.ID_FIELDNAME);
                if (id.equals(key)) {
                    return entity.encodePrettily();
                }
            }
            return jsonArray.encodePrettily();
        }
        return "{}";
    }

    /**
     * Convert Collection to json.
     *
     * @param key the key
     * @param collection the collection
     * @param clazz the clazz
     * @return the string
     */
    public String collectionToJson(String key, Collection<? extends Entity> collection, String clazz) {
        String result = "";
        boolean isArray = false;
        JsonArray entityArray = new JsonArray();

        for (Entity entityObj: collection) {
            entityArray.add(entityObj.toJson());
            if (!"".equals(key)) {
                if (entityObj.toString().equalsIgnoreCase(key)) {
                    result = entityObj.toJson().encodePrettily();
                    break;
                }
            } else {
                isArray = true;
            }
        }
        return !isArray ? result : new JsonObject().putArray(String.format("%ss", clazz), entityArray).encodePrettily();
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

}
