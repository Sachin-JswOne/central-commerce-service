package com.jswone.commerce.core.util;

import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@UtilityClass
public class SitemapGenerator {

    private static final String SITEMAP_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">";
    private static final String SITEMAP_FOOTER = "</urlset>";
    private static final String SITEMAP_INDEX_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">";
    private static final String SITEMAP_INDEX_FOOTER = "</sitemapindex>";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.of("UTC"));

    public static String generateSitemapXmlFromMeta(
            List<? extends com.jswone.commerce.core.model.seo.UrlMeta> urlMetas) {
        StringBuilder xml = new StringBuilder(SITEMAP_HEADER);
        for (com.jswone.commerce.core.model.seo.UrlMeta meta : urlMetas) {
            xml.append("<url><loc>").append(meta.getUrl()).append("</loc>");
            if (meta.getLastMod() != null) {
                xml.append("<lastmod>").append(DATE_FORMATTER.format(meta.getLastMod())).append("</lastmod>");
            }
            xml.append("<priority>").append(meta.getPriority()).append("</priority>");
            xml.append("</url>");
        }
        xml.append(SITEMAP_FOOTER);
        return xml.toString();
    }

    public static String generateSitemapIndexXml(List<String> sitemapUrls) {
        String lastModStr = DATE_FORMATTER.format(Instant.now());
        StringBuilder xml = new StringBuilder(SITEMAP_INDEX_HEADER);
        for (String url : sitemapUrls) {
            xml.append("<sitemap><loc>").append(url).append("</loc>");
            xml.append("<lastmod>").append(lastModStr).append("</lastmod></sitemap>");
        }
        xml.append(SITEMAP_INDEX_FOOTER);
        return xml.toString();
    }
}
