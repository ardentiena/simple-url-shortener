package dev.patrick.urlshortener.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Component
public class LinkCache {
    private static final Logger log = LoggerFactory.getLogger(LinkCache.class);
    private static final String KEY_PREFIX = "link:";

    private final StringRedisTemplate redisTemplate;
    private final boolean cacheEnabled;
    private final Duration cacheDuration;

    public LinkCache(StringRedisTemplate redisTemplate,
                     @Value("${app.cache.enabled:true}") boolean cacheEnabled,
                     @Value("${app.cache.max-ttl: PT24H}") Duration cacheDuration){
        this.redisTemplate = redisTemplate;
        this.cacheEnabled = cacheEnabled;
        this.cacheDuration = cacheDuration;
    }

    public Optional<String> findLongUrl(String alias){
        if(!cacheEnabled){
            return Optional.empty();
        }
        try{
            return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + alias));
        }catch (DataAccessException redisFailure){
            log.warn("Redis read failed, falling back to database: {}", redisFailure.getMessage());
            return Optional.empty();
        }
    }

    public void saveLongUrl(String alias, String longUrl, Instant expiredAt){
        if(!cacheEnabled){
            return;
        }
        Duration timeUntilExpiry = Duration.between(Instant.now(), expiredAt);
        Duration timeToLive = timeUntilExpiry.compareTo(cacheDuration) < 0 ? timeUntilExpiry : cacheDuration;
        if(timeToLive.isZero() || timeToLive.isNegative()){
            return;
        }
        try{
            redisTemplate.opsForValue().set(KEY_PREFIX + alias, longUrl, timeToLive);
        }catch (DataAccessException redisFailure){
            log.warn("Redis write failed, link server without caching: {}", redisFailure.getMessage());
        }
    }
}
