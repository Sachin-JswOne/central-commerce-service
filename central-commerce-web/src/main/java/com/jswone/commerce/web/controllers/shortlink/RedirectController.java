package com.jswone.commerce.web.controllers.shortlink;

import com.jswone.commerce.core.entity.shortlink.ShortLink;
import com.jswone.commerce.core.service.shortlink.ShortLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@Slf4j
@RequiredArgsConstructor
public class RedirectController {

    private final ShortLinkService shortLinkService;

    @GetMapping("/sl/{prefix}/{code}")
    public void handleRedirect(
            @PathVariable String prefix,
            @PathVariable String code,
            @RequestParam(required = false) String channel,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // Fast path: resolve the target URL from cache (stores only the URL string)
        Optional<String> targetUrlOpt = shortLinkService.getTargetUrl(prefix, code);

        if (targetUrlOpt.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // We still need the full entity for the expiry check and async click recording.
        // This is a DB call only on cache-miss paths or when we need entity-level data.
        Optional<ShortLink> shortLinkOpt = shortLinkService.getLinkFromDb(prefix, code);

        if (shortLinkOpt.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        ShortLink shortLink = shortLinkOpt.get();

        if (shortLink.getExpiresAt() != null && shortLink.getExpiresAt().isBefore(LocalDateTime.now())) {
            response.sendError(HttpServletResponse.SC_GONE, "Link has expired");
            return;
        }

        String targetUrl = targetUrlOpt.get();

        // Record click asynchronously
        String userAgent = request.getHeader("User-Agent");
        String remoteAddr = request.getRemoteAddr();
        String referrer = request.getHeader("Referer");
        shortLinkService.recordClick(shortLink, userAgent, remoteAddr, referrer, targetUrl);

        log.info("Redirecting to: {} (UA: {})", targetUrl, userAgent);

        response.sendRedirect(targetUrl);
    }
}
