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
package com.globo.galeb.core;

public class Constants {

    private Constants() {
    }

    // Vert.x defaults (org.vertx.java.core.net.impl.SocketDefaults)
    public static final boolean tcpNoDelay           = true;
    public static final int     tcpSendBufferSize    = 8 * 1024;
    public static final int     tcpReceiveBufferSize = 32 * 1024;

    public static final String CONF_ROOT_ROUTER                  = "router";
    public static final String CONF_ROOT_ROUTEMANAGER            = "routemanager";
    public static final String CONF_ROOT_HEALTHMANAGER           = "healthmanager";
    public static final String CONF_ROOT_STATSD                  = "statsd";

    public static final String CONF_INSTANCES                    = "instances";
    public static final String CONF_HOST                         = "host";
    public static final String CONF_PORT                         = "port";
    public static final String CONF_PREFIX                       = "prefix";
    public static final String CONF_ENABLE_ACCESSLOG             = "enableAccessLog";
    public static final String CONF_STATSD_ENABLE                = "enableStatsd";
    public static final String CONF_STATSD_HOST                  = "statsdHost";
    public static final String CONF_STATSD_PORT                  = "statsdPort";
    public static final String CONF_STATSD_PREFIX                = "statsdPrefix";

    public static final String CONF_STARTER_CONF                 = "_starterConf";


}
