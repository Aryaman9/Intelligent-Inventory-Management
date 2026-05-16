package com.inventory.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStatsResponse {

    private long totalTransactions;
    private long totalSalesCount;
    private long totalPurchasesCount;
    private BigDecimal totalSalesAmount;
    private BigDecimal totalRevenue;
    private BigDecimal totalProfit;
    private BigDecimal profitMargin;
    private Map<String, Long> paymentMethodBreakdown;
    private List<DailyRevenue> dailyRevenue;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenue {
        private String date;
        private BigDecimal revenue;
    }
}
