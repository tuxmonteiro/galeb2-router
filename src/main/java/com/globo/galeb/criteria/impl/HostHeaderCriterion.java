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
package com.globo.galeb.criteria.impl;

import java.util.Map;

//import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.criteria.ICriterion;
import com.globo.galeb.logger.SafeLogger;

import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServerRequest;

/**
 * Class HostHeaderCriterion.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 7, 2014.
 * @param <T> the generic type
 */
public class HostHeaderCriterion<T> implements ICriterion<T> {

    /** The log. */
    private SafeLogger log = null;

    /** The host. */
    private String host = "";

    /** The map. */
    private Map<String, T> map = null;

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#setLog(org.vertx.java.core.logging.Logger)
     */
    @Override
    public ICriterion<T> setLog(final SafeLogger logger) {
        log = logger;
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#given(java.util.Map)
     */
    @Override
    public ICriterion<T> given(final Map<String, T> map) {
        this.map = map;
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#when(java.lang.Object)
     */
    @Override
    public ICriterion<T> when(final Object param) {
        if (param instanceof HttpServerRequest) {
            host = new RequestMatch((HttpServerRequest)param).getHeader(HttpHeaders.HOST.toString());
        } else {
            if (log!=null){
                log.warn(String.format("Param is instance of %s.class. Expected %s.class",
                        param.getClass().getSimpleName(), HttpServerRequest.class.getSimpleName()));
            }
        }
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#thenGetResult()
     */
    @Override
    public T thenGetResult() {
        if ("".equals(host)) {
            log.warn("Host UNDEF");
            return null;
        }
        String hostWithoutPort = host.split(":")[0];
        if (!map.containsKey(hostWithoutPort)) {
            log.warn(String.format("Host: %s UNDEF", hostWithoutPort));
            return null;
        }

        return map.get(hostWithoutPort);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#action(com.globo.galeb.criteria.ICriterion.CriterionAction)
     */
    @Override
    public ICriterion<T> action(ICriterion.CriterionAction criterionAction) {
        return this;
    }
}
