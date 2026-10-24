package dev.patrick.urlshortener.repository;

import dev.patrick.urlshortener.model.Link;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LinkRepository extends JpaRepository<Link, Long> {
    Optional<Link> findByAlias(String alias);

    Boolean existsByAlias(String alias);
}
