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


/**
 * Class Constants.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class ConfVerticleDictionary {

    /**
     * Instantiates a new constants.
     */
    private ConfVerticleDictionary() {
    }

    /** The json attribute name used in configuration: CONF_ROOT_ROUTER. */
    public static final String CONF_ROOT_ROUTER          = "router";

    /** The json attribute name used in configuration: CONF_ROOT_ROUTEMANAGER. */
    public static final String CONF_ROOT_ROUTEMANAGER    = "routemanager";

    /** The json attribute name used in configuration: CONF_ROOT_HEALTHMANAGER. */
    public static final String CONF_ROOT_HEALTHMANAGER   = "healthmanager";

    /** The json attribute name used in configuration: CONF_ROOT_STATSD. */
    public static final String CONF_ROOT_STATSD          = "statsd";

    /** The json attribute name used in configuration: CONF_INSTANCES. */
    public static final String CONF_INSTANCES            = "instances";

    /** The json attribute name used in configuration: CONF_HOST. */
    public static final String CONF_HOST                 = "host";

    /** The json attribute name used in configuration: CONF_PORT. */
    public static final String CONF_PORT                 = "port";

    /** The json attribute name used in configuration: CONF_PREFIX. */
    public static final String CONF_PREFIX               = "prefix";

    /** The json attribute name used in configuration: CONF_ENABLE_ACCESSLOG. */
    public static final String CONF_ENABLE_ACCESSLOG     = "enableAccessLog";

    /** The json attribute name used in configuration: CONF_STATSD_ENABLE. */
    public static final String CONF_STATSD_ENABLE        = "enableStatsd";

    /** The json attribute name used in configuration: CONF_STATSD_HOST. */
    public static final String CONF_STATSD_HOST          = "statsdHost";

    /** The json attribute name used in configuration: CONF_STATSD_PORT. */
    public static final String CONF_STATSD_PORT          = "statsdPort";

    /** The json attribute name used in configuration: CONF_STATSD_PREFIX. */
    public static final String CONF_STATSD_PREFIX        = "statsdPrefix";

    /** The json attribute name used in configuration: CONF_STARTER_CONF. */
    public static final String CONF_STARTER_CONF         = "_starterConf";


}
