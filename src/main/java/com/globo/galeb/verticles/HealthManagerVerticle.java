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
package com.globo.galeb.verticles;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.globo.galeb.core.Backend;
import com.globo.galeb.core.Farm;
import com.globo.galeb.core.HttpCode;
import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.core.bus.ICallbackHealthcheck;
import com.globo.galeb.core.bus.IEventObserver;
import com.globo.galeb.core.bus.IQueueService;
import com.globo.galeb.core.bus.MessageBus;
import com.globo.galeb.core.bus.VertxQueueService;
import com.globo.galeb.scheduler.ISchedulerHandler;
import com.globo.galeb.scheduler.impl.VertxPeriodicScheduler;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 * Class HealthManagerVerticle.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class HealthManagerVerticle extends Verticle implements IEventObserver, ICallbackHealthcheck {

    /** The backends map. */
    private final Map<String, Set<String>> backendsMap = new HashMap<>();

    /** The bad backends map. */
    private final Map<String, Set<String>> badBackendsMap = new HashMap<>();

    /** The http header host. */
    private final String httpHeaderHost = HttpHeaders.HOST.toString();

    /** The farm. */
    private Farm farm;

    /** The queue service. */
    private IQueueService queueService;

    /** The Json conf. */
    private JsonObject conf;

    /** The logger. */
    private Logger log;

    /** The uri health check. */
    private String uriHealthCheck;

    /**
     * Class CheckBadBackendTaskHandler.
     *
     * @author: See AUTHORS file.
     * @version: 1.0.0, Oct 23, 2014.
     */
    private class CheckBadBackendTaskHandler implements ISchedulerHandler {

        /* (non-Javadoc)
         * @see org.vertx.java.core.Handler#handle(java.lang.Object)
         */
        @Override
        public void handle() {
            log.info("Checking bad backends...");
            if (badBackendsMap!=null) {
                Iterator<String> it = badBackendsMap.keySet().iterator();
                while (it.hasNext()) {
                    final String backend = it.next();
                    String[] hostWithPort = backend.split(":");
                    String host = hostWithPort[0];
                    Integer port = Integer.parseInt(hostWithPort[1]);
                    try {
                        HttpClient client = vertx.createHttpClient()
                            .setHost(host)
                            .setPort(port)
                            .exceptionHandler(new Handler<Throwable>() {
                                @Override
                                public void handle(Throwable event) {}
                            });
                        HttpClientRequest cReq = client.get(uriHealthCheck, new Handler<HttpClientResponse>() {
                                @Override
                                public void handle(HttpClientResponse cResp) {
                                    if (cResp!=null && cResp.statusCode()==HttpCode.OK) {
                                        queueService.publishBackendOk(backend);
                                    }
                                }
                            });
                        cReq.headers().set(httpHeaderHost, (String) badBackendsMap.get(backend).toArray()[0]);
                        cReq.exceptionHandler(new Handler<Throwable>() {
                            @Override
                            public void handle(Throwable event) {}
                        });
                        cReq.end();
                    } catch (RuntimeException e) {
                        log.error(e.getMessage());
                    }
                }
            }

        }

    }

    /* (non-Javadoc)
     * @see org.vertx.java.platform.Verticle#start()
     */
    @Override
    public void start() {
        log = container.logger();
        conf = container.config();
        uriHealthCheck = conf.getString("uriHealthCheck","/"); // Recommended = "/health"
        Long checkInterval = conf.getLong("checkInterval", 5000L); // Milliseconds Interval

        queueService = new VertxQueueService(vertx.eventBus(),container.logger());
        queueService.registerHealthcheck(this);
        farm = new Farm(this);
        farm.setLogger(log)
            .setPlataform(vertx)
            .setQueueService(queueService)
            .setStaticConf(conf)
            .start();

        new VertxPeriodicScheduler(vertx)
                .setPeriod(checkInterval).setHandler(new CheckBadBackendTaskHandler()).start();

        log.info(String.format("Instance %s started", this.toString()));
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IEventObserver#setVersion(java.lang.Long)
     */
    @Override
    public void setVersion(Long version) {}

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IEventObserver#postAddEvent(java.lang.String)
     */
    @Override
    public void postAddEvent(String message) {

        MessageBus messageBus = new MessageBus(message);
        if ("backend".equals(messageBus.getUriBase())) {
            boolean backendStatus = messageBus.getEntity().getBoolean(Backend.ELEGIBLE_FIELDNAME, true);
            String backendId = messageBus.getEntityId();
            String virtualhostId = messageBus.getParentId();
            final Map <String, Set<String>> tempMap = backendStatus ? backendsMap : badBackendsMap;

            if (!tempMap.containsKey(backendId)) {
                tempMap.put(backendId, new HashSet<String>());
            }
            Set<String> virtualhosts = tempMap.get(backendId);
            virtualhosts.add(virtualhostId);
        }
    };

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IEventObserver#postDelEvent(java.lang.String)
     */
    @Override
    public void postDelEvent(String message) {
        MessageBus messageBus = new MessageBus(message);
        if ("backend".equals(messageBus.getUriBase())) {

            boolean backendStatus = messageBus.getEntity().getBoolean(Backend.ELEGIBLE_FIELDNAME, true);
            String backendId = messageBus.getEntityId();
            String virtualhostId = messageBus.getParentId();
            final Map <String, Set<String>> tempMap = backendStatus ? backendsMap : badBackendsMap;

            if (tempMap.containsKey(backendId)) {
                Set<String> virtualhosts = tempMap.get(backendId);
                virtualhosts.remove(virtualhostId);
                if (virtualhosts.isEmpty()) {
                    tempMap.remove(backendId);
                }
            }
        }
    };

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.ICallbackHealthcheck#moveBackend(java.lang.String, boolean)
     */
    @Override
    public void moveBackend(String backend, boolean elegible) throws UnsupportedEncodingException {

        Set<String> virtualhosts = elegible ? badBackendsMap.get(backend) : backendsMap.get(backend);

        if (virtualhosts!=null) {
            Iterator<String> it = virtualhosts.iterator();
            while (it.hasNext()) {
                Virtualhost virtualhost = farm.getEntityById(it.next());

                JsonObject backendJson = null;
                if (virtualhost!=null) {

                    for (Backend backendSearched: virtualhost.getBackends(!elegible)) {
                        if (backend.equals(backendSearched.toString())) {
                            backendJson = backendSearched.toJson();
                            break;
                        }
                    }

                    if (backendJson!=null) {
                        String uriDel = String.format("/backend/%s", URLEncoder.encode(backend,"UTF-8"));
                        String uriAdd = "/backend";

                        backendJson.putBoolean(Backend.ELEGIBLE_FIELDNAME, !elegible);
                        queueService.queueToDel(backendJson, uriDel);

                        backendJson.putBoolean(Backend.ELEGIBLE_FIELDNAME, elegible);
                        queueService.queueToAdd(backendJson, uriAdd);
                    }

                }
            }
        }
    }

}
