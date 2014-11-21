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

import org.vertx.java.core.json.JsonObject;

/**
 * Interface ICallbackConnectionCounter.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public interface ICallbackConnectionCounter {

    /**
     * Callback global connections info.
     *
     * @param body the body
     */
    public void callbackGlobalConnectionsInfo(JsonObject body);

    /**
     * Sets the registered.
     *
     * @param registered the new registered
     */
    public void setRegistered(boolean registered);

    /**
     * Checks if is registered.
     *
     * @return true, if is registered
     */
    public boolean isRegistered();

}
