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

/**
 * Class CounterConsoleOut: metrics console output
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class CounterConsoleOut implements ICounter {

    /* (non-Javadoc)
     * @see com.globo.galeb.metrics.ICounter#httpCode(java.lang.String, java.lang.Integer)
     */
    @Override
    public void httpCode(String key, Integer code) {
        System.out.println(String.format("%s.httpCode%d:%d", key, code, 1));
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.metrics.ICounter#httpCode(java.lang.String, java.lang.String, java.lang.Integer)
     */
    @Override
    public void httpCode(String serverHost, String backendId, Integer code) {
        httpCode(String.format("%s.%s", serverHost, backendId), code);
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
        System.out.println(String.format("%s.httpCode%d:%d%s", key, code, 1, srtSample));
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
        System.out.println(String.format("%s.httpCode%d:%d%s", key, code, -1, srtSample));
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.metrics.ICounter#requestTime(java.lang.String, java.lang.Long)
     */
    @Override
    public void requestTime(String key, Long initialRequestTime) {
        Long requestTime = System.currentTimeMillis() - initialRequestTime;
        System.out.println(String.format("%s.requestTime:%d", key, requestTime));
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.metrics.ICounter#requestTime(java.lang.String, java.lang.String, java.lang.Long)
     */
    @Override
    public void requestTime(String serverHost, String backendId,
            Long initialRequestTime) {
        requestTime(String.format("%s.%s", serverHost, backendId), initialRequestTime);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.metrics.ICounter#sendActiveSessions(java.lang.String, java.lang.Long)
     */
    @Override
    public void sendActiveSessions(String key, Long initialRequestTime) {
        System.out.println(String.format("%s.active:%d", key, 1));
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.metrics.ICounter#sendActiveSessions(java.lang.String, java.lang.String, java.lang.Long)
     */
    @Override
    public void sendActiveSessions(String serverHost, String backendId,
            Long initialRequestTime) {
        sendActiveSessions(String.format("%s.%s", serverHost, backendId), initialRequestTime);
    }

}
