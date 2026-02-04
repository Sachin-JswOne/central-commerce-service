package com.jswone.commerce.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "seo.url")
public class SeoUrlProperties {

    private Category category = new Category();
    private Brand brand = new Brand();
    private Product product = new Product();

    @Getter
    @Setter
    public static class Category {
        private String base;
        private String location;
    }

    @Getter
    @Setter
    public static class Brand {
        private String base;
        private String location;
    }

    @Getter
    @Setter
    public static class Product {
        private String base;
        private String location;
        private String variant;
    }
}