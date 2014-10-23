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
package com.globo.galeb.metrics;

import org.vertx.java.core.datagram.DatagramSocket;
import org.vertx.java.core.logging.Logger;

/**
 * Statsd Client Vert.X implementation
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class StatsdClient {

    /** The Constant PATTERN_COUNT. */
    private static final String PATTERN_COUNT = "%s:%s|c";

    /** The Constant PATTERN_TIME. */
    private static final String PATTERN_TIME  = "%s:%s|ms";

    /** The Constant PATTERN_GAUGE. */
    private static final String PATTERN_GAUGE = "%s:%s|g";

    /** The Constant PATTERN_SET. */
    private static final String PATTERN_SET   = "%s:%s|s";

    /**
     * Enum TypeStatsdMessage.
     *
     * @author: See AUTHORS file.
     * @version: 1.0.0, Oct 23, 2014.
     */
    public static enum TypeStatsdMessage {

        /** The type count. */
        COUNT(PATTERN_COUNT),

        /** The type time. */
        TIME(PATTERN_TIME),

        /** The type gauge. */
        GAUGE(PATTERN_GAUGE),

        /** The type set. */
        SET(PATTERN_SET);

        /** The pattern. */
        private final String pattern;

        /**
         * Instantiates a new type statsd message.
         *
         * @param pattern the pattern
         */
        private TypeStatsdMessage(String pattern) {
            this.pattern = pattern;
        }

        /**
         * Gets the pattern.
         *
         * @return the pattern
         */
        public String getPattern() {
            return this.pattern;
        }
    }

    /** Statsd server host. */
    private String statsDhost;

    /** Statsd server port. */
    private Integer statsDPort;

    /** The statsd prefix. */
    private String prefix;

    /** The logger. */
    private final Logger log;

    /** The DatagramSocket socket. */
    private final DatagramSocket socket;

    /**
     * Instantiates a new statsd client.
     *
     * @param statsDhost the stats dhost
     * @param statsDPort the stats d port
     * @param prefix the prefix
     * @param socket the socket
     * @param log the log
     */
    public StatsdClient(String statsDhost, Integer statsDPort, String prefix,
                        final DatagramSocket socket, final Logger log) {
        this.statsDhost = statsDhost;
        this.statsDPort = statsDPort;
        this.prefix = "".equals(prefix) ? "stats" : prefix;
        this.log = log;
        this.socket = socket;
    }

    /**
     * Instantiates a new statsd client.
     *
     * @param socket the socket
     * @param log the log
     */
    public StatsdClient(final DatagramSocket socket, final Logger log) {
        this("localhost", 8125, "", socket, log);
    }

    /**
     * Send message to statsd
     *
     * @param type the type
     * @param message the message
     */
    public void send(final TypeStatsdMessage type, String message) {
        String[] data = message.split(":");
        String key = data[0];
        String value = data[1];
        try {
            String id = String.format("".equals(prefix) ? "%s%s": "%s.%s", prefix, key);
            socket.send(String.format(type.getPattern(), id, value), statsDhost, statsDPort, null);
        } catch (io.netty.channel.ChannelException e) {
            log.error("io.netty.channel.ChannelException: Failed to open a socket.");
        } catch (RuntimeException e) {
            log.error(e.getMessage());
        }
    }
}
