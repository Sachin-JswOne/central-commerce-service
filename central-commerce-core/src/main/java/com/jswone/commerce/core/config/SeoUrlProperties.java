package com.jswone.commerce.core.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "seo.url")
public class SeoUrlProperties {

    private final Category category;
    private final Brand brand;
    private final Product product;
    private final MetadataTemplates metadata;

    public SeoUrlProperties(Category category, Brand brand, Product product, MetadataTemplates metadata) {
        this.category = category;
        this.brand = brand;
        this.product = product;
        this.metadata = metadata != null ? metadata : new MetadataTemplates(null, null, null);
    }

    @Getter
    public static class Category {
        private final String base;
        private final String location;

        public Category(String base, String location) {
            this.base = base;
            this.location = location;
        }
    }

    @Getter
    public static class Brand {
        private final String base;
        private final String location;

        public Brand(String base, String location) {
            this.base = base;
            this.location = location;
        }
    }

    @Getter
    public static class Product {
        private final String base;
        private final String location;
        private final String variant;

        public Product(String base, String location, String variant) {
            this.base = base;
            this.location = location;
            this.variant = variant;
        }
    }

    @Getter
    public static class MetadataTemplates {
        private final CategoryTemplate category;
        private final ProductTemplate product;
        private final VariantTemplate variant;

        public MetadataTemplates(CategoryTemplate category, ProductTemplate product, VariantTemplate variant) {
            this.category = category != null ? category : new CategoryTemplate(null, null);
            this.product = product != null ? product : new ProductTemplate(null, null);
            this.variant = variant != null ? variant : new VariantTemplate(null, null);
        }

        @Getter
        public static class CategoryTemplate {
            private final String title;
            private final String description;

            public CategoryTemplate(String title, String description) {
                this.title = title != null ? title : "{categoryName} | Buy Online at Best Prices";
                this.description = description != null ? description
                        : "Shop {categoryName} at {location}. Best deals and quality products.";
            }
        }

        @Getter
        public static class ProductTemplate {
            private final String title;
            private final String description;

            public ProductTemplate(String title, String description) {
                this.title = title != null ? title : "{productName} | {location} | JSW One";
                this.description = description != null ? description
                        : "Buy {productName} in {location}. Free shipping available.";
            }
        }

        @Getter
        public static class VariantTemplate {
            private final String title;
            private final String description;

            public VariantTemplate(String title, String description) {
                this.title = title != null ? title : "{productName} - {variantAttributes} | {location}";
                this.description = description != null ? description
                        : "Buy {productName} ({variantAttributes}) in {location}.";
            }
        }
    }
}