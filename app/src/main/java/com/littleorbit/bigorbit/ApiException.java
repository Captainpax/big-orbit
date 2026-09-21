package com.littleorbit.bigorbit;

/** Status-only network failure that never retains a response body. */
public final class ApiException extends Exception {
    private final int statusCode;

    public ApiException(int statusCode) {
        super("Big Orbit API request failed with status " + statusCode);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
