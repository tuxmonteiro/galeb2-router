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
package com.globo.galeb.core.bus;

import com.globo.galeb.core.SafeJsonObject;

/**
 * Interface IQueueService.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public interface IQueueService {

    /** The Constant QUEUE_HEALTHCHECK_OK. */
    public static final String QUEUE_HEALTHCHECK_OK              = "healthcheck.ok";

    /** The Constant QUEUE_HEALTHCHECK_FAIL. */
    public static final String QUEUE_HEALTHCHECK_FAIL            = "healthcheck.fail";

    /** The Constant QUEUE_BACKEND_CONNECTIONS_PREFIX. */
    public static final String QUEUE_BACKEND_CONNECTIONS_PREFIX  = "conn_";

    /**
     * Enum ACTION.
     *
     * @author: See AUTHORS file.
     * @version: 1.0.0, Oct 23, 2014.
     */
    public enum ACTION {

        /** The add. */
        ADD         ("route.add"),

        /** The del. */
        DEL         ("route.del"),

        /** The set version. */
        SET_VERSION ("route.version"),

        /** The shared data. */
        SHARED_DATA ("route.shareddata");

        /** The queue name. */
        private String queue;

        /**
         * Instantiates a new action.
         *
         * @param queue the queue name
         */
        private ACTION(final String queue) {
            this.queue = queue;
        }

        /* (non-Javadoc)
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return queue;
        }
    }

    /**
     * Queue to add.
     *
     * @param json the json
     * @param uri the uri
     */
    public void queueToAdd(SafeJsonObject json, String uri);

    /**
     * Queue to del.
     *
     * @param json the json
     * @param uri the uri
     */
    public void queueToDel(SafeJsonObject json, String uri);

    /**
     * Queue to change.
     *
     * @param json the json
     * @param uri the uri
     */
    public void queueToChange(SafeJsonObject json, String uri);

    /**
     * Register healthcheck.
     *
     * @param callbackHealthcheck the callback healthcheck
     */
    public void registerHealthcheck(ICallbackHealthcheck callbackHealthcheck);

    /**
     * Publish backendOk.
     *
     * @param backend the backend
     */
    public void publishBackendOk(String backend);

    /**
     * Publish backendFail.
     *
     * @param backend the backend
     */
    public void publishBackendFail(String backend);

    /**
     * Publish backend connections.
     *
     * @param queueActiveConnections the queue active connections
     * @param myConnections the local connections
     */
    public void publishBackendConnections(String queueActiveConnections,
            SafeJsonObject myConnections);

    /**
     * Register connections counter.
     *
     * @param connectionsCounter the connections counter
     * @param queueActiveConnections the queue active connections
     */
    public void registerConnectionsCounter(
            ICallbackConnectionCounter connectionsCounter,
            String queueActiveConnections);

    /**
     * Publish active connections.
     *
     * @param queueActiveConnections the queue active connections
     * @param myConnections the local connections
     */
    public void publishActiveConnections(String queueActiveConnections,
            SafeJsonObject myConnections);

    /**
     * Unregister connections counter.
     *
     * @param connectionsCounter the connections counter
     * @param queueActiveConnections the queue active connections
     */
    public void unregisterConnectionsCounter(
            ICallbackConnectionCounter connectionsCounter,
            String queueActiveConnections);

    /**
     * Register queue to add.
     *
     * @param starter the starter
     * @param callbackQueueAction the callback queue action
     */
    public void registerQueueAdd(Object starter,
            ICallbackQueueAction callbackQueueAction);

    /**
     * Register queue to del.
     *
     * @param starter the starter
     * @param callbackQueueAction the callback queue action
     */
    public void registerQueueDel(Object starter,
            ICallbackQueueAction callbackQueueAction);

    /**
     * Register queue to version.
     *
     * @param starter the starter
     * @param callbackQueueAction the callback queue action
     */
    public void registerQueueVersion(Object starter,
            ICallbackQueueAction callbackQueueAction);

    /**
     * Register update shared data.
     *
     * @param starter the starter
     * @param callbackSharedData the callback shared data
     */
    public void registerUpdateSharedData(Object starter,
            ICallbackSharedData callbackSharedData);

    /**
     * Update shared data.
     */
    public void updateSharedData();

}
