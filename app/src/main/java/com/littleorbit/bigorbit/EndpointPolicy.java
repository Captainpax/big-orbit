package com.littleorbit.bigorbit;

import okhttp3.HttpUrl;

/** Keeps production traffic on the fixed Little Orbit HTTPS authority. */
public final class EndpointPolicy {
    private EndpointPolicy() {}

    public static boolean trusted(String value, boolean qaBuild) {
        HttpUrl url = HttpUrl.parse(value);
        if (url == null || url.username().length() > 0 || url.password().length() > 0
                || url.query() != null || url.fragment() != null) {
            return false;
        }
        if (qaBuild) {
            return smokeLoopback(url) || production(url);
        }
        return production(url);
    }

    private static boolean smokeLoopback(HttpUrl url) {
        return "http".equals(url.scheme())
                && "127.0.0.1".equals(url.host())
                && url.port() == 18180
                && "/api".equals(url.encodedPath());
    }

    private static boolean production(HttpUrl url) {
        return "https".equals(url.scheme())
                && "lil-orb.pax-kun.com".equals(url.host())
                && url.port() == 443
                && "/api".equals(url.encodedPath());
    }
}
