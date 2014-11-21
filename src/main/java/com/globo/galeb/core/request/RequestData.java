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
package com.globo.galeb.core.request;

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

    /** The Constant HTTP_VERSION_DEFAULT. */
    private static final String HTTP_VERSION_DEFAULT = "HTTP_1_1";

    /** The headers. */
    private MultiMap headers = new CaseInsensitiveMultiMap();

    /** The params. */
    private MultiMap params = new CaseInsensitiveMultiMap();

    /** The version. */
    private String version = HTTP_VERSION_DEFAULT;

    /** The keep alive. */
    private boolean keepAlive = true;

    /** The uri. */
    private URI uri = null;

    /** The remote address. */
    private String remoteAddress = "0.0.0.0";

    /** The remote port. */
    private String remotePort = "0";

    /** The properties. */
    private final JsonObject properties = new JsonObject();

    /** The http header host. */
    private final String httpHeaderHost = HttpHeaders.HOST.toString();

    /** The http header connection. */
    private final String httpHeaderConnection = HttpHeaders.CONNECTION.toString();

    /**
     * Instantiates a new request data.
     */
    public RequestData() {
        this((HttpServerRequest)null);
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
            this.version = request.version().toString();
            this.uri = request.absoluteURI();
            InetSocketAddress localRemoteAddress = request.remoteAddress();
            this.remoteAddress = localRemoteAddress.getHostString();
            this.remotePort = Integer.toString(localRemoteAddress.getPort());
            this.keepAlive = headers.contains(httpHeaderConnection) ?
                    !"close".equalsIgnoreCase(headers.get(httpHeaderConnection)) :
                        request.version().equals(HTTP_VERSION_DEFAULT);
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
        }
        this.version = HTTP_VERSION_DEFAULT;
        this.keepAlive = true;
    }

    /**
     * Sets the headers.
     *
     * @param headers the headers
     * @return the request data
     */
    public RequestData setHeaders(MultiMap headers) {
        this.headers = headers;
        return this;
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
     * Sets the params.
     *
     * @param params the params
     * @return the request data
     */
    public RequestData setParams(MultiMap params) {
        this.params = params;
        return this;
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
     * Sets the version.
     *
     * @param version the version
     * @return the request data
     */
    public RequestData setVersion(String version) {
        this.version = version;
        return this;
    }

    /**
     * Gets the version.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the keep alive.
     *
     * @param keepAlive the keep alive
     * @return the request data
     */
    public RequestData setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    /**
     * Gets the keep alive.
     *
     * @return the keep alive
     */
    public boolean getKeepAlive() {
        return this.keepAlive;
    }

    /**
     * Sets the uri.
     *
     * @param uri the uri
     * @return the request data
     */
    public RequestData setUri(URI uri) {
        this.uri = uri;
        return this;
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
     * Sets the remote address.
     *
     * @param remoteAddress the remote address
     * @return the request data
     */
    public RequestData setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
        return this;
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
     * Sets the remote port.
     *
     * @param remotePort the remote port
     * @return the request data
     */
    public RequestData setRemotePort(String remotePort) {
        this.remotePort = remotePort;
        return this;
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
     * Sets the properties.
     *
     * @param properties the new properties
     */
    public RequestData setProperties(JsonObject properties) {
        this.properties.mergeIn(properties);
        return this;
    }

    /**
     * Gets the properties.
     *
     * @return the properties
     */
    public JsonObject getProperties() {
        return properties;
    }

}
