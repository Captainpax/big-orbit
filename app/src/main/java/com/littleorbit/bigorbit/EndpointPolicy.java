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
            return ("http".equals(url.scheme()) && "10.0.2.2".equals(url.host()))
                    || ("https".equals(url.scheme())
                    && "lil-orb.pax-kun.com".equals(url.host()));
        }
        return "https".equals(url.scheme())
                && "lil-orb.pax-kun.com".equals(url.host())
                && url.port() == 443
                && "/api".equals(url.encodedPath());
    }
}
