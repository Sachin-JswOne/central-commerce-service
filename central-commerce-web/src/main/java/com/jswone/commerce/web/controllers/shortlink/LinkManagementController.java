package com.jswone.commerce.web.controllers.shortlink;

import com.jswone.commerce.core.entity.shortlink.ShortLink;
import com.jswone.commerce.core.model.request.shortlink.CreateShortLinkRequest;
import com.jswone.commerce.core.service.shortlink.ShortLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/links")
public class LinkManagementController {

    private final ShortLinkService shortLinkService;

    @Value("${short.link.app.base.url}")
    private String baseUrl;

    @PostMapping
    public ResponseEntity<ShortLinkResponse> createShortLink(@Valid @RequestBody CreateShortLinkRequest request) {
        ShortLink link = shortLinkService.createShortLink(request);
        return ResponseEntity.ok(new ShortLinkResponse(link, baseUrl));
    }

    @lombok.Data
    public static class ShortLinkResponse {
        private String shortUrl;
        private String code;
        private String prefix;

        public ShortLinkResponse(ShortLink link, String baseUrl) {
            this.code = link.getCode();
            this.prefix = link.getPrefix();
            this.shortUrl = baseUrl + "/" + link.getPrefix() + "/" + link.getCode();
        }
    }

}
