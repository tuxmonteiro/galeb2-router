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

import com.globo.galeb.bus.ICallbackHealthcheck;
import com.globo.galeb.bus.IEventObserver;
import com.globo.galeb.bus.IQueueService;
import com.globo.galeb.bus.MessageBus;
import com.globo.galeb.bus.VertxQueueService;
import com.globo.galeb.entity.Entity;
import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.entity.IJsonable.StatusType;
import com.globo.galeb.entity.impl.Farm;
import com.globo.galeb.entity.impl.backend.BackendPool;
import com.globo.galeb.entity.impl.backend.IBackend;
import com.globo.galeb.logger.SafeLogger;
import com.globo.galeb.rulereturn.HttpCode;
import com.globo.galeb.scheduler.ISchedulerHandler;
import com.globo.galeb.scheduler.impl.VertxPeriodicScheduler;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.json.JsonObject;
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
    private SafeLogger log;

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
        log = new SafeLogger().setLogger(container.logger());
        conf = container.config();
        uriHealthCheck = conf.getString("uriHealthCheck","/"); // Recommended = "/health"
        Long checkInterval = conf.getLong("checkInterval", 5000L); // Milliseconds Interval

        queueService = new VertxQueueService(vertx.eventBus(), log);
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
            boolean running = messageBus.getEntity().getString(IJsonable.STATUS_FIELDNAME, StatusType.RUNNING_STATUS.toString())
                                                        .equals(StatusType.RUNNING_STATUS.toString());

            String backendId = messageBus.getEntityId();
            String virtualhostId = messageBus.getParentId();
            final Map <String, Set<String>> tempMap = running ? backendsMap : badBackendsMap;

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

            boolean running = messageBus.getEntity().getString(IJsonable.STATUS_FIELDNAME, StatusType.RUNNING_STATUS.toString())
                    .equals(StatusType.RUNNING_STATUS.toString());

            String backendId = messageBus.getEntityId();
            String virtualhostId = messageBus.getParentId();
            final Map <String, Set<String>> tempMap = running ? backendsMap : badBackendsMap;

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
    public void moveBackend(String backendId, boolean status) throws UnsupportedEncodingException {

        Set<String> backendpools = status ? badBackendsMap.get(backendId) : backendsMap.get(backendId);

        if (backendpools!=null) {
            Iterator<String> it = backendpools.iterator();
            while (it.hasNext()) {
                BackendPool backendPool = farm.getBackendPoolById(it.next());
                IBackend backend = !status ? backendPool.getEntityById(backendId) : backendPool.getBadBackendById(backendId);

                if (backend!=null) {
                    String uriDel = String.format("/backend/%s", URLEncoder.encode(backendId,"UTF-8"));
                    String uriAdd = "/backend";

                    ((Entity) backend).setStatus(status ? StatusType.FAILED_STATUS : StatusType.RUNNING_STATUS);
                    queueService.queueToDel(backend.toJson(), uriDel);

                    ((Entity) backend).setStatus(!status ? StatusType.FAILED_STATUS : StatusType.RUNNING_STATUS);
                    queueService.queueToAdd(backend.toJson(), uriAdd);
                }
            }
        }

    }

}
