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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.globo.galeb.core.Backend;
import com.globo.galeb.criteria.ICriterion;
import com.globo.galeb.logger.SafeLogger;

import org.vertx.java.core.logging.Logger;

/**
 * Class BackendLeastConnCriterion.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 7, 2014.
 * @param <T> the generic type
 */
public class BackendLeastConnCriterion<T> implements ICriterion<T> {

    /** The log. */
    private final SafeLogger log            = new SafeLogger();

    /** The backends. */
    private List<T>          backends       = new ArrayList<T>();

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#setLog(org.vertx.java.core.logging.Logger)
     */
    @Override
    public ICriterion<T> setLog(final Logger logger) {
        log.setLogger(logger);
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#given(java.util.Map)
     */
    @Override
    public ICriterion<T> given(final Map<String, T> map) {
        if (map!=null) {
            this.backends = (List<T>) map.values();
        }
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#when(java.lang.Object)
     */
    @Override
    public ICriterion<T> when(final Object param) {
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#thenGetResult()
     */
    @Override
    public T thenGetResult() {

        if (backends.isEmpty()) {
            return null;
        }

        Collections.sort(backends, new Comparator<T>() {

            @Override
            public int compare(Object o1, Object o2) {
                if (o1 instanceof Backend) {
                    Backend backend1 = (Backend) o1;
                    Backend backend2 = (Backend) o2;

                    return backend1.getActiveConnections() - backend2.getActiveConnections();
                } else {
                    log.error(String.format("%s support only %s class",
                            this.getClass().getName(), Backend.class.getName()));
                }
                return 0;
            }

        });

        return backends.get(0);
    }

}
