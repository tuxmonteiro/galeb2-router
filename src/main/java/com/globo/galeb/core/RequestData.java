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
package com.globo.galeb.core;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;

import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;
import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;

/**
 * Class RequestData.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class RequestData {

    /** The headers. */
    private final MultiMap headers;

    /** The params. */
    private final MultiMap params;

    /** The uri. */
    private URI uri;

    /** The remote address. */
    private String remoteAddress;

    /** The remote port. */
    private String remotePort;

    /** The properties. */
    private JsonObject properties;

    /** The http header host. */
    private final String httpHeaderHost = HttpHeaders.HOST.toString();

    /**
     * Instantiates a new request data.
     */
    public RequestData() {
        this("", "");
    }

    /**
     * Instantiates a new request data.
     *
     * @param remoteAddress the remote address
     * @param remotePort the remote port
     */
    public RequestData(final String remoteAddress,
                       final String remotePort) {
        this(new CaseInsensitiveMultiMap(),
             new CaseInsensitiveMultiMap(),
             null,
             remoteAddress,
             remotePort);
    }

    /**
     * Instantiates a new request data.
     *
     * @param headers the headers
     * @param params the params
     * @param uri the uri
     * @param remoteAddress the remote address
     * @param remotePort the remote port
     */
    public RequestData(final MultiMap headers,
                       final MultiMap params,
                       final URI uri,
                       final String remoteAddress,
                       final String remotePort) {
        this.headers = headers;
        this.params = params;
        this.uri = uri;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
    }

    /**
     * Instantiates a new request data.
     *
     * @param request the httpServerRequest
     */
    public RequestData(final HttpServerRequest request) {
        if (request!=null) {
            this.headers = request.headers();
            this.params = request.params();
            this.uri = request.absoluteURI();
            InetSocketAddress localRemoteAddress = request.remoteAddress();
            this.remoteAddress = localRemoteAddress.getHostString();
            this.remotePort = Integer.toString(localRemoteAddress.getPort());
        } else {
            this.headers = new CaseInsensitiveMultiMap();
            this.params = new CaseInsensitiveMultiMap();
            this.uri = null;
            this.remoteAddress = "";
            this.remotePort = "";
        }
    }

    /**
     * Instantiates a new request data.
     *
     * @param request the request
     */
    public RequestData(final ServerWebSocket request) {
        if (request!=null) {
            this.headers = request.headers();
            this.params = new CaseInsensitiveMultiMap();
            try {
                this.uri = new URI(request.uri());
            } catch (URISyntaxException e) {
                this.uri = null;
            };
            InetSocketAddress localRemoteAddress = request.remoteAddress();
            this.remoteAddress = localRemoteAddress.getHostString();
            this.remotePort = Integer.toString(localRemoteAddress.getPort());
        } else {
            this.headers = new CaseInsensitiveMultiMap();
            this.params = new CaseInsensitiveMultiMap();
            this.uri = null;
            this.remoteAddress = "";
            this.remotePort = "";
        }
    }

    /**
     * Gets the headers.
     *
     * @return the headers
     */
    public MultiMap getHeaders() {
        return headers;
    }

    /**
     * Gets the params.
     *
     * @return the params
     */
    public MultiMap getParams() {
        return params;
    }

    /**
     * Gets the uri.
     *
     * @return the uri
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Gets the remote address.
     *
     * @return the remote address
     */
    public String getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Gets the remote port.
     *
     * @return the remote port
     */
    public String getRemotePort() {
        return remotePort;
    }

    /**
     * Gets the header host.
     *
     * @return the header host
     */
    public String getHeaderHost() {
        return headers.contains(httpHeaderHost) ? headers.get(httpHeaderHost) : "";
    }

    /**
     * Gets the properties.
     *
     * @return the properties
     */
    public JsonObject getProperties() {
        return properties;
    }

    /**
     * Sets the properties.
     *
     * @param properties the new properties
     */
    public void setProperties(final JsonObject properties) {
        this.properties = properties;
    }
}
