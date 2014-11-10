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

import java.net.InetSocketAddress;

import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;
import org.vertx.java.core.http.HttpServerRequest;

import com.globo.galeb.criteria.IWhenMatch;

/**
 * Class RequestMatch.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 7, 2014.
 */
public class RequestMatch implements IWhenMatch {

    /** The uri path. */
    private String   uriPath       = "";

    /** The headers. */
    private MultiMap headers       = new CaseInsensitiveMultiMap();

    /** The params. */
    private MultiMap params        = new CaseInsensitiveMultiMap();

    /** The remote address. */
    private String   remoteAddress = "";

    /** The remote port. */
    private String   remotePort    = "";

    /**
     * Instantiates a new request match.
     */
    public RequestMatch() {
        this(null);
    }

    /**
     * Instantiates a new request match.
     *
     * @param req the req
     */
    public RequestMatch(HttpServerRequest req) {
        if (req!=null) {
            this.uriPath = req.absoluteURI().getPath();
            this.headers = req.headers();
            this.params = req.params();
            InetSocketAddress localRemoteAddress = req.remoteAddress();
            this.remoteAddress = localRemoteAddress.getHostString();
            this.remotePort = Integer.toString(localRemoteAddress.getPort());
        }
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.IWhenMatch#getUri()
     */
    @Override
    public String getUriPath() {
        return uriPath;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.IWhenMatch#getHeader(java.lang.String)
     */
    @Override
    public String getHeader(String header) {
        return headers.contains(header) ? headers.get(header) : null;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.IWhenMatch#getParam(java.lang.String)
     */
    @Override
    public String getParam(String param) {
        return params.contains(param) ? params.get(param) : null;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.IWhenMatch#getRemoteAddress()
     */
    @Override
    public String getRemoteAddress() {
        return this.remoteAddress;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.IWhenMatch#getRemotePort()
     */
    @Override
    public String getRemotePort() {
        return this.remotePort;
    }

}
