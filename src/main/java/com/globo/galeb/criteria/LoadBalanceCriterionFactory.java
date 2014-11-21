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
package com.globo.galeb.criteria;

import com.globo.galeb.criteria.impl.RandomCriterion;
import com.globo.galeb.entity.impl.backend.IBackend;

/**
 * A factory for creating LoadBalanceCriterion objects.
 */
public class LoadBalanceCriterionFactory {

    /** The Constant CLASS_SUFFIX. */
    public static final String CLASS_SUFFIX        = "Criterion";

    /** The Constant CLASS_PACKAGE. */
    public static final String CLASS_PACKAGE       = LoadBalanceCriterionFactory.class.getPackage().getName()+".impl.";

    /** The Constant DEFAULT_LOADBALANCE. */
    public static final String DEFAULT_LOADBALANCE = RandomCriterion.class.getSimpleName().replaceFirst(CLASS_SUFFIX, "");

    /**
     * Creates the load balance by name
     *
     * @param loadBalanceName the load balance name
     * @return the criterion
     */
    @SuppressWarnings("unchecked")
    public static ICriterion<IBackend> create(String loadBalanceName) {
        if (loadBalanceName==null || "".equals(loadBalanceName)) {
            return create(DEFAULT_LOADBALANCE);
        }

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String loadBalanceFullName = CLASS_PACKAGE+loadBalanceName+CLASS_SUFFIX;

        try {
            Class<ICriterion<IBackend>> clazz = (Class<ICriterion<IBackend>>) loader.loadClass(loadBalanceFullName);
            ICriterion<IBackend> instance = clazz.newInstance();
            return (ICriterion<IBackend>) instance;

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            // Load default
            return create(DEFAULT_LOADBALANCE);
        }
    }

}
