package com.jswone.commerce.core.service.shortlink;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.constants.CacheNames;
import com.jswone.commerce.core.entity.shortlink.ShortLink;
import com.jswone.commerce.core.enums.shortlink.LinkType;
import com.jswone.commerce.core.model.elastic.shotlink.ShortLinkClicksIndex;
import com.jswone.commerce.core.model.request.shortlink.CreateShortLinkRequest;
import com.jswone.commerce.core.repository.shortlink.ShortLinkRepository;
import com.jswone.commerce.core.util.shortlinks.Base62Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.jswone.commerce.core.config.ProfileAwareCacheConfig.getCacheNameWithProfile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortLinkService {

    private final ShortLinkRepository repository;
    //    private final ShortLinkClickRepository clickRepository;
    private final CacheManager cacheManager;
    private final CommerceValueConfig commerceValueConfig;
    private static final String PREFIX = CacheNames.SHORT_LINK_CACHE_PREFIX;

    @Value("${short.link.app.base.url}")
    private String portalBaseUrl;


    @Transactional
    public ShortLink createShortLink(CreateShortLinkRequest request) {
        // Idempotency check: look for an existing link with the same type, businessId, and channel
        Optional<ShortLink> existing = repository.findByTypeAndBusinessIdAndChannel(
                request.getType(), request.getBusinessId(), request.getChannel());

        if (existing.isPresent()) {
            log.info("Returning existing short link for {} : {} : {}",
                    request.getType(), request.getBusinessId(), request.getChannel());
            return existing.get();
        }

        ShortLink link = ShortLink.builder()
                .type(request.getType())
                .prefix(request.getType().getPrefix())
                .businessId(request.getBusinessId())
                .channel(request.getChannel())
                .expiresAt(isPermanentType(request.getType()) ? null : request.getExpiresAt())
                .targetTemplate(request.getLink())
                .code("") // Temporary
                .build();

        link = repository.save(link);

        // Generate code based on ID to ensure uniqueness and Base62 representation
        String code = Base62Util.encode(link.getId());
        link.setCode(code);
        repository.save(link);

        // Cache only the resolved target URL
        cacheTargetUrl(link);

        return link;
    }

    /**
     * Returns the resolved target URL for the given short link prefix+code.
     * Checks Redis first (stores only the URL string); falls back to the DB on cache miss.
     *
     * @return the target URL string, or {@link Optional#empty()} if the link does not exist.
     */
    public Optional<String> getTargetUrl(String prefix, String code) {
        String cacheKey = prefix + ":" + code;
        Cache cache = getShortLinkCache();
        Cache.ValueWrapper wrapper = cache.get(cacheKey);

        if (wrapper != null) {
            String cachedUrl = (String) wrapper.get();
            if (cachedUrl != null) {
                log.debug("Cache hit for short link key '{}'", cacheKey);
                return Optional.of(cachedUrl);
            }
        }

        log.debug("Cache miss for short link key '{}' – falling back to DB", cacheKey);
        Optional<ShortLink> dbLink = repository.findByPrefixAndCode(prefix, code);
        dbLink.ifPresent(this::cacheTargetUrl);

        return dbLink.map(this::buildTargetUrl);
    }

    /**
     * Loads the full {@link ShortLink} entity directly from the DB.
     * Use this when you need the complete entity (e.g. expiry check, click recording).
     */
    public Optional<ShortLink> getLinkFromDb(String prefix, String code) {
        return repository.findByPrefixAndCode(prefix, code);
    }

    @Async
    @Transactional
    public void recordClick(ShortLink shortLink, String userAgent, String remoteAddr, String referrer, String targetUrl) {
        ShortLinkClicksIndex click = ShortLinkClicksIndex.builder()
                .shortLinkId(shortLink.getId())
                .userAgent(userAgent)
                .ipHash(hashIp(remoteAddr))
                .referrer(referrer)
                .channel(shortLink.getChannel())
                .clickedAt(LocalDateTime.now())
                .clickedUrl(targetUrl)
                .build();

//        clickRepository.save(click);

        // Update last accessed at
        shortLink.setLastAccessedAt(LocalDateTime.now());
        repository.save(shortLink);
    }

    public String buildTargetUrl(ShortLink link) {
        String url = portalBaseUrl + buildPortalUrl(link);
        return appendUtmParams(url);
    }

    private String buildPortalUrl(ShortLink link) {
        if (link.getType() == LinkType.CUSTOM) {
            return link.getBusinessId();
        }
        // Simple template replacement: {id} -> businessId
        String template = link.getTargetTemplate();
        if (template == null) return "/";
        return template.replace("{id}", link.getBusinessId());
    }

    private String appendUtmParams(String url) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.fromUriString(url);

        return builder.toUriString();
    }

    private String hashIp(String ip) {
        // Simple mock for IP hashing as requested for PII protection
        return Integer.toHexString(ip.hashCode());
    }

    private boolean isPermanentType(LinkType type) {
        return type == LinkType.ORDER ||
                type == LinkType.SHIPMENT ||
                type == LinkType.LEDGER ||
                type == LinkType.INVOICE;
    }

    /**
     * Stores only the resolved target URL string in Redis.
     * This keeps the cache payload minimal – no serialized entity, no type metadata.
     */
    private void cacheTargetUrl(ShortLink link) {
        Cache cache = getShortLinkCache();
        String cacheKey = link.getPrefix() + ":" + link.getCode();

        cache.put(cacheKey, link.getTargetTemplate());
        log.debug("Cached target URL for key '{}': {}", cacheKey, link.getTargetTemplate());
    }

    private Cache getShortLinkCache() {
        Cache cache = cacheManager.getCache(getCacheNameWithProfile(commerceValueConfig.getRedisCacheProfile(), PREFIX));
        if (cache == null) {
            log.error("Short Link - Cache '{}' not found in CacheConfig", PREFIX);
            throw new IllegalStateException("Cache not configured: " + PREFIX);
        }
        return cache;
    }

}
