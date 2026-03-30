package com.jswone.commerce.web.controllers.shortlink;

import com.jswone.commerce.core.entity.shortlink.ShortLink;
import com.jswone.commerce.core.service.shortlink.ShortLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@Slf4j
@RequiredArgsConstructor
public class RedirectController {

    private final ShortLinkService shortLinkService;

    @Value("${short.link.app.base.url}")
    private String portalBaseUrl;

    @GetMapping("/sl/{prefix}/{code}")
    public void handleRedirect(
            @PathVariable String prefix,
            @PathVariable String code,
            @RequestParam(required = false) String channel,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        Optional<ShortLink> shortLinkOpt = shortLinkService.getLink(prefix, code);

        if (shortLinkOpt.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        ShortLink shortLink = shortLinkOpt.get();

        if (shortLink.getExpiresAt() != null && shortLink.getExpiresAt().isBefore(LocalDateTime.now())) {
            response.sendError(HttpServletResponse.SC_GONE, "Link has expired");
            return;
        }

        String targetUrl = shortLink.getTargetTemplate();
        String portalPath = shortLinkService.buildTargetUrl(shortLink).replace(portalBaseUrl, "");

        // Record click asynchronously
        String userAgent = request.getHeader("User-Agent");
        String remoteAddr = request.getRemoteAddr();
        String referrer = request.getHeader("Referer");
        shortLinkService.recordClick(shortLink, userAgent, remoteAddr, referrer, targetUrl);

        log.info("Redirecting to: {} (UA: {})", targetUrl, userAgent);

        response.sendRedirect(targetUrl);
    }
}
