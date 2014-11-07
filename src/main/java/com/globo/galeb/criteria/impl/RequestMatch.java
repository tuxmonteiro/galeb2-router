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

import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServerRequest;

import com.globo.galeb.criteria.IWhenMatch;

/**
 * Class RequestMatch.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 7, 2014.
 */
public class RequestMatch implements IWhenMatch {

    /** The match. */
    private Object match;

    /** The req. */
    private final HttpServerRequest req;

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
        this.req = req;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.IWhenMatch#getUri()
     */
    @Override
    public String getUri() {
        return req.absoluteURI().getPath();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.IWhenMatch#getHeader(java.lang.String)
     */
    @Override
    public String getHeader(String header) {
        MultiMap headers = req.headers();
        return headers.contains(header) ? headers.get(header) : null;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.IWhenMatch#getParam(java.lang.String)
     */
    @Override
    public String getParam(String param) {
        MultiMap params = req.params();
        return params.contains(param) ? params.get(param) : null;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.IWhenMatch#isNull()
     */
    @Override
    public boolean isNull() {
        return req == null;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.IWhenMatch#getMatch()
     */
    @Override
    public Object getMatch() {
        return match;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.IWhenMatch#setMatch(java.lang.Object)
     */
    @Override
    public IWhenMatch setMatch(Object match) {
        this.match = match;
        return this;
    }

}
