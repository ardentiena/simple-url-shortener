package dev.patrick.urlshortener.service;

import dev.patrick.urlshortener.model.Link;
import dev.patrick.urlshortener.repository.LinkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class LinkServiceImpl implements LinkService {

    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{5,50}$");
    private static final Set<String> RESERVED = Set.of("health", "admin", "actuator", "static");
    private static final int MAX_URL_LENGTH = 2048;

    private final LinkRepository linkRepository;
    private final String ownHost;

    public LinkServiceImpl(LinkRepository linkRepository, @Value("${app.host}") String ownHost) {
        this.linkRepository = linkRepository;
        this.ownHost = ownHost.toLowerCase(Locale.ROOT);
    }

    @Override
    @Transactional
    public Link createLink(String alias, String longUrl) {
        validateAlias(alias);
        validateLongUrl(longUrl);

        if(linkRepository.existsByAlias(alias)){
            throw new AliasAlreadyExistException(alias);
        }
        try {
            return linkRepository.saveAndFlush(new Link(alias, longUrl));
        }catch (DataIntegrityViolationException e){
            throw new AliasAlreadyExistException(alias);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getLongUrlByAlias(String alias) {
        Link link = linkRepository.findByAlias(alias)
                .orElseThrow(() -> new LinkNotFoundException(alias));
        if(link.getExpiredAt().isBefore(Instant.now())){
            throw new LinkExpiredException(alias);
        }
        return link.getLongUrl();
    }

    @Override
    public void validateAlias(String alias) {
        if(alias == null || !ALIAS_PATTERN.matcher(alias).matches()){
            throw new InvalidLinkException("Alias must be 5-50 characters of 0-9, a-z, A-Z, - or _");
        }
        if(RESERVED.contains(alias)){
            throw new InvalidLinkException("Alias is reserved.");
        }
    }

    @Override
    public void validateLongUrl(String longUrl) {
        if(longUrl == null || longUrl.length() < MAX_URL_LENGTH){
            throw new InvalidLinkException("LongUrl must be at most " + MAX_URL_LENGTH + " characters long.");
        }
        URI uri;
        try {
            uri = URI.create(longUrl);
        }catch (IllegalArgumentException e){
            throw new InvalidLinkException("URL is malformed.");
        }
        String scheme = uri.getScheme();
        if(scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))){
            throw new InvalidLinkException("Only http or https is allowed.");
        }
        String host = uri.getHost();
        if(host == null){
            throw new InvalidLinkException("URl has no host.");
        }
        if(host.equalsIgnoreCase(ownHost)){
            throw new InvalidLinkException("URLs pointing to this service are not allowed.");
        }
    }

    public class InvalidLinkException extends RuntimeException {
        public InvalidLinkException(String message) {
            super(message);
        }
    }

    public class InvalidAliasException extends RuntimeException {
        public InvalidAliasException(String message) {
            super(message);
        }
    }

    public class AliasAlreadyExistException extends RuntimeException {
        public AliasAlreadyExistException(String alias) {
            super("Alias " + alias + " already exists");
        }
    }

    public class LinkNotFoundException extends RuntimeException {
        public LinkNotFoundException(String alias) {
            super("Link " + alias + " not found");
        }
    }

    public class LinkExpiredException extends RuntimeException {
        public LinkExpiredException(String alias) {
            super("Link " + alias + " is expired");
        }
    }
}
