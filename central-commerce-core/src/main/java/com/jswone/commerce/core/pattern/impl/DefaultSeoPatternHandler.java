package com.jswone.commerce.core.pattern.impl;

import com.jswone.commerce.core.enums.seo.CategoryType;
import com.jswone.commerce.core.enums.seo.SeoEntityType;
import com.jswone.commerce.core.enums.seo.SeoOperationType;
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
import com.jswone.commerce.core.util.CatalogueUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

import static com.jswone.commerce.core.constants.BuyAgainConstants.LOCALE_EN_US;
import static com.jswone.commerce.core.service.impl.CentralCatalogueServiceImpl.STOREFRONT_MSME;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultSeoPatternHandler implements SeoPatternHandler {

    private final UrlTemplateResolver templateResolver;
    private final CentralCatalogueClient catalogueClient;

    @Override
    public SeoData fetchData(SeoContext context) {

        if (context.getOperationType() == SeoOperationType.URL_GENERATION) {
            return null; // No data needed for URL generation
        }

        try {
            String title = null;
            String image = null;
            Map<String, String> defaultSelectedAttributes = null;

            // ---------------- CATEGORY ----------------
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

            // ---------------- PRODUCT ----------------
            else if (context.getEntityType() == SeoEntityType.PRODUCT
                    && context.getSlug() != null) {

                Product product = fetchProductBySlug(context.getSlug());

                if (product != null && product.getAttributes() != null) {
                    title = CatalogueUtil.str(
                            product.getAttributes().get("product_title"));
                    image = CatalogueUtil.extractImage(product);
                }
            }

            // ---------------- VARIANT ----------------
            else if (context.getEntityType() == SeoEntityType.VARIANT
                    && context.getVariantMmid() != null) {

                Product product = fetchProductByVariantMmid(
                        context.getVariantMmid());

                if (product != null && product.getAttributes() != null) {
                    title = CatalogueUtil.str(
                            product.getAttributes().get("product_title"));

                    // Find the matched variant
                    if (product.getVariants() != null) {
                        var matchedVariant = product.getVariants().stream()
                                .filter(v -> context.getVariantMmid().equals(v.getVariantMmid()))
                                .findFirst()
                                .orElse(null);

                        if (matchedVariant != null) {
                            image = CatalogueUtil.extractImage(product);
                            // TODO: Extract defaultSelectedAttributes from matchedVariant
                            // defaultSelectedAttributes = extract from matchedVariant.getAttributes()
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
            log.warn(
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
            throw new IllegalArgumentException("Invalid variant MMID: " + variantMmid);
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

        String title;
        String description;

        // data

        String entityTitle = getTitle(ctx, data);

        // Generate title and description based on location
        if (ctx.getLocation() != null && !ctx.getLocation().isEmpty()) {
            // Location-based templates
            // Title: "Buy {title} online in {location} | JSW One MSME"
            title = String.format("Buy %s online in %s | JSW One MSME",
                    entityTitle,
                    formatLocation(ctx.getLocation()));

            // Description: "Shop for {title} online in {location} on JSW One MSME.
            // Available online, best prices assured."
            description = String.format(
                    "Shop for %s online in %s on JSW One MSME. Available online, best prices assured.",
                    entityTitle,
                    formatLocation(ctx.getLocation()));
        } else {
            // Base case - simple title and description
            title = entityTitle + " | JSW One MSME";
            description = "Shop for " + entityTitle + " on JSW One MSME. Available online, best prices assured.";
        }

        String canonical = templateResolver.productBase();

        // Get og:image
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
     * Format location name - capitalize first letter of each word
     */
    private String formatLocation(String location) {
        if (location == null || location.isEmpty()) {
            return "";
        }
        // Simple capitalization for display
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
