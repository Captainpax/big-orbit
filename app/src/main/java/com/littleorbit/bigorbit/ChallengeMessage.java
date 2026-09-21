package com.littleorbit.bigorbit;

/** Byte-exact domain separation shared with the Little Orbit API. */
public final class ChallengeMessage {
    private ChallengeMessage() {}

    public static String canonical(
            String purpose, String challengeId, String challenge) {
        if (!("session".equals(purpose) || "enrollment".equals(purpose))) {
            throw new IllegalArgumentException("Unsupported challenge purpose");
        }
        return "big-orbit:" + purpose + ":" + challengeId + ":" + challenge;
    }
}
