package dev.patrick.urlshortener.service;

import dev.patrick.urlshortener.model.Link;

import java.util.Optional;

public interface LinkService {
    Link createLink(String alias, String longUrl);
    String getLongUrlByAlias(String alias);
    void validateAlias(String alias);
    void validateLongUrl(String longUrl);
}