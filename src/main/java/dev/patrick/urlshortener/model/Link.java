package  dev.patrick.urlshortener.model;

import java.math.BigDecimal;
import java.security.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor 
@NoArgsConstructor 
public class Link{
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private BigDecimal id;

    @Column(nullable = false)
    private String alias;

    @Column (nullable = false)
    private String longUrl;

    @Column (nullable = false)
    private Timestamp expired_at;

    @Column (nullable = false)
    private Timestamp created_at;
}