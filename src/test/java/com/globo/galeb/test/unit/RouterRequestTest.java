package com.globo.galeb.test.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;

import org.junit.Test;
import org.vertx.java.core.http.CaseInsensitiveMultiMap;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpVersion;

import com.globo.galeb.core.request.RouterRequest;

public class RouterRequestTest {

    public HttpServerRequest getHttpServerRequestHTTP_1_0() {
        HttpServerRequest httpServerRequest = mock(HttpServerRequest.class);
        when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_0);
        when(httpServerRequest.headers()).thenReturn(new CaseInsensitiveMultiMap());
        when(httpServerRequest.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 0));

        return httpServerRequest;
    }

    public HttpServerRequest getHttpServerRequestHTTP_1_1() {
        HttpServerRequest httpServerRequest = mock(HttpServerRequest.class);
        when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_1);
        when(httpServerRequest.headers()).thenReturn(new CaseInsensitiveMultiMap());
        when(httpServerRequest.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 0));

        return httpServerRequest;
    }

    @Test
    public void headersWithHttpVersion10WithConnectionKeepAlive() {

        HttpServerRequest httpServerRequest = getHttpServerRequestHTTP_1_0();
        httpServerRequest.headers().set("Connection", "keep-alive");
        RouterRequest routerRequest = new RouterRequest(httpServerRequest);

        boolean isKeepAlive = routerRequest.isHttpKeepAlive();

        assertThat(isKeepAlive).isTrue();

    }

    @Test
    public void headersWithHttpVersion10WithConnectionClose() {

        HttpServerRequest httpServerRequest = getHttpServerRequestHTTP_1_0();
        httpServerRequest.headers().set("Connection", "close");
        RouterRequest routerRequest = new RouterRequest(httpServerRequest);

        boolean isKeepAlive = routerRequest.isHttpKeepAlive();

        assertThat(isKeepAlive).isFalse();

    }

    @Test
    public void headersWithHttpVersion10WithoutConnectionHeader() {

        HttpServerRequest httpServerRequest = getHttpServerRequestHTTP_1_0();
        RouterRequest routerRequest = new RouterRequest(httpServerRequest);

        boolean isKeepAlive = routerRequest.isHttpKeepAlive();

        assertThat(isKeepAlive).isFalse();

    }

    @Test
    public void headersWithHttpVersion11WithConnectionKeepAlive() {

        HttpServerRequest httpServerRequest = getHttpServerRequestHTTP_1_1();
        httpServerRequest.headers().set("Connection", "keep-alive");
        RouterRequest routerRequest = new RouterRequest(httpServerRequest);

        boolean isKeepAlive = routerRequest.isHttpKeepAlive();

        assertThat(isKeepAlive).isTrue();

    }

    @Test
    public void headersWithHttpVersion11WithConnectionClose() {

        HttpServerRequest httpServerRequest = getHttpServerRequestHTTP_1_1();
        httpServerRequest.headers().set("Connection", "close");
        RouterRequest routerRequest = new RouterRequest(httpServerRequest);

        boolean isKeepAlive = routerRequest.isHttpKeepAlive();

        assertThat(isKeepAlive).isFalse();

    }

    @Test
    public void headersWithHttpVersion11WithoutConnectionHeader() {

        HttpServerRequest httpServerRequest = getHttpServerRequestHTTP_1_1();
        RouterRequest routerRequest = new RouterRequest(httpServerRequest);

        boolean isKeepAlive = routerRequest.isHttpKeepAlive();

        assertThat(isKeepAlive).isTrue();

    }

}
