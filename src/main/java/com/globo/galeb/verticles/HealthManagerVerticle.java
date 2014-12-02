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
import java.util.HashSet;
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
import com.globo.galeb.entity.impl.backend.BackendWithoutSessionPool;
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
    private final Set<Entity> backendsMap = new HashSet<>();

    /** The bad backends map. */
    private final Set<Entity> badBackendsMap = new HashSet<>();

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
                for (final Entity backend : badBackendsMap) {
                    final BackendPool backendPool = farm.getBackendPoolById(backend .getParentId());
                    String[] hostWithPort = backend.toString().split(":");
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
                        HttpClientRequest cReq = client.get(backendPool.getHealthCheck(), new Handler<HttpClientResponse>() {
                                @Override
                                public void handle(HttpClientResponse cResp) {
                                    if (cResp!=null && cResp.statusCode()==HttpCode.OK) {
                                        queueService.publishBackendOk(backend.toJson());
                                    }
                                }
                            });
                        cReq.headers().set(httpHeaderHost, backend.toString());
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

            final Set<Entity> tempSet = running ? backendsMap : badBackendsMap;
            tempSet.add(new BackendWithoutSessionPool(messageBus.getEntity()));
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

            final Set<Entity> tempSet = running ? backendsMap : badBackendsMap;
            tempSet.remove(new BackendWithoutSessionPool(messageBus.getEntity()));
        }
    };

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.ICallbackHealthcheck#moveBackend(java.lang.String, boolean)
     */
    @Override
    public void moveBackend(JsonObject backend, boolean status) throws UnsupportedEncodingException {

        String backendId = backend.getString(IJsonable.ID_FIELDNAME);
        if (backend!=null) {
            String uriDel = String.format("/backend/%s", URLEncoder.encode(backendId,"UTF-8"));
            String uriAdd = "/backend";

            backend.putString(IJsonable.STATUS_FIELDNAME, status ? StatusType.FAILED_STATUS.toString() : StatusType.RUNNING_STATUS.toString());
            queueService.queueToDel(backend, uriDel);

            backend.putString(IJsonable.STATUS_FIELDNAME, !status ? StatusType.FAILED_STATUS.toString() : StatusType.RUNNING_STATUS.toString());
            queueService.queueToAdd(backend, uriAdd);
        }
    }

}
