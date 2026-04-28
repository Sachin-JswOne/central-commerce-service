package com.jswone.commerce.core.repository.ai;

import com.jswone.commerce.core.entity.ai.PromptTemplate;
import com.jswone.commerce.core.enums.ai.PromptType;
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
import java.util.List;
import java.util.Optional;

@Repository
public class PromptTemplateRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PromptTemplateRepository(@Qualifier("centralCommerceJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    private final RowMapper<PromptTemplate> rowMapper = new RowMapper<>() {
        @Override
        public PromptTemplate mapRow(ResultSet rs, int rowNum) throws SQLException {
            return PromptTemplate.builder()
                    .id(rs.getLong("id"))
                    .promptType(PromptType.fromValue(rs.getString("prompt_type")))
                    .version(rs.getInt("version"))
                    .encodedPrompt(rs.getString("encoded_prompt"))
                    .encoding(rs.getString("encoding"))
                    .active(rs.getBoolean("is_active"))
                    .createdBy(rs.getString("created_by"))
                    .description(rs.getString("description"))
                    .createdAt(getLocalDateTime(rs, "created_at"))
                    .updatedAt(getLocalDateTime(rs, "updated_at"))
                    .build();
        }
    };

    public PromptTemplate save(PromptTemplate promptTemplate) {
        String sql = "INSERT INTO ai_prompts (prompt_type, version, encoded_prompt, encoding, is_active, " +
                "created_by, description, created_at, updated_at) VALUES (:promptType, :version, :encodedPrompt, " +
                ":encoding, :active, :createdBy, :description, :createdAt, :updatedAt)";

        LocalDateTime now = LocalDateTime.now();
        promptTemplate.setCreatedAt(now);
        promptTemplate.setUpdatedAt(now);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("promptType", promptTemplate.getPromptType().getValue())
                .addValue("version", promptTemplate.getVersion())
                .addValue("encodedPrompt", promptTemplate.getEncodedPrompt())
                .addValue("encoding", promptTemplate.getEncoding())
                .addValue("active", promptTemplate.getActive())
                .addValue("createdBy", promptTemplate.getCreatedBy())
                .addValue("description", promptTemplate.getDescription())
                .addValue("createdAt", promptTemplate.getCreatedAt())
                .addValue("updatedAt", promptTemplate.getUpdatedAt());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});

        Number key = keyHolder.getKey();
        if (key != null) {
            promptTemplate.setId(key.longValue());
        }

        return promptTemplate;
    }

    public Optional<PromptTemplate> findActiveByType(PromptType promptType) {
        String sql = "SELECT * FROM ai_prompts WHERE prompt_type = ? AND is_active = true " +
                "ORDER BY version DESC LIMIT 1";
        return jdbcTemplate.getJdbcOperations().query(sql, rowMapper, promptType.getValue()).stream().findFirst();
    }

    public Optional<PromptTemplate> findByTypeAndVersion(PromptType promptType, int version) {
        String sql = "SELECT * FROM ai_prompts WHERE prompt_type = ? AND version = ?";
        return jdbcTemplate.getJdbcOperations().query(sql, rowMapper, promptType.getValue(), version).stream().findFirst();
    }

    public List<PromptTemplate> findAllByType(PromptType promptType) {
        String sql = "SELECT * FROM ai_prompts WHERE prompt_type = ? ORDER BY version DESC";
        return jdbcTemplate.getJdbcOperations().query(sql, rowMapper, promptType.getValue());
    }

    public int findLatestVersion(PromptType promptType) {
        String sql = "SELECT COALESCE(MAX(version), 0) FROM ai_prompts WHERE prompt_type = :promptType";
        Integer latestVersion = jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("promptType", promptType.getValue()),
                Integer.class
        );
        return latestVersion == null ? 0 : latestVersion;
    }

    public void deactivateAllVersions(PromptType promptType) {
        String sql = "UPDATE ai_prompts SET is_active = false, updated_at = :updatedAt " +
                "WHERE prompt_type = :promptType AND is_active = true";
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("promptType", promptType.getValue())
                .addValue("updatedAt", LocalDateTime.now()));
    }

    public void activateVersion(PromptType promptType, int version) {
        String sql = "UPDATE ai_prompts SET is_active = true, updated_at = :updatedAt " +
                "WHERE prompt_type = :promptType AND version = :version";
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("promptType", promptType.getValue())
                .addValue("version", version)
                .addValue("updatedAt", LocalDateTime.now()));
    }

    public boolean existsByType(PromptType promptType) {
        String sql = "SELECT COUNT(1) FROM ai_prompts WHERE prompt_type = :promptType";
        Integer count = jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("promptType", promptType.getValue()),
                Integer.class
        );
        return count != null && count > 0;
    }

    private LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        java.sql.Timestamp timestamp = rs.getTimestamp(column);
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
