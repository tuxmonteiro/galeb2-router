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

import java.util.HashMap;
import java.util.Map;
import com.globo.galeb.entity.impl.Farm;

/**
 * Class MessageToMapBuilder.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class MessageToMapBuilder {

    /** The path supported map. */
    private final Map<String, MessageToMap<?>> pathSupportedMap = new HashMap<>();

    /** The farm instance. */
    private Farm farm = null;

    /**
     * Instantiates a new MessageToMapBuilder.
     */
    public MessageToMapBuilder() {

        // TODO: Dynamic loader

        MessageToMap<?> farmMap = new FarmMap();
        MessageToMap<?> virtualhostMap = new VirtualhostMap();
        MessageToMap<?> ruleMap = new RuleMap();
        MessageToMap<?> backendPoolMap = new BackendPoolMap();
        MessageToMap<?> backendMap = new BackendMap();

        pathSupportedMap.put(farmMap.getUriBase(), farmMap);
        pathSupportedMap.put(virtualhostMap.getUriBase(), virtualhostMap);
        pathSupportedMap.put(ruleMap.getUriBase(), ruleMap);
        pathSupportedMap.put(backendPoolMap.getUriBase(), backendPoolMap);
        pathSupportedMap.put(backendMap.getUriBase(), backendMap);
    }

    /**
     * Gets the single instance of MessageToMap.
     *
     * @param message the message
     * @param farm the farm
     * @return single instance of MessageToMap
     */
    public MessageToMap<?> getMessageToMap(String message) {

        final MessageBus messageBus = new MessageBus(message);
        MessageToMap<?> messageToMap = pathSupportedMap.get(messageBus.getUriBase());

        if (messageToMap!=null && farm!=null) {
            return messageToMap.setMessageBus(messageBus).setFarm(farm);
        } else {
            return new NullMap();
        }
    }

    /**
     * Sets the farm.
     *
     * @param farm the farm
     * @return this
     */
    public MessageToMapBuilder setFarm(final Farm farm) {
        this.farm = farm;
        return this;
    }

}
