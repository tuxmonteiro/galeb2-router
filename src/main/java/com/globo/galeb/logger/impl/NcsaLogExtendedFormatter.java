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
package com.globo.galeb.logger.impl;

import java.util.Calendar;

import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpVersion;

import com.globo.galeb.logger.HttpLogFormatter;

/**
 * Class NcsaLogExtendedFormatter.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class NcsaLogExtendedFormatter implements HttpLogFormatter {

    /** The HttpServerRequest. */
    private HttpServerRequest req;

    /* (non-Javadoc)
     * @see com.globo.galeb.logger.HttpLogFormatter#setRequestData(java.lang.Object)
     */
    @Override
    public HttpLogFormatter setRequestData(final Object request) {
        this.req = (HttpServerRequest) request;
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.logger.HttpLogFormatter#getFormatedLog()
     */
    @Override
    public String getFormatedLog() {
        if (req!=null) {

            // format: virtualhost remotehost rfc931 authuser [date] "method request_uri version" status bytes

            String virtualhost = req.headers().contains(HttpHeaders.HOST) ?
                                    req.headers().get(HttpHeaders.HOST) : "-";

            String remotehost = req.remoteAddress().getHostString();
            String rfc931 = "-";
            String authuser = "-";
            String date = req.headers().contains(HttpHeaders.DATE) ?
                            req.headers().get(HttpHeaders.DATE) :
                            Calendar.getInstance().getTime().toString();

            String method = req.method();
            HttpVersion httpVersion = req.version();
            String version = "";

            switch (httpVersion) {
                case HTTP_1_0:
                    version = "HTTP/1.0";
                    break;
                case HTTP_1_1:
                    version = "HTTP/1.1";
                    break;
                default:
                    version = httpVersion.toString();
                    break;
            }

            String requestUri = req.path();
            int status = req.response().getStatusCode();
            int bytes = 0;

            try {
                bytes = req.headers().contains(HttpHeaders.CONTENT_LENGTH) ?
                            Integer.parseInt(req.headers().get(HttpHeaders.CONTENT_LENGTH)) : 0;
            } catch (NumberFormatException ignore) {
            }

            return String.format("%s %s %s %s [%s] \"%s %s %s\" %d %d",
                    virtualhost,
                    remotehost,
                    rfc931,
                    authuser,
                    date,
                    method,
                    requestUri,
                    version,
                    status,
                    bytes);

        } else {
            return null;

        }
    }

}
