package com.informationplatform.hub.identity.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UsernameNormalizerTest {

    @Test
    void trimsAndLowerCasesWithStableLocale() {
        assertEquals("admin", UsernameNormalizer.normalize("  AdMiN  "));
        assertEquals("", UsernameNormalizer.normalize(null));
    }
}
