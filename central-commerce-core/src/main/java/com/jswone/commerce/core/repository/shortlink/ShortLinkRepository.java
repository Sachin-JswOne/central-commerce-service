package com.jswone.commerce.core.repository.shortlink;

import com.jswone.commerce.core.entity.shortlink.ShortLink;
import com.jswone.commerce.core.enums.shortlink.LinkType;
import com.jswone.commerce.core.mapper.PurchasedSkuRowMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class ShortLinkRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ShortLinkRepository(
            @Qualifier("centralCommerceJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);

    }

    private final RowMapper<ShortLink> rowMapper = new RowMapper<ShortLink>() {
        @Override
        public ShortLink mapRow(ResultSet rs, int rowNum) throws SQLException {
            return ShortLink.builder()
                    .id(rs.getLong("id"))
                    .prefix(rs.getString("prefix"))
                    .code(rs.getString("code"))
                    .type(LinkType.valueOf(rs.getString("type")))
                    .businessId(rs.getString("business_id"))
                    .channel(rs.getString("channel"))
                    .targetTemplate(rs.getString("target_template"))
                    .expiresAt(getLocalDateTime(rs, "expires_at"))
                    .createdAt(getLocalDateTime(rs, "created_at"))
                    .updatedAt(getLocalDateTime(rs, "updated_at"))
                    .lastAccessedAt(getLocalDateTime(rs, "last_accessed_at"))
                    .build();
        }
    };

    private LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        java.sql.Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }

    public ShortLink save(ShortLink link) {
        if (link.getId() == null) {
            return insert(link);
        } else {
            return update(link);
        }
    }

    private ShortLink insert(ShortLink link) {
        String sql = "INSERT INTO short_links (prefix, code, type, business_id, channel, target_template, " +
                "expires_at, created_at, updated_at) " +
                "VALUES (:prefix, :code, :type, :businessId, :channel, :targetTemplate, " +
                ":expiresAt, :createdAt, :updatedAt)";

        LocalDateTime now = LocalDateTime.now();
        link.setCreatedAt(now);
        link.setUpdatedAt(now);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("prefix", link.getPrefix())
                .addValue("code", link.getCode())
                .addValue("type", link.getType().name())
                .addValue("businessId", link.getBusinessId())
                .addValue("channel", link.getChannel())
                .addValue("targetTemplate", link.getTargetTemplate())
                .addValue("expiresAt", link.getExpiresAt())
                .addValue("createdAt", link.getCreatedAt())
                .addValue("updatedAt", link.getUpdatedAt());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});

        Number key = keyHolder.getKey();
        if (key != null) {
            link.setId(key.longValue());
        }
        return link;
    }

    private ShortLink update(ShortLink link) {
        String sql = "UPDATE short_links SET prefix = :prefix, code = :code, type = :type, " +
                "business_id = :businessId, channel = :channel, target_template = :targetTemplate, " +
                "expires_at = :expiresAt, updated_at = :updatedAt, " +
                "last_accessed_at = :lastAccessedAt WHERE id = :id";

        link.setUpdatedAt(LocalDateTime.now());

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", link.getId())
                .addValue("prefix", link.getPrefix())
                .addValue("code", link.getCode())
                .addValue("type", link.getType().name())
                .addValue("businessId", link.getBusinessId())
                .addValue("channel", link.getChannel())
                .addValue("targetTemplate", link.getTargetTemplate())
                .addValue("expiresAt", link.getExpiresAt())
                .addValue("updatedAt", link.getUpdatedAt())
                .addValue("lastAccessedAt", link.getLastAccessedAt());

        jdbcTemplate.update(sql, params);
        return link;
    }

    public Optional<ShortLink> findByPrefixAndCode(String prefix, String code) {
        String sql = "SELECT * FROM short_links WHERE prefix = ? AND code = ?";

        return jdbcTemplate.getJdbcOperations().query(sql, rowMapper, prefix, code).stream().findFirst();
    }

    public Optional<ShortLink> findByTypeAndBusinessIdAndChannel(LinkType type, String businessId, String channel) {
        String sql = "SELECT * FROM short_links WHERE type = ? AND business_id = ? AND channel = ?";

        return jdbcTemplate.getJdbcOperations().query(sql, rowMapper,type.name(),businessId,channel).stream().findFirst();
    }

}
