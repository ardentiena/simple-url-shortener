package dev.patrick.urlshortener.dto;

import dev.patrick.urlshortener.model.Link;

import java.time.Instant;

public record LinkResponse(
        String alias,
        String shortUrl,
        String longUrl,
        Instant expiredAt) {
    public static LinkResponse from(Link link, String shortUrl){
        return new LinkResponse(
                link.getAlias(),
                shortUrl,
                link.getLongUrl(),
                link.getExpiredAt());
    }
}
