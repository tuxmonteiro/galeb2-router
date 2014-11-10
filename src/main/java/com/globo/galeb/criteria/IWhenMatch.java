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

/**
 * Interface IWhenMatch.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 7, 2014.
 */
public interface IWhenMatch {

    /**
     * Gets the uri.
     *
     * @return the uri
     */
    public String getUriPath();

    /**
     * Gets the header.
     *
     * @param header the header
     * @return the header
     */
    public String getHeader(String header);

    /**
     * Gets the param.
     *
     * @param param the param
     * @return the param
     */
    public String getParam(String param);

    /**
     * Gets the remote address.
     *
     * @return the remote address
     */
    public String getRemoteAddress();

    /**
     * Gets the remote port.
     *
     * @return the remote port
     */
    public String getRemotePort();
}
