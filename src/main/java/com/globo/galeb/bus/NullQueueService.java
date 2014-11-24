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
package com.globo.galeb.bus;

import org.vertx.java.core.json.JsonObject;

/**
 * Class NullQueueService.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 18, 2014.
 */
public class NullQueueService implements IQueueService {

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#queueToAdd(org.vertx.java.core.json.JsonObject, java.lang.String)
     */
    @Override
    public void queueToAdd(JsonObject json, String uri) {
        //
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#queueToDel(org.vertx.java.core.json.JsonObject, java.lang.String)
     */
    @Override
    public void queueToDel(JsonObject json, String uri) {
        //
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#queueToChange(org.vertx.java.core.json.JsonObject, java.lang.String)
     */
    @Override
    public void queueToChange(JsonObject json, String uri) {
        //
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#registerHealthcheck(com.globo.galeb.core.bus.ICallbackHealthcheck)
     */
    @Override
    public void registerHealthcheck(ICallbackHealthcheck callbackHealthcheck) {
        //
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#publishBackendOk(java.lang.String)
     */
    @Override
    public void publishBackendOk(String backend) {
        //
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#publishBackendFail(java.lang.String)
     */
    @Override
    public void publishBackendFail(String backend) {
        //
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#publishBackendConnections(java.lang.String, org.vertx.java.core.json.JsonObject)
     */
    @Override
    public void publishBackendConnections(String queueActiveConnections,
            JsonObject myConnections) {
        //
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#registerConnectionsCounter(com.globo.galeb.core.bus.ICallbackConnectionCounter, java.lang.String)
     */
    @Override
    public void registerConnectionsCounter(
            ICallbackConnectionCounter connectionsCounter,
            String queueActiveConnections) {
        //
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#publishActiveConnections(java.lang.String, org.vertx.java.core.json.JsonObject)
     */
    @Override
    public void publishActiveConnections(String queueActiveConnections,
            JsonObject myConnections) {
        //
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#unregisterConnectionsCounter(com.globo.galeb.core.bus.ICallbackConnectionCounter, java.lang.String)
     */
    @Override
    public void unregisterConnectionsCounter(
            ICallbackConnectionCounter connectionsCounter,
            String queueActiveConnections) {
        //
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#registerQueueAdd(java.lang.Object, com.globo.galeb.core.bus.ICallbackQueueAction)
     */
    @Override
    public void registerQueueAdd(Object starter,
            ICallbackQueueAction callbackQueueAction) {
        //
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#registerQueueDel(java.lang.Object, com.globo.galeb.core.bus.ICallbackQueueAction)
     */
    @Override
    public void registerQueueDel(Object starter,
            ICallbackQueueAction callbackQueueAction) {
        //
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.bus.IQueueService#registerQueueVersion(java.lang.Object, com.globo.galeb.core.bus.ICallbackQueueAction)
     */
    @Override
    public void registerQueueVersion(Object starter,
            ICallbackQueueAction callbackQueueAction) {
        //
    }

}
