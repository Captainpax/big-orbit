package com.littleorbit.bigorbit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class ChallengeMessageTest {
    @Test
    public void canonicalMessageIsByteStableAndPurposeSeparated() {
        assertEquals(
                "big-orbit:session:1234:random-value",
                ChallengeMessage.canonical("session", "1234", "random-value"));
        assertEquals(
                "big-orbit:enrollment:1234:random-value",
                ChallengeMessage.canonical("enrollment", "1234", "random-value"));
        assertEquals(
                "big-orbit:bootstrap:1234:random-value",
                ChallengeMessage.canonical("bootstrap", "1234", "random-value"));
    }

    @Test
    public void arbitraryPurposesAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ChallengeMessage.canonical("delete", "1234", "random-value"));
    }
}
