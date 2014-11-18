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
package com.globo.galeb.loadbalance.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.globo.galeb.core.Backend;
import com.globo.galeb.core.IBackend;

/**
 * Class LeastConnectionsFinder: Find the backend with least connection
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class LeastConnectionsFinder {

    /** The map of the backends. */
    private final Map<Backend, Integer> mapBackends = new HashMap<>();

    /**
     * Instantiates a new least connections finder.
     *
     * @param backends the backends
     */
    public LeastConnectionsFinder(final Collection<Backend> backends) {
        for (Backend backend : backends) {
            mapBackends.put(backend, backend.getActiveConnections());
        }
    }

    /**
     * Adds the backend to map.
     *
     * @param backend the backend
     */
    public void add(final Backend backend) {
        mapBackends.put(backend, backend.getActiveConnections());
    }

    /**
     * Adds a backends collection to map.
     *
     * @param backends the backends
     */
    public void addAll(final Collection<Backend> backends) {
        for (Backend backend : backends) {
            add(backend);
        }
    }

    /**
     * Update the backends map
     */
    public void update() {
        addAll(mapBackends.keySet());
    }

    /**
     * Rebuild the backends map
     *
     * @param backends the backends
     */
    public void rebuild(final Collection<Backend> backends) {
        mapBackends.clear();
        addAll(backends);
    }

    /**
     * Gets the backend chosen
     *
     * @return the backend
     */
    public IBackend get() {
        IBackend chosen;
        if (!mapBackends.isEmpty()) {
            chosen = Collections.min(mapBackends.entrySet(), new Comparator<Entry<Backend, Integer>>() {
                @Override
                public int compare(Entry<Backend, Integer> o1, Entry<Backend, Integer> o2) {
                    return o1.getValue().compareTo(o2.getValue());
                }
            }).getKey();
         } else {
             chosen = null;
         }

        return chosen;
    }
}
