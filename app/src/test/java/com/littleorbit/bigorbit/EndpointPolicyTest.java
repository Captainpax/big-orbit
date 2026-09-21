package com.littleorbit.bigorbit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class EndpointPolicyTest {
    @Test
    public void productionAcceptsOnlyExactHttpsApiAuthority() {
        assertTrue(EndpointPolicy.trusted("https://lil-orb.pax-kun.com/api", false));
        assertFalse(EndpointPolicy.trusted("http://lil-orb.pax-kun.com/api", false));
        assertFalse(EndpointPolicy.trusted("https://lil-orb.pax-kun.com.example/api", false));
        assertFalse(EndpointPolicy.trusted("https://lil-orb.pax-kun.com/api?next=x", false));
        assertFalse(EndpointPolicy.trusted("https://user@lil-orb.pax-kun.com/api", false));
    }

    @Test
    public void qaAllowsOnlyFixedEmulatorOrProductionAuthority() {
        assertTrue(EndpointPolicy.trusted("http://10.0.2.2:18180/api", true));
        assertTrue(EndpointPolicy.trusted("https://lil-orb.pax-kun.com/api", true));
        assertFalse(EndpointPolicy.trusted("http://127.0.0.1:18180/api", true));
    }
}
