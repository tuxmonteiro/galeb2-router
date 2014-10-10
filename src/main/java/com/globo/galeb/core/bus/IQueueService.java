/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
 * PARTICULAR PURPOSE.
 */
package com.globo.galeb.core.bus;

import com.globo.galeb.core.SafeJsonObject;

public interface IQueueService {

    public static final String QUEUE_HEALTHCHECK_OK              = "healthcheck.ok";
    public static final String QUEUE_HEALTHCHECK_FAIL            = "healthcheck.fail";
    public static final String QUEUE_BACKEND_CONNECTIONS_PREFIX  = "conn_";

    public enum ACTION {
        ADD         ("route.add"),
        DEL         ("route.del"),
        SET_VERSION ("route.version"),
        SHARED_DATA ("route.shareddata");

        private String queue;
        private ACTION(final String queue) {
            this.queue = queue;
        }
        @Override
        public String toString() {
            return queue;
        }
    }

    public void queueToAdd(SafeJsonObject json, String uri);

    public void queueToDel(SafeJsonObject json, String uri);

    public void queueToChange(SafeJsonObject json, String uri);

    public void registerHealthcheck(ICallbackHealthcheck callbackHealthcheck);

    public void publishBackendOk(String backend);

    public void publishBackendFail(String backend);

    public void publishBackendConnections(String queueActiveConnections,
            SafeJsonObject myConnections);

    public void registerConnectionsCounter(
            ICallbackConnectionCounter connectionsCounter,
            String queueActiveConnections);

    public void publishActiveConnections(String queueActiveConnections,
            SafeJsonObject myConnections);

    public void unregisterConnectionsCounter(
            ICallbackConnectionCounter connectionsCounter,
            String queueActiveConnections);

    public void registerQueueAdd(Object starter,
            ICallbackQueueAction callbackQueueAction);

    public void registerQueueDel(Object starter,
            ICallbackQueueAction callbackQueueAction);

    public void registerQueueVersion(Object starter,
            ICallbackQueueAction callbackQueueAction);

    public void registerUpdateSharedData(Object starter,
            ICallbackSharedData callbackSharedData);

    public void updateSharedData();

    public void notifyBackendFail(String backendId);

}
