package dev.patrick.urlshortener.dto;

public record CreateLinkRequest(String alias, String longUrl) {
}
