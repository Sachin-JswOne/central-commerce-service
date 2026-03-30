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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
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

    private static final Map<LinkType, String> TEMPLATES = Map.of(
            LinkType.LEDGER, "/ledger?tab=usable-ledger-balance&customerId={id}",
            LinkType.INVOICE, "/invoices?invoiceId={id}",
            LinkType.SHIPMENT, "/shipment/{id}?view=details",
            LinkType.ORDER, "/orders/{id}",
            LinkType.DOWNLOAD, "/download/{id}"
    );

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
//                .utmSource(request.getUtmSource())
//                .utmMedium(request.getUtmMedium())
//                .utmCampaign(request.getUtmCampaign())
//                .utmContent(request.getUtmContent())
                .targetTemplate(request.getLink())
                .code("") // Temporary
                .build();

        link = repository.save(link);

        // Generate code based on ID to ensure uniqueness and Base62 representation
        String code = Base62Util.encode(link.getId());
        link.setCode(code);
        repository.save(link);

        // Cache the link metadata
        cacheLink(link);

        return link;
    }

    public Optional<ShortLink> getLink(String prefix, String code) {
        String cacheKey = prefix + ":" + code;
        Cache cache = getShortLinkCache();
        Cache.ValueWrapper wrapper = cache.get(cacheKey);

        ShortLink cachedLink = (ShortLink) wrapper.get();

        if (cachedLink != null) {
            log.debug("Cache hit for {}", cacheKey);
            return Optional.of(cachedLink);
        }

        log.debug("Cache miss for {}", cacheKey);
        Optional<ShortLink> dbLink = repository.findByPrefixAndCode(prefix, code);
        dbLink.ifPresent(this::cacheLink);

        return dbLink;
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
        return appendUtmParams(url, link);
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

    private String appendUtmParams(String url, ShortLink link) {
        org.springframework.web.util.UriComponentsBuilder builder =
                org.springframework.web.util.UriComponentsBuilder.fromUriString(url);

//        if (link.getUtmSource() != null) builder.queryParam("utm_source", link.getUtmSource());
//        if (link.getUtmMedium() != null) builder.queryParam("utm_medium", link.getUtmMedium());
//        if (link.getUtmCampaign() != null) builder.queryParam("utm_campaign", link.getUtmCampaign());
//        if (link.getUtmContent() != null) builder.queryParam("utm_content", link.getUtmContent());

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

    private void cacheLink(ShortLink link) {

        Cache cache = getShortLinkCache();

        String cacheKey = link.getPrefix() + ":" + link.getCode();

        // If it's a download link, set TTL based on expiry
        if (link.getType() == LinkType.DOWNLOAD && link.getExpiresAt() != null) {
            Duration ttl = Duration.between(LocalDateTime.now(), link.getExpiresAt());
            if (ttl.getSeconds() > 0) {
                cache.put(cacheKey, link);
//                redisTemplate.opsForValue().set(cacheKey, link, ttl);
            }
        } else {
            // Permanent links or default TTL (e.g., 30 days)
            cache.put(cacheKey, link);
//            redisTemplate.opsForValue().set(cacheKey, link, Duration.ofDays(30));
        }
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
