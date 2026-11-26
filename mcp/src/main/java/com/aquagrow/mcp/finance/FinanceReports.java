package com.aquagrow.mcp.finance;

import java.util.List;

/** Result shapes of the financial tools. Money is in the farm currency, weights in kg, rates per kg. */
public final class FinanceReports {

    private FinanceReports() {
    }

    public record CategoryAmount(String category, double amount) {
    }

    public record BatchFinancials(
            String batchId,
            String batchName,
            String batchStatus,
            boolean provisional,
            String currency,
            double grossSales,
            double commission,
            double netRevenue,
            double harvestedKg,
            double billedKg,
            Double netRatePerBilledKg,
            double totalDirectCost,
            List<CategoryAmount> costByCategory,
            double profit,
            Double marginPercent,
            Double roiPercent,
            Double costPerKgHarvested,
            Double breakEvenRatePerBilledKg,
            List<String> notes) {
    }

    public record MonthPnl(
            String month,
            double grossSales,
            double commission,
            double netRevenue,
            double batchCosts,
            double farmCosts,
            Double salaries,
            double totalCosts,
            double profit) {
    }

    public record FarmPnl(
            String from,
            String to,
            String currency,
            boolean salariesIncluded,
            List<MonthPnl> months,
            MonthPnl total,
            List<CategoryAmount> costByCategory,
            List<CategoryAmount> costByMacroInvestment,
            List<String> notes) {
    }

    public record FeedEfficiency(
            String batchId,
            String batchName,
            String batchStatus,
            double totalFeedKg,
            List<CategoryAmount> feedByType,
            String firstFeedDate,
            String lastFeedDate,
            int daysWithFeed,
            Double averageDailyFeedKg,
            double harvestedKg,
            Double stockedBiomassKg,
            Double biomassGainKg,
            Double fcr,
            Double feedPerKgHarvested,
            String currency,
            double feedExpenditure,
            Double feedCostPerKgHarvested,
            Double latestAverageWeightGrams,
            String latestSampleDate,
            List<String> notes) {
    }

    public record UnpaidFeedOrder(
            String orderId,
            String feedType,
            double quantityKg,
            String supplier,
            String orderDate,
            String status,
            String paymentStatus) {
    }

    public record FeedOrdersDue(
            int count,
            double totalQuantityKg,
            boolean amountsRecorded,
            List<UnpaidFeedOrder> orders) {
    }

    public record EmployeeDue(String employeeId, String name, String role, double expectedBaseSalary) {
    }

    public record AdvanceBalance(String employeeId, String name, double advanceTaken) {
    }

    public record SalariesDue(
            String month,
            List<EmployeeDue> notYetPaid,
            double totalExpectedBaseSalary,
            List<AdvanceBalance> advanceBalances,
            double totalAdvanceOutstanding) {
    }

    public record Payables(
            String asOf,
            String currency,
            FeedOrdersDue feedOrders,
            SalariesDue salaries,
            List<String> notes) {
    }
}
