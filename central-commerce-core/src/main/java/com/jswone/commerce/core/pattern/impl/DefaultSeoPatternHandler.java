package com.jswone.commerce.core.pattern.impl;

import com.jswone.commerce.core.config.CommerceValueConfig;
import com.jswone.commerce.core.config.SeoUrlProperties;

import com.jswone.commerce.core.enums.seo.SeoEntityType;
import com.jswone.commerce.core.model.seo.SeoContext;
import com.jswone.commerce.core.model.seo.SeoData;
import com.jswone.commerce.core.model.seo.SeoMeta;
import com.jswone.commerce.core.model.seo.UrlMeta;
import com.jswone.commerce.core.pattern.SeoPatternHandler;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.template.UrlTemplateResolver;
import com.jswone.commerce.core.constants.SeoConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultSeoPatternHandler implements SeoPatternHandler {

    private final UrlTemplateResolver templateResolver;
    private final CentralCatalogueClient catalogueClient;
    private final SeoUrlProperties seoUrlProperties;
    private final CommerceValueConfig commerceValueConfig;

    /**
     * Get title from fetched SeoData
     */
    private String getTitle(SeoContext ctx, SeoData data) {
        if (data != null && data.getTitle() != null) {
            return data.getTitle();
        }
        // Fallback to slug
        return ctx.getSlug();
    }

    /**
     * Get image from fetched SeoData
     */
    private String getImage(SeoData data) {
        if (data != null && data.getImage() != null) {
            return data.getImage();
        }
        // No fallback for image
        return null;
    }

    @Override
    public UrlMeta generateUrl(SeoContext ctx) {

        String template;

        if (ctx.getEntityType() == SeoEntityType.CATEGORY) {
            template = ctx.getLocation() == null
                    ? templateResolver.categoryBase(ctx)
                    : templateResolver.categoryLocation(ctx);
        } else if (ctx.getEntityType() == SeoEntityType.PRODUCT) {
            template = ctx.getLocation() == null
                    ? templateResolver.productBase()
                    : templateResolver.productLocation();
        } else {
            template = templateResolver.variantLocation();
        }

        // Format location for URL: lowercase and replace spaces with hyphens
        String urlLocation = ctx.getLocation() != null
                ? ctx.getLocation().toLowerCase().replaceAll("\\s+", "-")
                : "";

        String url = template
                .replace("{slug}", ctx.getSlug())
                .replace("{location}", urlLocation)
                .replace("{variantMmid}", ctx.getVariantMmid() == null ? "" : ctx.getVariantMmid());

        // Use lastModifiedAt from context if available, otherwise use current time
        Instant lastMod = ctx.getLastModifiedAt() != null ? ctx.getLastModifiedAt() : Instant.now();

        return new UrlMeta(url, lastMod, priority(ctx));
    }

    @Override
    public SeoMeta generateMeta(SeoContext ctx, SeoData data) {

        String entityTitle = getTitle(ctx, data);
        String location = ctx.getLocation() != null ? formatLocation(ctx.getLocation()) : "";

        String title;
        String description;

        switch (ctx.getEntityType()) {
            case CATEGORY:
                title = buildFromTemplate(
                        seoUrlProperties.getMetadata().getCategory().getTitle(),
                        entityTitle, location, ctx);
                description = buildFromTemplate(
                        seoUrlProperties.getMetadata().getCategory().getDescription(),
                        entityTitle, location, ctx);
                break;

            case VARIANT:
                String variantAttrs = extractVariantAttributes(data);
                title = buildFromTemplate(
                        seoUrlProperties.getMetadata().getVariant().getTitle(),
                        entityTitle, location, ctx);

                // Only add variant attributes to description if they exist
                description = buildFromTemplate(
                        seoUrlProperties.getMetadata().getVariant().getDescription(),
                        entityTitle, location, ctx);

                // Replace placeholder - template already has parentheses if needed
                if (variantAttrs != null && !variantAttrs.isEmpty()) {
                    title = title.replace(SeoConstants.PLACEHOLDER_VARIANT_ATTRIBUTES, variantAttrs);
                    description = description.replace(SeoConstants.PLACEHOLDER_VARIANT_ATTRIBUTES, variantAttrs);
                } else {
                    // Remove placeholder completely if no attributes (including any surrounding
                    // parentheses)
                    title = title.replace(SeoConstants.PLACEHOLDER_VARIANT_ATTRIBUTES, "");
                    description = description
                            .replaceAll("\\(\\s*" + SeoConstants.PLACEHOLDER_VARIANT_ATTRIBUTES + "\\s*\\)", "")
                            .replace(SeoConstants.PLACEHOLDER_VARIANT_ATTRIBUTES, "")
                            .replaceAll("\\s+", " ")
                            .trim();
                }
                break;

            case PRODUCT:
            default:
                title = buildFromTemplate(
                        seoUrlProperties.getMetadata().getProduct().getTitle(),
                        entityTitle, location, ctx);
                description = buildFromTemplate(
                        seoUrlProperties.getMetadata().getProduct().getDescription(),
                        entityTitle, location, ctx);
                break;
        }

        String canonical = commerceValueConfig.getJoplMsmeWebUrl().concat(generateUrl(ctx).getUrl());
        String ogImage = getImage(data);

        return SeoMeta.builder()
                .canonical(canonical)
                .title(title)
                .description(description)
                .ogImage(ogImage)
                .ogUrl(canonical)
                .description(description)
                .ogImage(ogImage)
                .ogDescription(description)
                .ogTitle(title)
                .ogType(ogType(ctx))
                .build();
    }

    /**
     * Build metadata string from template by replacing placeholders
     */
    private String buildFromTemplate(String template, String entityTitle, String location,
            SeoContext ctx) {
        if (template == null || template.isEmpty()) {
            return entityTitle;
        }

        return template
                .replace(SeoConstants.PLACEHOLDER_CATEGORY_NAME, entityTitle)
                .replace(SeoConstants.PLACEHOLDER_PRODUCT_NAME, entityTitle)
                .replace(SeoConstants.PLACEHOLDER_LOCATION,
                        location != null && !location.isEmpty() ? location : SeoConstants.DEFAULT_LOCATION)
                .replace(SeoConstants.PLACEHOLDER_SLUG, ctx.getSlug() != null ? ctx.getSlug() : "");
    }

    /**
     * Extract variant attributes as a formatted string for metadata
     * Formats attributes like: "Length 3000mm, Thickness 16mm, Width 1500mm"
     */
    private String extractVariantAttributes(SeoData data) {
        if (data == null || data.getVariantSpec() == null || data.getVariantSpec().isEmpty()) {
            return "";
        }

        Map<String, Object> variantSpec = data.getVariantSpec();
        List<String> parts = new ArrayList<>();

        // Common order for variant attributes
        String[] preferredOrder = { "length", "thickness", "width", "diameter", "grade", "finish" };

        for (String key : preferredOrder) {
            if (variantSpec.containsKey(key)) {
                Object value = variantSpec.get(key);
                if (value != null) {
                    String displayName = capitalize(key);
                    String displayValue = formatValue(value);
                    parts.add(displayName + " " + displayValue);
                }
            }
        }

        // Add any remaining attributes not in the preferred order
        for (Map.Entry<String, Object> entry : variantSpec.entrySet()) {
            String key = entry.getKey();
            if (!Arrays.asList(preferredOrder).contains(key.toLowerCase()) && entry.getValue() != null) {
                String displayName = capitalize(key);
                String displayValue = formatValue(entry.getValue());
                parts.add(displayName + " " + displayValue);
            }
        }

        return String.join(", ", parts);
    }

    /**
     * Format value - remove trailing .0 for whole numbers
     */
    private String formatValue(Object value) {
        if (value instanceof Double) {
            Double d = (Double) value;
            if (d == d.intValue()) {
                return String.valueOf(d.intValue());
            }
            return String.valueOf(d);
        }
        return String.valueOf(value);
    }

    /**
     * Capitalize first letter of string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Format location name - capitalize first letter of each word
     */
    private String formatLocation(String location) {
        if (location == null || location.isEmpty()) {
            return "";
        }
        return location.substring(0, 1).toUpperCase() + location.substring(1);
    }

    private String ogType(SeoContext ctx) {
        if (ctx.getEntityType() == SeoEntityType.CATEGORY) {
            return "category";
        }
        return "product";
    }

    private double priority(SeoContext ctx) {
        return ctx.getEntityType() == SeoEntityType.CATEGORY ? 0.8 : 0.7;
    }
}
