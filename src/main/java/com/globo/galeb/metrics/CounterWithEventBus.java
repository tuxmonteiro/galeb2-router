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

import static com.globo.galeb.verticles.StatsdVerticle.QUEUE_COUNTER;
import static com.globo.galeb.verticles.StatsdVerticle.QUEUE_TIMER;

import org.vertx.java.core.eventbus.EventBus;

/**
 * Class CounterWithEventBus.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 4, 2014.
 */
public class CounterWithEventBus implements ICounter {

    /** The EventBus. */
    private EventBus eb;

    /**
     * Cleanup the key.
     *
     * @param aString the a string
     * @param strDefault the str default
     * @return the string
     */
    private String cleanupString(String aString, String strDefault) {
        return !"".equals(aString)?aString.replaceAll("[^\\w]", "_"):strDefault;
    }

    /**
     * Merge virtualhost id with backend id.
     *
     * @param virtualhostId the virtualhost id
     * @param backendId the backend id
     * @return the string merged
     */
    private String mergeVirtualhostIdWithBackendId(String virtualhostId, String backendId) {
        return String.format("%s.%s",cleanupString(virtualhostId,"UNDEF"), cleanupString(backendId, "UNDEF"));
    }

    /**
     * Instantiates a new counter with statsd.
     *
     * @param eb the EventBus
     */
    public CounterWithEventBus(final EventBus eb) {
        this.eb = eb;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.metrics.ICounter#httpCode(java.lang.String, java.lang.Integer)
     */
    @Override
    public void httpCode(String key, Integer code) {
        if (key!=null && !("".equals(key))) {
            eb.send(QUEUE_TIMER, String.format("%s.httpCode%d:%d", key, code, 1));
        }
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.metrics.ICounter#httpCode(java.lang.String, java.lang.String, java.lang.Integer)
     */
    @Override
    public void httpCode(String virtualhostId, String backendId, Integer code) {
        httpCode(mergeVirtualhostIdWithBackendId(virtualhostId, backendId), code);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.metrics.ICounter#incrHttpCode(java.lang.String, java.lang.Integer)
     */
    @Override
    public void incrHttpCode(String key, Integer code) {
        incrHttpCode(key, code, 1.0);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.metrics.ICounter#incrHttpCode(java.lang.String, java.lang.Integer, double)
     */
    @Override
    public void incrHttpCode(String key, Integer code, double sample) {
        String srtSample = sample > 0.0 && sample < 1.0 ? String.format("|@%f", sample) : "";
        if (key!=null && !("".equals(key))) {
            eb.send(QUEUE_COUNTER, String.format("%s.httpCode%d:%d%s", key, code, 1, srtSample));
        }
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.metrics.ICounter#decrHttpCode(java.lang.String, java.lang.Integer)
     */
    @Override
    public void decrHttpCode(String key, Integer code) {
        decrHttpCode(key, code, 1.0);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.metrics.ICounter#decrHttpCode(java.lang.String, java.lang.Integer, double)
     */
    @Override
    public void decrHttpCode(String key, Integer code, double sample) {
        String srtSample = sample > 0.0 && sample < 1.0 ? String.format("|@%f", sample) : "";
        if (key!=null && !("".equals(key))) {
            eb.send(QUEUE_COUNTER, String.format("%s.httpCode%d:%d%s", key, code, -1, srtSample));
        }
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.metrics.ICounter#requestTime(java.lang.String, java.lang.Long)
     */
    @Override
    public void requestTime(String key, final Long initialRequestTime) {
        Long requestTime = System.currentTimeMillis() - initialRequestTime;
        if (key!=null && !("".equals(key))) {
            eb.send(QUEUE_TIMER, String.format("%s.requestTime:%d", key, requestTime));
        }
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.metrics.ICounter#requestTime(java.lang.String, java.lang.String, java.lang.Long)
     */
    @Override
    public void requestTime(String virtualhostId, String backendId,
            Long initialRequestTime) {
        requestTime(mergeVirtualhostIdWithBackendId(virtualhostId, backendId), initialRequestTime);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.metrics.ICounter#sendActiveSessions(java.lang.String, java.lang.Long)
     */
    @Override
    public void sendActiveSessions(String key, Long initialRequestTime) {
        if (key!=null && !("".equals(key))) {
            eb.send(QUEUE_COUNTER, String.format("%s.active:%d", key, 1));
        }
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.metrics.ICounter#sendActiveSessions(java.lang.String, java.lang.String, java.lang.Long)
     */
    @Override
    public void sendActiveSessions(String virtualhostId, String backendId, Long initialRequestTime) {
        sendActiveSessions(mergeVirtualhostIdWithBackendId(virtualhostId, backendId), initialRequestTime);
    }
}
