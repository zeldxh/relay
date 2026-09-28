package dev.zeldxh.relay.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyHasherTest {

    private final ApiKeyHasher apiKeyHasher = new ApiKeyHasher();

    @Test
    void generatesUniquePrefixedKeys() {
        String a = apiKeyHasher.generateKey();
        String b = apiKeyHasher.generateKey();

        assertThat(a).isNotEqualTo(b);
        assertThat(a).startsWith("relay_");
    }

    @Test
    void hashIsDeterministicAndDoesNotLeakTheRawKey() {
        String key = apiKeyHasher.generateKey();

        assertThat(apiKeyHasher.hash(key)).isEqualTo(apiKeyHasher.hash(key));
        assertThat(apiKeyHasher.hash(key)).isNotEqualTo(key);
    }

    @Test
    void differentKeysHashDifferently() {
        String a = apiKeyHasher.generateKey();
        String b = apiKeyHasher.generateKey();

        assertThat(apiKeyHasher.hash(a)).isNotEqualTo(apiKeyHasher.hash(b));
    }
}
