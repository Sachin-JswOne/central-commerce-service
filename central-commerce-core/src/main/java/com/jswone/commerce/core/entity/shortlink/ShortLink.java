package com.jswone.commerce.core.entity.shortlink;

import com.google.cloud.spring.data.datastore.core.mapping.Entity;
import com.jswone.commerce.core.enums.shortlink.LinkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShortLink {

    private Long id;

    private String prefix; // ld, iv, sh, or, dl

    private String code; // Base62 string

    private LinkType type; // LEDGER, INVOICE, SHIPMENT, ORDER, DOWNLOAD

    private String businessId;

    private String channel; // WHATSAPP, EMAIL, SMS

    private String targetTemplate;

//    private String utmSource;
//
//    private String utmMedium;
//
//    private String utmCampaign;
//
//    private String utmContent;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastAccessedAt;

}
