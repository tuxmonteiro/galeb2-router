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
 * Interface ICounter.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public interface ICounter {

    /**
     * http code count.
     *
     * @param key the key
     * @param code the code
     */
    public abstract void httpCode(String key, Integer code);

    /**
     * http code count.
     *
     * @param virtualhostId the virtualhost id
     * @param backendId the backend id
     * @param code the code
     */
    public abstract void httpCode(String virtualhostId, String backendId, Integer code);

    /**
     * Increment http code count.
     *
     * @param key the key
     * @param code the code
     */
    public abstract void incrHttpCode(String key, Integer code);

    /**
     * Increment http code count.
     *
     * @param key the key
     * @param code the code
     * @param sample the sample
     */
    public abstract void incrHttpCode(String key, Integer code, double sample);

    /**
     * Decrement http code count.
     *
     * @param key the key
     * @param code the code
     */
    public abstract void decrHttpCode(String key, Integer code);

    /**
     * Decrement http code count.
     *
     * @param key the key
     * @param code the code
     * @param sample the sample
     */
    public abstract void decrHttpCode(String key, Integer code, double sample);

    /**
     * Request time count.
     *
     * @param key the key
     * @param initialRequestTime the initial request time
     */
    public abstract void requestTime(String key, Long initialRequestTime);

    /**
     * Request time count.
     *
     * @param virtualhostId the virtualhost id
     * @param backendId the backend id
     * @param initialRequestTime the initial request time
     */
    public abstract void requestTime(String virtualhostId, String backendId, Long initialRequestTime);

    /**
     * Send active sessions.
     *
     * @param key the key
     * @param initialRequestTime the initial request time
     */
    public abstract void sendActiveSessions(String key, Long initialRequestTime);

    /**
     * Send active sessions.
     *
     * @param virtualhostId the virtualhost id
     * @param backendId the backend id
     * @param initialRequestTime the initial request time
     */
    public abstract void sendActiveSessions(String virtualhostId, String backendId, Long initialRequestTime);

}
