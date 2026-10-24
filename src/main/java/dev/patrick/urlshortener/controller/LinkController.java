package dev.patrick.urlshortener.controller;

import dev.patrick.urlshortener.dto.CreateLinkRequest;
import dev.patrick.urlshortener.dto.LinkResponse;
import dev.patrick.urlshortener.model.Link;
import dev.patrick.urlshortener.service.LinkServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
public class LinkController {
    private final LinkServiceImpl linkService;

    public LinkController(LinkServiceImpl linkService) {
        this.linkService = linkService;
    }

    @PostMapping("/api/links")
    public ResponseEntity<LinkResponse> createLink(@RequestBody CreateLinkRequest linkRequest) {
        Link link = linkService.createLink(linkRequest.alias(), linkRequest.longUrl());
        String shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/{alias}")
                .buildAndExpand(link.getAlias())
                .toUriString();
        LinkResponse responseBody = new LinkResponse(
                link.getAlias(),
                shortUrl,
                link.getLongUrl(),
                link.getExpiredAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    @GetMapping("/{alias}")
    public ResponseEntity<Void> redirect(@PathVariable String alias){
        String longUrl = linkService.getLongUrlByAlias(alias);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl))
                .build();
    }
}
