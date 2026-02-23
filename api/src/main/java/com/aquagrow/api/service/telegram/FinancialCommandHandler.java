package com.aquagrow.api.service.telegram;

import com.aquagrow.api.model.Employee;
import com.aquagrow.api.model.Expenditure;
import com.aquagrow.api.model.SalaryPayment;
import com.aquagrow.api.repository.EmployeeRepository;
import com.aquagrow.api.security.TenantContext;
import com.aquagrow.api.service.ExpenditureService;
import com.aquagrow.api.service.SalaryPaymentService;
import com.aquagrow.api.service.TelegramMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class FinancialCommandHandler implements TelegramCommandHandler {

    private final ExpenditureService expenditureService;
    private final SalaryPaymentService salaryPaymentService;
    private final EmployeeRepository employeeRepository;
    private final TelegramMessageService telegramMessageService;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    // Extended Expenditure: EX [Date?] [BatchName] [Amount] [Macro] [Type]
    // [SubType] [Description?]
    // Date is optional (YYYY-MM-DD); Description is optional
    private static final Pattern EXPENDITURE_EXTENDED_PATTERN = Pattern.compile(
            "^EX\\s+(?:(\\d{4}-\\d{2}-\\d{2})\\s+)?(\\S+)\\s+(\\d+(?:\\.\\d+)?)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)(?:\\s+(.+))?$",
            Pattern.CASE_INSENSITIVE);

    // Simple Expenditure (backward compat): EX [BatchName] [Category] [Amount]
    // [Description?]
    private static final Pattern EXPENDITURE_SIMPLE_PATTERN = Pattern
            .compile("^EX\\s+(\\S+)\\s+(\\S+)\\s+(\\d+(?:\\.\\d+)?)(?:\\s+(.+))?$", Pattern.CASE_INSENSITIVE);

    // Salary: PAY [EmployeeName] [Base] [Bonus] [Description]
    private static final Pattern SALARY_PATTERN = Pattern.compile(
            "^PAY\\s+(\\S+)\\s+(\\d+(?:\\.\\d+)?)(?:\\s+(\\d+(?:\\.\\d+)?))?(?:\\s+(.+))?$", Pattern.CASE_INSENSITIVE);

    // Financial Summary: FIN [Month]
    private static final Pattern FINANCIAL_SUMMARY_PATTERN = Pattern.compile("^FIN(?:\\s+(\\d{4}-\\d{2}))?$",
            Pattern.CASE_INSENSITIVE);

    // Raw Expenditure (User specific TSV/pasted format)
    private static final Pattern RAW_EXPENDITURE_PATTERN = Pattern.compile(
            "^(\\d{1,2}\\s+[a-zA-Z]{3}\\s+\\d{4})\\t(\\d{4}-\\d{2}-\\d{2})\\t([A-Z_]+)\\t([a-zA-Z]+)\\t\"(.+)\"\\t₹([\\d,]+(?:\\.\\d{2})?)$");

    @Override
    public boolean canHandle(String text) {
        String cleanText = text.trim().toUpperCase();
        return cleanText.startsWith("EX ") ||
                cleanText.startsWith("PAY ") ||
                cleanText.equals("FIN") || cleanText.startsWith("FIN ") ||
                RAW_EXPENDITURE_PATTERN.matcher(text.trim()).matches();
    }

    @Override
    public void handle(String chatId, String text) {
        String cleanText = text.trim();
        if (cleanText.startsWith("EX") || cleanText.startsWith("ex")) {
            handleExpenditure(chatId, cleanText);
        } else if (cleanText.startsWith("PAY") || cleanText.startsWith("pay")) {
            handleSalary(chatId, cleanText);
        } else if (cleanText.startsWith("FIN") || cleanText.startsWith("fin")) {
            handleFinancialSummary(chatId, cleanText);
        } else if (RAW_EXPENDITURE_PATTERN.matcher(cleanText).matches()) {
            handleRawExpenditure(chatId, cleanText);
        }
    }

    private void handleExpenditure(String chatId, String text) {
        // Try extended format first: EX [Date?] [Batch] [Amount] [Macro] [Type]
        // [SubType] [Desc?]
        Matcher extended = EXPENDITURE_EXTENDED_PATTERN.matcher(text.trim());
        if (extended.matches()) {
            String dateStr = extended.group(1);
            String batchName = extended.group(2);
            double amount = Double.parseDouble(extended.group(3));
            String macro = extended.group(4);
            String type = extended.group(5);
            String subType = extended.group(6);
            String description = extended.group(7);
            LocalDate date = (dateStr != null) ? LocalDate.parse(dateStr) : LocalDate.now();

            expenditureService.logExpenditureExtended(date, batchName, amount, macro, type, subType, description);
            telegramMessageService.sendMessage(chatId,
                    String.format("✅ Expense of ₹%.2f logged for %s [%s / %s / %s] on %s.",
                            amount, batchName, macro, type, subType, date));
            return;
        }

        // Fall back to simple format: EX [Batch] [Category] [Amount] [Desc?]
        Matcher simple = EXPENDITURE_SIMPLE_PATTERN.matcher(text.trim());
        if (simple.matches()) {
            String batchName = simple.group(1);
            String category = simple.group(2);
            double amount = Double.parseDouble(simple.group(3));
            String description = simple.group(4);

            expenditureService.logExpenditureByName(batchName, category, amount, description);
            telegramMessageService.sendMessage(chatId,
                    String.format("✅ Expense of ₹%.2f logged for %s (%s).", amount, batchName, category));
            return;
        }

        telegramMessageService.sendMessage(chatId,
                "❌ Invalid format.\n" +
                        "• Simple: <code>EX [Batch] [Category] [Amount] [Desc?]</code>\n" +
                        "• Extended: <code>EX [Date?] [Batch] [Amount] [Macro] [Type] [SubType] [Desc?]</code>\n" +
                        "<i>Ex: EX 2026-02-23 Batch1 3250 NO_RETURN NoReturn Preparation \"Kalupu mandu\"</i>");
    }

    private void handleSalary(String chatId, String text) {
        Matcher matcher = SALARY_PATTERN.matcher(text.trim());
        if (!matcher.matches()) {
            telegramMessageService.sendMessage(chatId,
                    "❌ Invalid format. Use: <code>PAY [Employee] [Base] [Bonus] [Description]</code>");
            return;
        }

        String employeeName = matcher.group(1);
        double base = Double.parseDouble(matcher.group(2));
        double bonus = matcher.group(3) != null ? Double.parseDouble(matcher.group(3)) : 0.0;

        Optional<Employee> empOpt = employeeRepository.findFirstByNameAndTenantId(employeeName,
                TenantContext.getTenantId());
        if (empOpt.isEmpty()) {
            telegramMessageService.sendMessage(chatId, "❌ Employee not found: " + employeeName);
            return;
        }

        SalaryPayment payment = salaryPaymentService.createSalaryPayment(
                empOpt.get().getId(), LocalDate.now(), base, 0.0, bonus);

        telegramMessageService.sendMessage(chatId,
                String.format("✅ Salary payment of ₹%.2f logged for %s.", payment.getNetAmount(), employeeName));
    }

    private void handleFinancialSummary(String chatId, String text) {
        Matcher matcher = FINANCIAL_SUMMARY_PATTERN.matcher(text.trim());
        if (!matcher.matches())
            return;

        String monthStr = matcher.group(1);
        LocalDate date = (monthStr != null) ? LocalDate.parse(monthStr + "-01") : LocalDate.now();

        Map<String, Double> summary = expenditureService.getMonthlySummary(date);
        if (summary.isEmpty()) {
            telegramMessageService.sendMessage(chatId, "💰 No expenses found for " + date.format(MONTH_FORMATTER));
            return;
        }

        StringBuilder sb = new StringBuilder("💰 <b>Expense Summary (" + date.format(MONTH_FORMATTER) + ")</b>:\n\n");
        double total = 0;
        for (Map.Entry<String, Double> entry : summary.entrySet()) {
            sb.append(String.format("• %s: <b>₹%.2f</b>\n", entry.getKey(), entry.getValue()));
            total += entry.getValue();
        }
        sb.append(String.format("\nTotal: <b>₹%.2f</b>", total));
        telegramMessageService.sendMessage(chatId, sb.toString());
    }

    private void handleRawExpenditure(String chatId, String text) {
        Matcher matcher = RAW_EXPENDITURE_PATTERN.matcher(text.trim());
        if (!matcher.matches())
            return;

        String expenseDateStr = matcher.group(2);
        String macro = matcher.group(3);
        String category = matcher.group(4);
        String desc = matcher.group(5);
        String amountStr = matcher.group(6).replace(",", "");
        double amount = Double.parseDouble(amountStr);

        Expenditure expenditure = Expenditure.builder()
                .date(LocalDate.parse(expenseDateStr))
                .macroInvestment(macro)
                .category(category)
                .description(desc)
                .amount(amount)
                .tenantId(TenantContext.getTenantId())
                .build();

        expenditureService.logExpenditure(expenditure);
        telegramMessageService.sendMessage(chatId,
                String.format("✅ Raw expense of ₹%.2f logged (%s).", amount, category));
    }
}
