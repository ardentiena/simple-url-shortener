package dev.patrick.urlshortener.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AliasGeneratorTest {
    private final AliasGenerator aliasGenerator = new AliasGenerator();

    @Test
    void generate_returnsSixCharactersFromAlphanumericAlphabet() {
        for(int repetition = 0; repetition < 1000; repetition++){
            assertTrue(aliasGenerator.generate().matches("^[0-9a-zA-Z]{6}$"));
        }
    }
}
