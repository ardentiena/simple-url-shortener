package dev.patrick.urlshortener.service;

import dev.patrick.urlshortener.component.LinkCache;
import dev.patrick.urlshortener.model.Link;
import dev.patrick.urlshortener.repository.LinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkServiceImplTest {
    @Mock
    LinkRepository linkRepository;
    @Mock
    LinkCache linkCache;

    private LinkServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LinkServiceImpl(linkRepository, linkCache, "localhost");
    }

    @Test
    void createLink_saveValidLink() {
        when(linkRepository.existsByAlias("spring")).thenReturn(false);
        when(linkRepository.saveAndFlush(any(Link.class))).thenAnswer(invoc -> invoc.getArgument(0));

        Link result = service.createLink("spring", "https://spring.io");

        assertEquals("spring", result.getAlias());
        assertEquals("https://spring.io", result.getLongUrl());
    }

    @Test
    void createLink_rejectsReservedAlias() {
        assertThrows(LinkServiceImpl.InvalidLinkException.class,
                () -> service.createLink("admin", "https://spring.io"));
        verifyNoInteractions(linkRepository);
    }

    @Test
    void createLink_rejectsShortAlias() {
        assertThrows(LinkServiceImpl.InvalidLinkException.class,
                () -> service.createLink("abc", "https://spring.io"));
    }

    @Test
    void createLink_rejectsNonHttpScheme() {
        assertThrows(LinkServiceImpl.InvalidLinkException.class,
                () -> service.createLink("myftp", "ftp://example.com/file"));
    }

    @Test
    void createLink_rejectsOwnHost() {
        assertThrows(LinkServiceImpl.InvalidLinkException.class,
                () -> service.createLink("loop1", "http://localhost:8081/abc"));
    }

    @Test
    void createLink_rejectsNullUrl() {
        assertThrows(LinkServiceImpl.InvalidLinkException.class,
                () -> service.createLink("valid1", null));
    }

    @Test
    void createLink_rejectsUrlLongerThan2048() {
        String longUrl = "https://example.com/" + "a".repeat(2050);
        assertThrows(LinkServiceImpl.InvalidLinkException.class,
                () -> service.createLink("valid1", longUrl));
    }

    @Test
    void createLink_rejectsDuplicateAlias() {
        when(linkRepository.existsByAlias("spring")).thenReturn(true);

        assertThrows(LinkServiceImpl.AliasAlreadyExistException.class,
                () -> service.createLink("spring", "https://spring.io"));
    }

    @Test
    void getLongUrlByAlias_returnsCachedUrlWithoutQueryingDatabase(){
        when(linkCache.findLongUrl("spring")).thenReturn(Optional.of("https://spring.io"));
        String longUrl = service.getLongUrlByAlias("spring");
        assertEquals("https://spring.io", longUrl);
        verifyNoInteractions(linkRepository);
    }

    @Test
    void getLongUrlByAlias_storesUrlInCacheAfterDatabaseHit(){
        Instant expiredAt = Instant.now().plus(1, ChronoUnit.DAYS);
        Link link = new Link("spring", "https://spring.io");
        link.setExpiredAt(expiredAt);
        when(linkRepository.findByAlias("spring")).thenReturn(Optional.of(link));
        service.getLongUrlByAlias("spring");
        verify(linkCache).saveLongUrl("spring", "https://spring.io", expiredAt);
    }

    @Test
    void getLongUrlByAlias_doesNotCacheExpiredLink(){
        Link link = new Link("old123", "https://spring.io");
        link.setExpiredAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(linkRepository.findByAlias("old123")).thenReturn(Optional.of(link));

        assertThrows(LinkServiceImpl.LinkExpiredException.class, () -> service.getLongUrlByAlias("old123"));
        verify(linkCache, never()).saveLongUrl(any(), any(), any());
    }

    @Test
    void getLongUrlByAlias_returnsUrlForActiveLink() {
        Link link = new Link("spring", "https://spring.io");
        link.setExpiredAt(Instant.now().plus(1, ChronoUnit.DAYS));
        when(linkRepository.findByAlias("spring")).thenReturn(Optional.of(link));

        assertEquals("https://spring.io", service.getLongUrlByAlias("spring"));
    }

    @Test
    void getLongUrlByAlias_throwsNotFound() {
        when(linkRepository.findByAlias("nothere")).thenReturn(Optional.empty());

        assertThrows(LinkServiceImpl.LinkNotFoundException.class,
                () -> service.getLongUrlByAlias("nothere"));
    }

    @Test
    void getLongUrlByAlias_throwsExpired() {
        Link link = new Link("old123", "https://spring.io");
        link.setExpiredAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(linkRepository.findByAlias("old123")).thenReturn(Optional.of(link));

        assertThrows(LinkServiceImpl.LinkExpiredException.class,
                () -> service.getLongUrlByAlias("old123"));
    }
}
