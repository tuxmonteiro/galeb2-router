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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.globo.galeb.criteria.ICriterion;
import com.globo.galeb.logger.SafeLogger;

import org.vertx.java.core.logging.Logger;

/**
 * Class RoundRobinCriterion.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 7, 2014.
 * @param <T> the generic type
 */
public class RoundRobinCriterion<T> implements ICriterion<T> {

    /** The log. */
    private final SafeLogger log            = new SafeLogger();

    /** The collection. */
    private List<T>          collection     = new LinkedList<T>();

    /** The originals. */
    private List<T>          originals      = new LinkedList<T>();

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
            if (collection.isEmpty()) {
                collection.addAll(new LinkedList<T>(map.values()));
            }
            originals.addAll(map.values());
        } else {
            log.error(String.format("%s: map is null", this.getClass().getName()));
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
        T result = null;

        if (originals.isEmpty()) {
            return result;
        }

        if (!collection.isEmpty()) {
            result = ((LinkedList<T>) collection).poll();
        }

        if (collection.isEmpty()) {
            collection.addAll(new LinkedList<T>(originals));
        }

        return result;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#action(com.globo.galeb.criteria.ICriterion.CriterionAction)
     */
    @Override
    public ICriterion<T> action(ICriterion.CriterionAction criterionAction) {
        switch (criterionAction) {
            case RESET_REQUIRED:
                this.collection.clear();
                break;

            default:
                break;
        }
        return this;
    }

}
