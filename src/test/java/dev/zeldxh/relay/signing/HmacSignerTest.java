package dev.zeldxh.relay.signing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HmacSignerTest {

    private final HmacSigner signer = new HmacSigner();

    @Test
    void sameInputsProduceSameSignature() {
        String a = signer.sign("{\"hello\":\"world\"}", "s3cr3t");
        String b = signer.sign("{\"hello\":\"world\"}", "s3cr3t");

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSize(64);
    }

    @Test
    void differentSecretsProduceDifferentSignatures() {
        String a = signer.sign("{\"hello\":\"world\"}", "secret-a");
        String b = signer.sign("{\"hello\":\"world\"}", "secret-b");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void differentPayloadsProduceDifferentSignatures() {
        String a = signer.sign("{\"a\":1}", "s3cr3t");
        String b = signer.sign("{\"a\":2}", "s3cr3t");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void matchesTheKnownVectorTheJsSdkIsTestedAgainst() {
        // Fixed input/output pair shared with sdk/src/signing.test.ts, so a change here that
        // breaks compatibility with the client SDK's signature verification fails loudly on
        // both sides instead of silently drifting apart.
        String signature = signer.sign("{\"type\":\"ping\"}", "test-secret");

        assertThat(signature).isEqualTo("5a325db300c4be4c44b2d95c065fdce8b91830a6e6ce2622d63c301205b83cc3");
    }

    @Test
    void generatesUniqueHexSecrets() {
        String a = signer.generateSecret();
        String b = signer.generateSecret();

        assertThat(a).isNotEqualTo(b);
        assertThat(a).hasSize(64);
        assertThat(a).matches("[0-9a-f]+");
    }
}
