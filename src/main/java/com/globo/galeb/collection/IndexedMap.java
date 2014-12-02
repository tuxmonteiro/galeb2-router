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
package com.globo.galeb.collection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class IndexedMap.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 8, 2014.
 * @param <K> the key type
 * @param <V> the value type
 */
public class IndexedMap<K, V> extends TreeMap<K, V> {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 7940164428900645591L;

    /** The indexed keys. */
    private Map<Integer, K> indexedKeys = new HashMap<>();

    /**
     * Instantiates a new indexed map.
     */
    public IndexedMap() {
        super();
    }

    /**
     * Instantiates a new indexed map.
     *
     * @param map the map
     */
    public IndexedMap(Map<? extends K, ? extends V> map) {
        super(map);
        putAllToIndex(map);
    }

    /* (non-Javadoc)
     * @see java.util.HashMap#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public V put(K key, V value) {
        Integer index = getNextIndex();
        indexedKeys.put(index, key);
        return super.put(key, value);
    }

    /* (non-Javadoc)
     * @see java.util.HashMap#putAll(java.util.Map)
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        putAllToIndex(m);
        super.putAll(m);
    }

    /* (non-Javadoc)
     * @see java.util.HashMap#remove(java.lang.Object)
     */
    @Override
    public V remove(Object key) {
        indexedKeys.remove(key);
        return super.remove(key);
    }

    /* (non-Javadoc)
     * @see java.util.HashMap#clear()
     */
    @Override
    public void clear() {
        super.clear();
        indexedKeys.clear();
    }

    /**
     * Put all to index.
     *
     * @param map the map
     */
    private void putAllToIndex(Map<? extends K, ? extends V> map) {
        Integer nextIndex = getNextIndex();
        for (K key: map.keySet()) {
            indexedKeys.put(nextIndex, key);
            nextIndex++;
        }
    }

    /**
     * Gets the next index.
     *
     * @return the next index
     */
    private synchronized Integer getNextIndex() {
        return indexedKeys.isEmpty() ? 0 : Collections.max(indexedKeys.keySet()) + 1;
    }

    /**
     * Gets the value by index.
     *
     * @param index the index
     * @return the value by index
     */
    public V getValueByIndex(Integer index) {
        return super.get(indexedKeys.get(index));
    }

    /**
     * Gets the key by index.
     *
     * @param index the index
     * @return the key by index
     */
    public K getKeyByIndex(Integer index) {
        return indexedKeys.get(index);
    }

    /**
     * Reindex the keys
     */
    public void reindex() {
        indexedKeys.clear();
        putAllToIndex(this);
    }

    /**
     * Gets the index.
     *
     * @param key the key
     * @return the index
     */
    public Integer getIndex(K key) {

        Integer index = -1;

        if (this.containsKey(key)) {
            for (Entry<Integer, K> entry : indexedKeys.entrySet()) {
                if (entry.getValue() == key) {
                    index = entry.getKey();
                    break;
                }
            }
        }

        return index;
    }

}
