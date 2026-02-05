package com.jswone.commerce.core.dao.mou;

import com.jswone.commerce.core.exceptions.CentralCommerceServiceException;
import com.jswone.commerce.core.model.mou.MouYearlySummaryDTO;
import com.jswone.commerce.core.model.mou.ProductCategoryDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class MouDashboardDao {

    private final JdbcTemplate jdbcTemplate;

    public MouDashboardDao(@Qualifier("mouJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<MouYearlySummaryDTO> fetchMouYearlySummary(String mouId, String financialYear,
                                                               String mouType) {

        String sql = """
                    SELECT last_updated_at, uom, achieved_qty_perc, achieved_qty,
                           remaining_qty, target_qty,
                           total_savings, potential_savings
                    FROM yearly_mou_target_and_achievement_jsw_steel
                    WHERE mou_id = ? AND financial_year = ? AND mou_type = ?
                """;

        try {
            return jdbcTemplate.query(sql, rs -> {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(MouYearlySummaryDTO.builder()
                        .mouId(mouId)
                        .financialYear(financialYear)
                        .mouType(mouType)
                        .uom(rs.getString("uom"))
                        .lastUpdatedDate(rs.getTimestamp("last_updated_at")
                                .toLocalDateTime().toLocalDate().toString())
                        .achievementPercentage(rs.getBigDecimal("achieved_qty_perc"))
                        .achievedQuantity(rs.getBigDecimal("achieved_qty"))
                        .remainingQuantity(rs.getBigDecimal("remaining_qty"))
                        .targetQuantity(rs.getBigDecimal("target_qty"))
                        .savingsAchieved(rs.getBigDecimal("total_savings"))
                        .potentialSavings(rs.getBigDecimal("potential_savings"))
                        .build()
                );
            }, mouId, financialYear, mouType);

        } catch (Exception ex) {
            log.error("DB error fetching Mou yearly summary for mouId={}", mouId, ex);
            throw new CentralCommerceServiceException("Failed to fetch MOU yearly summary",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public List<ProductCategoryDTO> fetchProductCategories(String mouId, String financialYear,
                                                           String mouType) {

        String sql = """
                    SELECT uom, category, volume_incentive_mt, consistency_incentive_mt,
                           msme_incentive_mt, achieved_qty_perc, achieved_qty,
                           remaining_qty, target_qty, total_savings, potential_savings
                    FROM mou_id_category_target_and_achievement_jsw_steel
                    WHERE mou_id = ? AND financial_year = ? AND mou_type = ?
                """;

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) ->
                            ProductCategoryDTO.builder()
                                    .mouId(mouId)
                                    .financialYear(financialYear)
                                    .mouType(mouType)
                                    .uom(rs.getString("uom"))
                                    .productCategory(rs.getString("category"))
                                    .volumeIncentiveRate(rs.getBigDecimal("volume_incentive_mt"))
                                    .consistencyIncentiveRate(rs.getBigDecimal("consistency_incentive_mt"))
                                    .msmeIncentiveRate(rs.getBigDecimal("msme_incentive_mt"))
                                    .achievementPercentage(rs.getBigDecimal("achieved_qty_perc"))
                                    .achievedQuantity(rs.getBigDecimal("achieved_qty"))
                                    .remainingQuantity(rs.getBigDecimal("remaining_qty"))
                                    .targetQuantity(rs.getBigDecimal("target_qty"))
                                    .savingsAchieved(rs.getBigDecimal("total_savings"))
                                    .potentialSavings(rs.getBigDecimal("potential_savings"))
                                    .build(),
                    mouId, financialYear, mouType
            );
        } catch (Exception e) {
            log.error("DB error fetching Mou product categories for mouId={}", mouId, e);
            throw new CentralCommerceServiceException("Failed to fetch Mou product categories",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
