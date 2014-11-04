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

import static org.vertx.java.core.datagram.InternetProtocolFamily.IPv4;
import static com.globo.galeb.metrics.StatsdClient.TypeStatsdMessage;
import static com.globo.galeb.core.Constants.CONF_HOST;
import static com.globo.galeb.core.Constants.CONF_PORT;
import static com.globo.galeb.core.Constants.CONF_PREFIX;

import com.globo.galeb.metrics.StatsdClient;

import org.vertx.java.core.Handler;
import org.vertx.java.core.datagram.DatagramSocket;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

/**
 * Class StatsdVerticle: Statsd client verticle implementation
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class StatsdVerticle extends Verticle {

    /** The Constant QUEUE_COUNTER. */
    public final static String QUEUE_COUNTER = "statsd.counter";

    /** The Constant QUEUE_TIMER. */
    public final static String QUEUE_TIMER   = "statsd.timer";

    /** The Constant QUEUE_GAUGE. */
    public final static String QUEUE_GAUGE   = "statsd.gauge";

    /** The Constant QUEUE_SET. */
    public final static String QUEUE_SET     = "statsd.set";

    /** The statsd client instance. */
    private StatsdClient statsdClient;

    /** The statsd host. */
    private String statsDhost;

    /** The statsd port. */
    private Integer statsDPort;

    /** The statsd prefix. */
    private String prefix;

    /* (non-Javadoc)
     * @see org.vertx.java.platform.Verticle#start()
     */
    @Override
    public void start() {

        final Logger log = container.logger();
        final JsonObject conf = container.config();
        this.prefix = conf.getString(CONF_PREFIX, "stats.");
        this.statsDhost = conf.getString(CONF_HOST, "localhost");
        this.statsDPort = conf.getInteger(CONF_PORT, 8125);
        final DatagramSocket dgram = vertx.createDatagramSocket(IPv4).setReuseAddress(true);
        statsdClient = new StatsdClient(statsDhost, statsDPort, prefix, dgram, container.logger());

        final EventBus eb = vertx.eventBus();

        /*
         * Receive from EventBus. Format => tag:num
         */
        eb.registerLocalHandler(QUEUE_COUNTER, getHandler(TypeStatsdMessage.COUNT));
        eb.registerLocalHandler(QUEUE_TIMER, getHandler(TypeStatsdMessage.TIME));
        eb.registerLocalHandler(QUEUE_GAUGE, getHandler(TypeStatsdMessage.GAUGE));
        eb.registerLocalHandler(QUEUE_SET, getHandler(TypeStatsdMessage.SET));

        log.info(String.format("Instance %s started", this.toString()));

    }

    /**
     * Gets the handler.
     *
     * @param type the type
     * @return the handler
     */
    private Handler<Message<String>> getHandler(final StatsdClient.TypeStatsdMessage type) {
        return new Handler<Message<String>>() {
            @Override
            public void handle(Message<String> message) {
                statsdClient.send(type, message.body());
            }
        };
    }
}
