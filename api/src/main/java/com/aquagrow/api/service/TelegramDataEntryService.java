package com.aquagrow.api.service;

import com.aquagrow.api.model.*;
import com.aquagrow.api.repository.*;
import com.aquagrow.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramDataEntryService {

    private final EmployeeRepository employeeRepository;
    private final BatchRepository batchRepository;
    private final FishTypeRepository fishTypeRepository;
    private final FeedService feedService;
    private final SampleService sampleService;
    private final TelegramMessageService telegramMessageService;
    private final FeedOrderService feedOrderService;
    private final SupplierRepository supplierRepository;
    private final FeedStockRepository feedStockRepository;
    private final FeedOrderRepository feedOrderRepository;

    // Enhanced Feed Pattern: [Date] [BatchName] [FeedType] [Amount] [Unit] [Time]
    // Example: 2026-02-10 2025-11-28 King Fish Feed 480 kg 07:00
    private static final Pattern ENHANCED_FEED_PATTERN = Pattern.compile(
            "^(?:(\\d{4}-\\d{2}-\\d{2})\\s+)?(\\S+)\\s+(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s*(kg|g|KG|G)(?:\\s+(\\d{2}:\\d{2}))?$",
            Pattern.CASE_INSENSITIVE);

    // Old Feed Pattern (kept for backward compatibility or simple entries)
    private static final Pattern FEED_PATTERN = Pattern.compile("^F\\s+(\\S+)\\s+(\\S+)\\s+(\\d+(\\.\\d+)?)$",
            Pattern.CASE_INSENSITIVE);

    // S [BatchName] [FishType] [Count] [TotalWeight]
    private static final Pattern SAMPLE_PATTERN = Pattern
            .compile("^S\\s+(\\S+)\\s+(\\S+)\\s+(\\d+)\\s+(\\d+(\\.\\d+)?)$", Pattern.CASE_INSENSITIVE);

    // Feed Order: FO [Supplier] [FeedType] [Quantity]
    private static final Pattern FEED_ORDER_PATTERN = Pattern.compile("^FO\\s+(\\S+)\\s+(.+?)\\s+(\\d+(?:\\.\\d+)?)$",
            Pattern.CASE_INSENSITIVE);

    // Feed Confirm: FC [OrderID]
    private static final Pattern FEED_CONFIRM_PATTERN = Pattern.compile("^FC\\s+(\\S+)$", Pattern.CASE_INSENSITIVE);

    // Feed Receive: FR [OrderID]
    private static final Pattern FEED_RECEIVE_PATTERN = Pattern.compile("^FR\\s+(\\S+)$", Pattern.CASE_INSENSITIVE);

    public void processMessage(String chatId, String text) {
        if (text.equalsIgnoreCase("/start")) {
            telegramMessageService.sendMessage(chatId, "👋 Welcome to AquaGrow Bot!\n\nYour Telegram Chat ID is: <code>"
                    + chatId + "</code>\n\nPlease share this ID with your admin to get authorized.");
            return;
        }

        Optional<Employee> employeeOpt = employeeRepository.findByTelegramChatId(chatId);
        if (employeeOpt.isEmpty()) {
            telegramMessageService.sendMessage(chatId,
                    "❌ You are not authorized to use this bot. Please contact your admin and provide your Chat ID: <code>"
                            + chatId + "</code>");
            return;
        }

        Employee employee = employeeOpt.get();
        TenantContext.setTenantId(employee.getTenantId());

        try {
            String cleanText = text.trim();
            if (cleanText.equalsIgnoreCase("/help")) {
                telegramMessageService.sendMessage(chatId, "📖 <b>Available Commands</b>:\n\n" +
                        "<b>Logging</b>:\n" +
                        "- <b>Feed (Detailed)</b>: <code>[Date] [BatchName] [FeedType] [Amount] [Unit] [Time]</code>\n"
                        +
                        "  <i>Ex: 2026-02-10 2025-11-28 King Fish Feed 480 kg 07:00</i>\n" +
                        "- <b>Sample</b>: <code>S [BatchName] [FishType] [Pieces] [TotalWeight]</code>\n" +
                        "  <i>Ex: S B1 Tilapia 10 2.5</i>\n\n" +
                        "<b>Feed Management</b>:\n" +
                        "- <b>Inventory</b>: <code>FI</code>\n" +
                        "- <b>Order</b>: <code>FO [Supplier] [FeedType] [Quantity]</code>\n" +
                        "- <b>Confirm</b>: <code>FC [OrderID]</code>\n" +
                        "- <b>Receive</b>: <code>FR [OrderID]</code>");
            } else if (cleanText.startsWith("FI") || cleanText.startsWith("fi")) {
                handleFeedInventory(chatId);
            } else if (cleanText.startsWith("FO") || cleanText.startsWith("fo")) {
                handleFeedOrder(chatId, cleanText);
            } else if (cleanText.startsWith("FC") || cleanText.startsWith("fc")) {
                handleFeedConfirm(chatId, cleanText);
            } else if (cleanText.startsWith("FR") || cleanText.startsWith("fr")) {
                handleFeedReceive(chatId, cleanText);
            } else if (cleanText.startsWith("F") || cleanText.startsWith("f")) {
                handleFeed(chatId, cleanText);
            } else if (ENHANCED_FEED_PATTERN.matcher(cleanText).matches()) {
                handleEnhancedFeed(chatId, cleanText);
            } else if (cleanText.startsWith("S") || cleanText.startsWith("s")) {
                handleSample(chatId, cleanText);
            } else {
                telegramMessageService.sendMessage(chatId, "❓ Unknown command. Type /help for instructions.");
            }
        } catch (Exception e) {
            log.error("Error processing Telegram message: {}", text, e);
            telegramMessageService.sendMessage(chatId, "❌ Error processing message: " + e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    private void handleFeed(String chatId, String text) {
        Matcher matcher = FEED_PATTERN.matcher(text.trim());
        if (!matcher.matches()) {
            telegramMessageService.sendMessage(chatId,
                    "❌ Invalid Feed format. Use: <code>F [BatchName] [FeedType] [Amount]</code>");
            return;
        }

        String batchName = matcher.group(1);
        String feedType = matcher.group(2);
        double amount = Double.parseDouble(matcher.group(3));

        Optional<Batch> batchOpt = batchRepository.findByName(batchName);
        if (batchOpt.isEmpty()) {
            telegramMessageService.sendMessage(chatId, "❌ Batch not found: " + batchName);
            return;
        }

        FeedLog feedLog = FeedLog.builder()
                .batchId(batchOpt.get().getId())
                .date(LocalDate.now())
                .feedType(feedType)
                .amount(amount)
                .tenantId(TenantContext.getTenantId())
                .build();

        feedService.createFeedLog(feedLog);
        telegramMessageService.sendMessage(chatId,
                String.format("✅ Logged %.1f kg of %s for batch %s", amount, feedType, batchName));
    }

    private void handleEnhancedFeed(String chatId, String text) {
        Matcher matcher = ENHANCED_FEED_PATTERN.matcher(text.trim());
        if (!matcher.matches()) {
            return; // Should not happen if pre-checked
        }

        String dateStr = matcher.group(1);
        String batchName = matcher.group(2);
        String feedType = matcher.group(3);
        double amount = Double.parseDouble(matcher.group(4));
        String unit = matcher.group(5);
        String timeStr = matcher.group(6);

        // Convert amount to kg
        if (unit.equalsIgnoreCase("g")) {
            amount = amount / 1000.0;
        }

        LocalDate date = (dateStr != null) ? LocalDate.parse(dateStr) : LocalDate.now();

        Optional<Batch> batchOpt = batchRepository.findByName(batchName);
        if (batchOpt.isEmpty()) {
            telegramMessageService.sendMessage(chatId, "❌ Batch not found: " + batchName);
            return;
        }

        FeedLog feedLog = FeedLog.builder()
                .batchId(batchOpt.get().getId())
                .date(date)
                .feedType(feedType)
                .amount(amount)
                .time(timeStr)
                .tenantId(TenantContext.getTenantId())
                .build();

        feedService.createFeedLog(feedLog);
        telegramMessageService.sendMessage(chatId,
                String.format("✅ Logged %.1f kg of %s for batch %s on %s%s",
                        amount, feedType, batchName, date, (timeStr != null ? " at " + timeStr : "")));
    }

    private void handleSample(String chatId, String text) {
        Matcher matcher = SAMPLE_PATTERN.matcher(text.trim());
        if (!matcher.matches()) {
            telegramMessageService.sendMessage(chatId,
                    "❌ Invalid Sample format. Use: <code>S [BatchName] [FishType] [Pieces] [TotalWeight]</code>");
            return;
        }

        String batchName = matcher.group(1);
        String fishTypeName = matcher.group(2);
        int pieces = Integer.parseInt(matcher.group(3));
        double totalWeight = Double.parseDouble(matcher.group(4));

        Optional<Batch> batchOpt = batchRepository.findByName(batchName);
        if (batchOpt.isEmpty()) {
            telegramMessageService.sendMessage(chatId, "❌ Batch not found: " + batchName);
            return;
        }

        Optional<FishType> fishTypeOpt = fishTypeRepository.findByName(fishTypeName);
        if (fishTypeOpt.isEmpty()) {
            telegramMessageService.sendMessage(chatId, "❌ Fish type not found: " + fishTypeName);
            return;
        }

        double averageWeight = totalWeight * 1000 / pieces; // Convert to grams

        SampleLog sampleLog = SampleLog.builder()
                .batchId(batchOpt.get().getId())
                .fishTypeId(fishTypeOpt.get().getId())
                .date(LocalDate.now())
                .sampleCount(pieces)
                .totalSampleWeight(totalWeight)
                .averageWeight(averageWeight)
                .tenantId(TenantContext.getTenantId())
                .build();

        sampleService.createSampleLog(sampleLog);
        telegramMessageService.sendMessage(chatId,
                String.format("✅ Logged sample for %s in batch %s: %d pcs, %.2f kg (Avg: %.1f g)",
                        fishTypeName, batchName, pieces, totalWeight, averageWeight));
    }

    private void handleFeedInventory(String chatId) {
        List<FeedStock> stocks = feedStockRepository.findByTenantId(TenantContext.getTenantId());
        if (stocks.isEmpty()) {
            telegramMessageService.sendMessage(chatId, "📦 No feed inventory records found.");
            return;
        }

        StringBuilder sb = new StringBuilder("📦 <b>Current Feed Inventory</b>:\n\n");
        for (FeedStock stock : stocks) {
            String status = stock.isCriticalStock() ? "🔴" : (stock.isLowStock() ? "🟡" : "🟢");
            sb.append(String.format("%s %s: <b>%.1f %s</b> (Min: %.0f)\n",
                    status, stock.getFeedType(), stock.getCurrentStock(), stock.getUnit(), stock.getThreshold()));
        }
        telegramMessageService.sendMessage(chatId, sb.toString());
    }

    private void handleFeedOrder(String chatId, String text) {
        Matcher matcher = FEED_ORDER_PATTERN.matcher(text.trim());
        if (!matcher.matches()) {
            telegramMessageService.sendMessage(chatId,
                    "❌ Invalid Order format. Use: <code>FO [Supplier] [FeedType] [Quantity]</code>");
            return;
        }

        String supplierName = matcher.group(1);
        String feedType = matcher.group(2);
        double quantity = Double.parseDouble(matcher.group(3));

        Optional<Supplier> supplierOpt = supplierRepository.findByNameAndTenantId(supplierName,
                TenantContext.getTenantId());
        if (supplierOpt.isEmpty()) {
            telegramMessageService.sendMessage(chatId, "❌ Supplier not found: " + supplierName);
            return;
        }

        FeedOrder order = FeedOrder.builder()
                .supplierId(supplierOpt.get().getId())
                .feedType(feedType)
                .quantity(quantity)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .tenantId(TenantContext.getTenantId())
                .build();

        FeedOrder saved = feedOrderService.createOrder(order);
        String shortId = saved.getId().substring(0, 8);
        telegramMessageService.sendMessage(chatId,
                String.format(
                        "✅ Order <code>%s</code> placed for %.0f kg of %s with %s.\n\n➡️ <b>Next Step</b>: Send <code>FC %s</code> to confirm once the supplier acknowledges.",
                        shortId, quantity, feedType, supplierName, shortId));
    }

    private void handleFeedConfirm(String chatId, String text) {
        Matcher matcher = FEED_CONFIRM_PATTERN.matcher(text.trim());
        if (!matcher.matches()) {
            telegramMessageService.sendMessage(chatId, "❌ Invalid format. Use: <code>FC [OrderID]</code>");
            return;
        }

        String partialId = matcher.group(1);
        Optional<FeedOrder> orderOpt = findOrderByPartialId(partialId);

        if (orderOpt.isEmpty()) {
            telegramMessageService.sendMessage(chatId, "❌ Order not found: " + partialId);
            return;
        }

        feedOrderService.confirmOrder(orderOpt.get().getId());
        String shortId = orderOpt.get().getId().substring(0, 8);
        telegramMessageService.sendMessage(chatId,
                "✅ Order <code>" + shortId + "</code> confirmed.\n\n➡️ <b>Next Step</b>: Send <code>FR " + shortId
                        + "</code> when the feed is delivered to the farm.");
    }

    private void handleFeedReceive(String chatId, String text) {
        Matcher matcher = FEED_RECEIVE_PATTERN.matcher(text.trim());
        if (!matcher.matches()) {
            telegramMessageService.sendMessage(chatId, "❌ Invalid format. Use: <code>FR [OrderID]</code>");
            return;
        }

        String partialId = matcher.group(1);
        Optional<FeedOrder> orderOpt = findOrderByPartialId(partialId);

        if (orderOpt.isEmpty()) {
            telegramMessageService.sendMessage(chatId, "❌ Order not found: " + partialId);
            return;
        }

        feedOrderService.receiveOrder(orderOpt.get().getId());
        telegramMessageService.sendMessage(chatId,
                "✅ Order <code>" + orderOpt.get().getId().substring(0, 8)
                        + "</code> received. Inventory updated.\n\n➡️ <b>Next Step</b>: Send <code>FI</code> to check your updated stock levels.");
    }

    private Optional<FeedOrder> findOrderByPartialId(String partialId) {
        return feedOrderRepository.findByTenantId(TenantContext.getTenantId()).stream()
                .filter(o -> o.getId().startsWith(partialId))
                .findFirst();
    }
}
