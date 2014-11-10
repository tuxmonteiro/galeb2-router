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

import com.globo.galeb.criteria.IWhenMatch;

/**
 * Class NullMatch.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 7, 2014.
 */
public class NullMatch implements IWhenMatch {

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.IWhenMatch#getUri()
     */
    @Override
    public String getUriPath() {
        return null;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.IWhenMatch#getHeader(java.lang.String)
     */
    @Override
    public String getHeader(String header) {
        return header;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.IWhenMatch#getParam(java.lang.String)
     */
    @Override
    public String getParam(String param) {
        return param;
    }

    @Override
    public String getRemoteAddress() {
        return null;
    }

    @Override
    public String getRemotePort() {
        return null;
    }

}
