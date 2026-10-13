package com.aquagrow.mcp.finance;

import com.aquagrow.mcp.finance.FinanceReports.*;
import com.aquagrow.mcp.model.Batch;
import com.aquagrow.mcp.model.Employee;
import com.aquagrow.mcp.model.Expenditure;
import com.aquagrow.mcp.model.FeedLog;
import com.aquagrow.mcp.model.FeedOrder;
import com.aquagrow.mcp.model.FishSeed;
import com.aquagrow.mcp.model.HarvestLog;
import com.aquagrow.mcp.model.OrderStatus;
import com.aquagrow.mcp.model.PaymentStatus;
import com.aquagrow.mcp.model.SalaryPayment;
import com.aquagrow.mcp.model.SampleLog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Financial formulas, kept free of Spring and I/O so each one can be tested on plain data.
 *
 * Conventions (agreed with the owner):
 * - Revenue is {@code HarvestLog.revenue}, which the API stores net of commission.
 * - Salaries are a farm-wide cost, never allocated to a batch. A salary payment costs
 *   {@code baseSalary + bonus} in its month. Expenditures with category "Salary" are
 *   ignored in months that have salary payment records (the web app can record a paid
 *   salary in both places) and counted only in months that have none.
 * - Unpaid feed orders carry no price, so they can only be reported by quantity.
 * - Items are recognised on their own dates (cash style).
 */
public final class FinanceCalculator {

    private FinanceCalculator() {
    }

    // ---------- batch ----------

    public static BatchFinancials batchFinancials(Batch batch, List<Expenditure> batchExpenditures,
                                                  List<HarvestLog> harvests, String currency) {
        List<String> notes = new ArrayList<>();

        double net = 0, commission = 0, harvestedKg = 0, billedKg = 0;
        int missingRevenue = 0;
        for (HarvestLog h : harvests) {
            if (h.getRevenue() == null) {
                missingRevenue++;
            }
            net += nz(h.getRevenue());
            commission += nz(h.getCommission());
            harvestedKg += nz(h.getBiomassWeight());
            billedKg += h.getActualWeight() != null ? h.getActualWeight() : nz(h.getBiomassWeight());
        }

        List<Expenditure> costs = new ArrayList<>();
        int salaryTagged = 0;
        double salaryTaggedAmount = 0;
        int missingAmount = 0;
        for (Expenditure e : batchExpenditures) {
            if (isSalary(e)) {
                salaryTagged++;
                salaryTaggedAmount += nz(e.getAmount());
            } else if (e.getAmount() == null) {
                missingAmount++;
            } else {
                costs.add(e);
            }
        }
        double cost = costs.stream().mapToDouble(Expenditure::getAmount).sum();
        double profit = net - cost;

        boolean provisional = !"Completed".equals(batch.getStatus());
        if (provisional) {
            notes.add("This batch is not fully harvested (status " + batch.getStatus()
                    + "): revenue is partial, so profit and margin are provisional.");
        }
        if (harvests.isEmpty()) {
            notes.add("No harvests are recorded for this batch yet, so there is no revenue.");
        }
        notes.add("Salaries and other farm-wide costs, and expenditures not tagged to this batch, are not included.");
        if (salaryTagged > 0) {
            notes.add(salaryTagged + " expenditure(s) of category Salary totalling " + r2(salaryTaggedAmount)
                    + " are tagged to this batch but excluded: salaries are farm-wide, not batch costs.");
        }
        if (missingRevenue > 0) {
            notes.add(missingRevenue + " harvest log(s) have no revenue recorded and count as 0.");
        }
        if (missingAmount > 0) {
            notes.add(missingAmount + " expenditure(s) have no amount and were ignored.");
        }

        return new BatchFinancials(
                batch.getId(), batch.getName(), batch.getStatus(), provisional, currency,
                r2(net + commission), r2(commission), r2(net),
                r2(harvestedKg), r2(billedKg), billedKg > 0 ? r2(net / billedKg) : null,
                r2(cost), byCategory(costs),
                r2(profit),
                net > 0 ? r2(profit / net * 100) : null,
                cost > 0 ? r2(profit / cost * 100) : null,
                harvestedKg > 0 ? r2(cost / harvestedKg) : null,
                billedKg > 0 ? r2(cost / billedKg) : null,
                notes);
    }

    // ---------- farm P&L ----------

    /**
     * @param payments       salary payments, ignored when {@code includeSalaries} is false
     * @param includeSalaries false for callers who may not see salary data
     */
    public static FarmPnl farmPnl(LocalDate from, LocalDate to, List<Expenditure> expenditures,
                                  List<HarvestLog> harvests, List<SalaryPayment> payments,
                                  boolean includeSalaries, String currency) {
        List<String> notes = new ArrayList<>();
        YearMonth first = YearMonth.from(from);
        YearMonth last = YearMonth.from(to);

        Map<YearMonth, double[]> rows = new TreeMap<>(); // [net, commission, batchCost, farmCost, salary]
        for (YearMonth m = first; !m.isAfter(last); m = m.plusMonths(1)) {
            rows.put(m, new double[5]);
        }

        for (HarvestLog h : harvests) {
            if (inRange(h.getDate(), from, to)) {
                double[] row = rows.get(YearMonth.from(h.getDate()));
                row[0] += nz(h.getRevenue());
                row[1] += nz(h.getCommission());
            }
        }

        Set<YearMonth> monthsWithPayments = new HashSet<>();
        Map<String, Double> categoryTotals = new LinkedHashMap<>();
        Map<String, Double> macroTotals = new LinkedHashMap<>();
        double salaryExpenditureIgnored = 0;
        int undated = 0;
        int missingAmount = 0;
        boolean salaryPaymentsPresent = false;

        if (includeSalaries && payments != null) {
            for (SalaryPayment p : payments) {
                YearMonth pm = paymentMonth(p);
                if (pm == null || pm.isBefore(first) || pm.isAfter(last)) {
                    continue;
                }
                double salaryCost = nz(p.getBaseSalary()) + nz(p.getBonus());
                rows.get(pm)[4] += salaryCost;
                monthsWithPayments.add(pm);
                salaryPaymentsPresent = true;
                categoryTotals.merge("Salary", salaryCost, Double::sum);
                macroTotals.merge("SALARY", salaryCost, Double::sum);
            }
        }

        int salaryExpendituresExcluded = 0;
        for (Expenditure e : expenditures) {
            if (e.getDate() == null) {
                undated++;
                continue;
            }
            if (!inRange(e.getDate(), from, to)) {
                continue;
            }
            if (e.getAmount() == null) {
                missingAmount++;
                continue;
            }
            YearMonth em = YearMonth.from(e.getDate());
            if (isSalary(e)) {
                if (!includeSalaries) {
                    salaryExpendituresExcluded++;
                    continue;
                }
                if (monthsWithPayments.contains(em)) {
                    salaryExpenditureIgnored += e.getAmount();
                    continue;
                }
                rows.get(em)[4] += e.getAmount();
                categoryTotals.merge("Salary", e.getAmount(), Double::sum);
                macroTotals.merge("SALARY", e.getAmount(), Double::sum);
                continue;
            }
            rows.get(em)[e.getBatchId() != null && !e.getBatchId().isBlank() ? 2 : 3] += e.getAmount();
            categoryTotals.merge(label(e.getCategory(), "Uncategorised"), e.getAmount(), Double::sum);
            macroTotals.merge(label(e.getMacroInvestment(), "Unallocated"), e.getAmount(), Double::sum);
        }

        List<MonthPnl> months = new ArrayList<>();
        double tNet = 0, tComm = 0, tBatch = 0, tFarm = 0, tSal = 0;
        for (Map.Entry<YearMonth, double[]> entry : rows.entrySet()) {
            double[] v = entry.getValue();
            months.add(monthRow(entry.getKey().toString(), v[0], v[1], v[2], v[3], includeSalaries ? v[4] : null));
            tNet += v[0];
            tComm += v[1];
            tBatch += v[2];
            tFarm += v[3];
            tSal += v[4];
        }
        MonthPnl total = monthRow("TOTAL", tNet, tComm, tBatch, tFarm, includeSalaries ? tSal : null);

        notes.add("Revenue is net of commission. Items are counted on their own dates, so a period's profit is not the "
                + "same as a batch's profit (a batch's costs and its harvest can fall in different months).");
        if (includeSalaries) {
            notes.add("Salaries are a farm-wide cost (base salary + bonus, per salary month); they are not allocated to batches.");
            if (salaryExpenditureIgnored > 0) {
                notes.add("Expenditures of category Salary totalling " + r2(salaryExpenditureIgnored)
                        + " were ignored in months that have salary payment records, to avoid counting salaries twice.");
            }
            if (!salaryPaymentsPresent && total.salaries() != null && total.salaries() == 0) {
                notes.add("No salary payments or Salary expenditures are recorded in this period.");
            }
        } else {
            notes.add("Salary data is only available to the main farm account, so profit here EXCLUDES salaries.");
        }
        if (undated > 0) {
            notes.add(undated + " expenditure(s) have no date and are not in any month.");
        }
        if (missingAmount > 0) {
            notes.add(missingAmount + " expenditure(s) in the period have no amount and were ignored.");
        }
        notes.add("Unpaid feed orders are not included: feed orders carry no price. Feed spend appears only if it was "
                + "entered as an expenditure.");

        return new FarmPnl(from.toString(), to.toString(), currency, includeSalaries, months, total,
                toList(categoryTotals), toList(macroTotals), notes);
    }

    private static MonthPnl monthRow(String month, double net, double commission, double batchCost,
                                     double farmCost, Double salaries) {
        double totalCost = batchCost + farmCost + (salaries == null ? 0 : salaries);
        return new MonthPnl(month, r2(net + commission), r2(commission), r2(net), r2(batchCost), r2(farmCost),
                salaries == null ? null : r2(salaries), r2(totalCost), r2(net - totalCost));
    }

    // ---------- feed efficiency ----------

    public static FeedEfficiency feedEfficiency(Batch batch, List<FeedLog> feedLogs, List<Expenditure> batchExpenditures,
                                                List<HarvestLog> harvests, List<SampleLog> samples,
                                                LocalDate today, String currency) {
        List<String> notes = new ArrayList<>();

        double totalFeed = 0;
        Map<String, Double> byType = new LinkedHashMap<>();
        Set<LocalDate> feedDates = new HashSet<>();
        for (FeedLog f : feedLogs) {
            totalFeed += nz(f.getAmount());
            byType.merge(label(f.getFeedType(), "Unspecified"), nz(f.getAmount()), Double::sum);
            if (f.getDate() != null) {
                feedDates.add(f.getDate());
            }
        }
        LocalDate firstFeed = feedDates.stream().min(Comparator.naturalOrder()).orElse(null);
        LocalDate lastFeed = feedDates.stream().max(Comparator.naturalOrder()).orElse(null);
        long span = firstFeed == null ? 0 : ChronoUnit.DAYS.between(firstFeed, lastFeed) + 1;

        double harvestedKg = harvests.stream().mapToDouble(h -> nz(h.getBiomassWeight())).sum();

        Double stocked = stockedBiomassKg(batch);
        boolean completed = "Completed".equals(batch.getStatus());
        Double gain = stocked != null && harvestedKg > 0 ? harvestedKg - stocked : null;
        Double fcr = completed && gain != null && gain > 0 && totalFeed > 0 ? r2(totalFeed / gain) : null;

        if (feedLogs.isEmpty()) {
            notes.add("No feed logs are recorded for this batch.");
        } else if (span > feedDates.size()) {
            notes.add((span - feedDates.size()) + " day(s) between the first and last feed log have no feed recorded, "
                    + "so the daily average is based on " + span + " calendar days.");
        }
        if (fcr == null) {
            if (!completed) {
                notes.add("FCR needs the final harvest; this batch is " + batch.getStatus()
                        + ". Feed used so far is shown, and feed per kg harvested is only a partial figure.");
            } else if (stocked == null) {
                notes.add("FCR cannot be calculated: stocked quantity or size is missing on one or more fish seeds.");
            } else if (gain == null || gain <= 0) {
                notes.add("FCR cannot be calculated: harvested weight is missing or not above the stocked weight.");
            }
        }

        double feedCost = batchExpenditures.stream()
                .filter(e -> e.getCategory() != null && "feed".equalsIgnoreCase(e.getCategory().trim()))
                .mapToDouble(e -> nz(e.getAmount())).sum();
        if (feedCost == 0) {
            notes.add("No expenditures of category Feed are tagged to this batch, so feed cost is 0 here.");
        }

        SampleLog latest = samples.stream()
                .filter(s -> s.getDate() != null && s.getAverageWeight() != null)
                .max(Comparator.comparing(SampleLog::getDate)).orElse(null);
        long recent = samples.stream()
                .filter(s -> s.getDate() != null && !s.getDate().isBefore(today.minusDays(30)) && !s.getDate().isAfter(today))
                .count();
        if (recent < 2) {
            notes.add("Only " + recent + " growth sample(s) in the last 30 days; growth-based conclusions are weak.");
        }

        return new FeedEfficiency(
                batch.getId(), batch.getName(), batch.getStatus(),
                r2(totalFeed), toList(byType),
                firstFeed == null ? null : firstFeed.toString(), lastFeed == null ? null : lastFeed.toString(),
                feedDates.size(), span > 0 ? r2(totalFeed / span) : null,
                r2(harvestedKg), stocked == null ? null : r2(stocked), gain == null ? null : r2(gain), fcr,
                harvestedKg > 0 && totalFeed > 0 ? r2(totalFeed / harvestedKg) : null,
                currency, r2(feedCost), harvestedKg > 0 && feedCost > 0 ? r2(feedCost / harvestedKg) : null,
                latest == null ? null : latest.getAverageWeight(), latest == null ? null : latest.getDate().toString(),
                notes);
    }

    /** Stocked biomass in kg, or null if any seed lacks quantity or size. */
    static Double stockedBiomassKg(Batch batch) {
        if (batch.getFishSeeds() == null || batch.getFishSeeds().isEmpty()) {
            return null;
        }
        double kg = 0;
        for (FishSeed seed : batch.getFishSeeds()) {
            if (seed.getQuantity() == null || seed.getSizeGrams() == null) {
                return null;
            }
            kg += seed.getQuantity() * seed.getSizeGrams() / 1000.0;
        }
        return kg;
    }

    // ---------- payables ----------

    public static Payables payables(LocalDate today, YearMonth month, List<FeedOrder> orders,
                                    Map<String, String> supplierNames, List<Employee> employees,
                                    List<SalaryPayment> payments, boolean includeSalaries, String currency) {
        List<String> notes = new ArrayList<>();

        List<UnpaidFeedOrder> unpaid = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .filter(o -> o.getPaymentStatus() != PaymentStatus.PAID)
                .sorted(Comparator.comparing(FeedOrder::getOrderDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(o -> new UnpaidFeedOrder(o.getId(), o.getFeedType(), nz(o.getQuantity()),
                        supplierNames.getOrDefault(o.getSupplierId(), o.getSupplierId()),
                        o.getOrderDate() == null ? null : o.getOrderDate().toLocalDate().toString(),
                        String.valueOf(o.getStatus()), String.valueOf(o.getPaymentStatus() == null ? PaymentStatus.UNPAID : o.getPaymentStatus())))
                .toList();
        FeedOrdersDue feedDue = new FeedOrdersDue(unpaid.size(),
                r2(unpaid.stream().mapToDouble(UnpaidFeedOrder::quantityKg).sum()), false, unpaid);
        if (!unpaid.isEmpty()) {
            notes.add("Feed orders have no price recorded, so the rupee amount owed is unknown; only quantities are shown. "
                    + "Ask the user for the amounts, or record the purchases as Feed expenditures.");
        }

        SalariesDue salaries = null;
        if (includeSalaries) {
            Set<String> paidEmployees = new HashSet<>();
            for (SalaryPayment p : payments) {
                if (month.equals(paymentMonth(p))) {
                    paidEmployees.add(p.getEmployeeId());
                }
            }
            List<EmployeeDue> notPaid = employees.stream()
                    .filter(e -> "Active".equalsIgnoreCase(e.getStatus()))
                    .filter(e -> !paidEmployees.contains(e.getId()))
                    .map(e -> new EmployeeDue(e.getId(), e.getName(), e.getRole(), r2(nz(e.getSalary()))))
                    .toList();
            List<AdvanceBalance> advances = employees.stream()
                    .filter(e -> "Active".equalsIgnoreCase(e.getStatus()) && nz(e.getAdvanceTaken()) > 0)
                    .map(e -> new AdvanceBalance(e.getId(), e.getName(), r2(e.getAdvanceTaken())))
                    .toList();
            salaries = new SalariesDue(month.toString(), notPaid,
                    r2(notPaid.stream().mapToDouble(EmployeeDue::expectedBaseSalary).sum()),
                    advances, r2(advances.stream().mapToDouble(AdvanceBalance::advanceTaken).sum()));
            if (month.isAfter(YearMonth.from(today))) {
                notes.add("The month " + month + " has not started yet; its salaries are not due.");
            }
            notes.add("Expected salary is each active employee's base salary; any advance deduction or bonus is not applied.");
        } else {
            notes.add("Salary data is only available to the main farm account, so it is not shown here.");
        }

        return new Payables(today.toString(), currency, feedDue, salaries, notes);
    }

    // ---------- helpers ----------

    static boolean isSalary(Expenditure e) {
        return e.getCategory() != null && "salary".equalsIgnoreCase(e.getCategory().trim());
    }

    /** The month a salary payment is for: its month field ("YYYY-MM"), else the month of its date. */
    static YearMonth paymentMonth(SalaryPayment p) {
        if (p.getMonth() != null) {
            try {
                return YearMonth.parse(p.getMonth().trim());
            } catch (DateTimeParseException ignored) {
                // fall back to the payment date
            }
        }
        return p.getDate() == null ? null : YearMonth.from(p.getDate());
    }

    private static boolean inRange(LocalDate d, LocalDate from, LocalDate to) {
        return d != null && !d.isBefore(from) && !d.isAfter(to);
    }

    private static List<CategoryAmount> byCategory(List<Expenditure> costs) {
        Map<String, Double> totals = new LinkedHashMap<>();
        for (Expenditure e : costs) {
            totals.merge(label(e.getCategory(), "Uncategorised"), e.getAmount(), Double::sum);
        }
        return toList(totals);
    }

    private static List<CategoryAmount> toList(Map<String, Double> totals) {
        return totals.entrySet().stream()
                .map(en -> new CategoryAmount(en.getKey(), r2(en.getValue())))
                .sorted(Comparator.comparingDouble(CategoryAmount::amount).reversed()
                        .thenComparing(CategoryAmount::category))
                .toList();
    }

    private static String label(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static double nz(Double v) {
        return v == null ? 0.0 : v;
    }

    static double r2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
