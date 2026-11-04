package dev.patrick.urlshortener.component;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class AliasGenerator {
    private static final String ALIAS_CHARACTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int ALIAS_LENGTH = 6;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate(){
        StringBuilder generatedAlias = new StringBuilder(ALIAS_LENGTH);
        for(int position = 0; position < ALIAS_LENGTH; position++){
            int charIndex = secureRandom.nextInt(ALIAS_CHARACTERS.length());
            generatedAlias.append(ALIAS_CHARACTERS.charAt(charIndex));
        }
        return generatedAlias.toString();
    }
}
