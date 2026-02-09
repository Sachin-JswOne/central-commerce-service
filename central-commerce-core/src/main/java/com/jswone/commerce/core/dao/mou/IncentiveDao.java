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

    private static final String INCENTIVE_DETAILS = "incentiveDetails";
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
                    FROM mou_id_category_target_and_achievement_jsw_steel_mv
                    WHERE mou_id = ? AND financial_year = ? AND category = ? AND mou_type = ?
                """;

        try {
            String json = jdbcTemplate.query(sql, rs -> rs.next() ? rs.getString(1) : null,
                    mouId, financialYear, productCategory, mouType);

            if (json == null || json.isBlank()) {
                return objectMapper.createArrayNode();
            }

            JsonNode root = objectMapper.readTree(json);

            if (root.has(INCENTIVE_DETAILS)) {
                return root.get(INCENTIVE_DETAILS);
            }
            return root;
        } catch (Exception ex) {
            log.error("Failed to fetch incentive JSON for mouId={}, financialYear={}," +
                    "productCategory={} and mouType={}", mouId, productCategory, financialYear, mouType, ex);

            throw new CentralCommerceServiceException(
                    "Failed to fetch Mou incentive details",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
