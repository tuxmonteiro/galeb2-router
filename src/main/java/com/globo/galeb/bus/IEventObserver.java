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

/**
 * An asynchronous update interface for receiving notifications.
 */
public interface IEventObserver {

    /**
     * This method is called when information about an Version
     * event which was previously requested using an asynchronous
     * interface becomes available.
     *
     * @param version the version
     */
    public void setVersion(Long version);

    /**
     * This method is called when information about an Add
     * event which was previously requested using an asynchronous
     * interface becomes available.
     *
     * @param message the message
     */
    public void postAddEvent(String message);

    /**
     * This method is called when information about an Del
     * event which was previously requested using an asynchronous
     * interface becomes available.
     *
     * @param message the message
     */
    public void postDelEvent(String message);
}
