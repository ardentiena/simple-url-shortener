package  dev.patrick.urlshortener.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table(name = "links")
@Data
@NoArgsConstructor 
public class Link {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private BigDecimal id;

    @Column(nullable = false, unique = true, length = 50)
    private String alias;

    @Column(nullable = false, columnDefinition = "text")
    private String longUrl;

    @Column(nullable = false)
    private Instant expiredAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Link(String alias, String longUrl) {
        this.alias = alias;
        this.longUrl = longUrl;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (expiredAt == null) {
            expiredAt = createdAt.plus(90, ChronoUnit.DAYS);
        }
    }
}