package com.jswone.commerce.core.pattern.impl;

import com.jswone.commerce.core.config.SeoUrlProperties;
import com.jswone.commerce.core.enums.seo.CategoryType;
import com.jswone.commerce.core.enums.seo.SeoEntityType;
import com.jswone.commerce.core.enums.seo.SeoOperationType;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.CatalogueBreadCrumbData;
import com.jswone.commerce.core.model.centralCatalogue.Product;
import com.jswone.commerce.core.model.request.ProductBulkRequest;
import com.jswone.commerce.core.model.response.centralCatalogue.ProductBulkResponse;
import com.jswone.commerce.core.model.seo.SeoContext;
import com.jswone.commerce.core.model.seo.SeoData;
import com.jswone.commerce.core.model.seo.SeoMeta;
import com.jswone.commerce.core.model.seo.UrlMeta;
import com.jswone.commerce.core.pattern.SeoPatternHandler;
import com.jswone.commerce.core.rest.CentralCatalogueClient;
import com.jswone.commerce.core.template.UrlTemplateResolver;
import com.jswone.commerce.core.constants.SeoConstants;
import com.jswone.commerce.core.util.CatalogueUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

import static com.jswone.commerce.core.constants.SeoConstants.LOCALE_EN_US;
import static com.jswone.commerce.core.constants.SeoConstants.STOREFRONT_MSME;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultSeoPatternHandler implements SeoPatternHandler {

    private final UrlTemplateResolver templateResolver;
    private final CentralCatalogueClient catalogueClient;
    private final SeoUrlProperties seoUrlProperties;

    @Override
    public SeoData fetchData(SeoContext context) {

        if (context.getOperationType() == SeoOperationType.URL_GENERATION) {
            return null;
        }

        try {
            String title = null;
            String image = null;
            Map<String, String> defaultSelectedAttributes = null;

            if (context.getEntityType() == SeoEntityType.CATEGORY
                    && context.getCategoryId() != null
                    && context.getSlug() != null) {

                CatalogueBreadCrumbData breadcrumb = catalogueClient.getBreadcrumb(
                        context.getCategoryId(),
                        context.getSlug());

                if (breadcrumb != null) {

                    var matchedBread = breadcrumb.getBread_crumb_details()
                            .stream()
                            .filter(bread -> bread.getAttributes() != null
                                    && bread.getAttributes().getSlug() != null
                                    && bread.getAttributes().getSlug()
                                            .equalsIgnoreCase(context.getSlug()))
                            .findFirst()
                            .orElse(null);

                    if (matchedBread != null) {
                        title = matchedBread.getAttributes().getCategory_title();

                        if (matchedBread.getAttributes().getMeta_image() != null) {
                            image = matchedBread.getAttributes()
                                    .getMeta_image()
                                    .getPublic_url();
                        }
                    }
                }
            }

            else if (context.getEntityType() == SeoEntityType.PRODUCT
                    && context.getSlug() != null) {

                Product product = fetchProductBySlug(context.getSlug());

                if (product != null && product.getAttributes() != null) {
                    title = CatalogueUtil.str(
                            product.getAttributes().get(SeoConstants.ATTR_PRODUCT_TITLE));
                    image = CatalogueUtil.extractImage(product);
                }
            }

            else if (context.getEntityType() == SeoEntityType.VARIANT
                    && context.getVariantMmid() != null) {

                Product product = fetchProductByVariantMmid(
                        context.getVariantMmid());

                if (product != null && product.getAttributes() != null) {
                    title = CatalogueUtil.str(
                            product.getAttributes().get(SeoConstants.ATTR_PRODUCT_TITLE));

                    // Find the matched variant
                    if (product.getVariants() != null) {
                        var matchedVariant = product.getVariants().stream()
                                .filter(v -> context.getVariantMmid().equals(v.getVariantMmid()))
                                .findFirst()
                                .orElse(null);

                        if (matchedVariant != null) {
                            image = CatalogueUtil.extractImage(product);
                            // TODO: Extract defaultSelectedAttributes from matchedVariant
                        }
                    }
                }
            }

            return SeoData.builder()
                    .title(title)
                    .image(image)
                    .defaultSelectedAttributes(defaultSelectedAttributes)
                    .build();

        } catch (Exception e) {
            log.error(
                    "SEO data fetch failed | entityType={} | slug={} | categoryId={} | variantMmid={}",
                    context.getEntityType(),
                    context.getSlug(),
                    context.getCategoryId(),
                    context.getVariantMmid(),
                    e);
            return null;
        }
    }

    private Product fetchProductBySlug(String slug) {

        ProductBulkResponse response = catalogueClient.getProductFromSlug(slug, STOREFRONT_MSME);

        if (response == null || response.getProducts().isEmpty()) {
            return null;
        }

        return response.getProducts().getFirst();
    }

    private Product fetchProductByVariantMmid(String variantMmid) {

        String productMmid = extractProductMmid(variantMmid);

        ProductBulkResponse response = catalogueClient.bulkMMIDResponse(
                new ProductBulkRequest(Set.of(productMmid),
                        STOREFRONT_MSME,
                        LOCALE_EN_US));

        if (response == null || response.getProducts().isEmpty()) {
            return null;
        }

        return response.getProducts().getFirst();
    }

    private String extractProductMmid(String variantMmid) {

        String[] parts = variantMmid.split("-");

        if (parts.length < 2) {
            throw new CentralCommerceServiceException("Invalid variant MMID: " + variantMmid);
        }

        return parts[0] + "-" + parts[1];
    }

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
                        entityTitle, location, data, ctx);
                description = buildFromTemplate(
                        seoUrlProperties.getMetadata().getCategory().getDescription(),
                        entityTitle, location, data, ctx);
                break;

            case VARIANT:
                String variantAttrs = extractVariantAttributes(data);
                title = buildFromTemplate(
                        seoUrlProperties.getMetadata().getVariant().getTitle(),
                        entityTitle, location, data, ctx)
                        .replace(SeoConstants.PLACEHOLDER_VARIANT_ATTRIBUTES, variantAttrs);
                description = buildFromTemplate(
                        seoUrlProperties.getMetadata().getVariant().getDescription(),
                        entityTitle, location, data, ctx)
                        .replace(SeoConstants.PLACEHOLDER_VARIANT_ATTRIBUTES, variantAttrs);
                break;

            case PRODUCT:
            default:
                title = buildFromTemplate(
                        seoUrlProperties.getMetadata().getProduct().getTitle(),
                        entityTitle, location, data, ctx);
                description = buildFromTemplate(
                        seoUrlProperties.getMetadata().getProduct().getDescription(),
                        entityTitle, location, data, ctx);
                break;
        }

        String canonical = templateResolver.productBase();
        String ogImage = getImage(data);

        return new SeoMeta(
                title,
                description,
                canonical,
                title,
                ogType(ctx),
                canonical,
                ogImage,
                description);
    }

    /**
     * Build metadata string from template by replacing placeholders
     */
    private String buildFromTemplate(String template, String entityTitle, String location, SeoData data,
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
     * TODO: Enhance with actual variant attribute extraction when available
     */
    private String extractVariantAttributes(SeoData data) {

        return "";
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
            return ctx.getCategoryType() == CategoryType.BRAND
                    ? "brand"
                    : "category";
        }
        return "product";
    }

    private double priority(SeoContext ctx) {
        return ctx.getEntityType() == SeoEntityType.CATEGORY ? 0.6 : 0.8;
    }
}
