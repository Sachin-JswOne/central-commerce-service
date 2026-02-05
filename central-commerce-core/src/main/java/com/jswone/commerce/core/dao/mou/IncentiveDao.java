package com.jswone.commerce.core.dao.mou;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class IncentiveDao {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public IncentiveDao(@Qualifier("mouJdbcTemplate") JdbcTemplate jdbcTemplate,
                        ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public JsonNode fetchIncentives(String mouId, String financialYear, String productCategory,
                                    String mouType) {

        String sql = """
                    SELECT incentive_details_json
                    FROM mou_id_category_target_and_achievement_jsw_steel
                    WHERE mou_id = ? AND category = ? AND mou_type = ?
                """;

        try {
            String json = jdbcTemplate.query(sql, rs -> rs.next() ? rs.getString(1) : null,
                    mouId, productCategory, mouType);

            if (json == null || json.isBlank()) {
                return objectMapper.createObjectNode();
            }

            return objectMapper.readTree(json);

        } catch (Exception ex) {
            log.error("Failed to fetch incentive JSON for mouId={}, category={}", mouId, productCategory, ex);

            throw new CentralCommerceServiceException("Failed to fetch Mou incentive details",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
