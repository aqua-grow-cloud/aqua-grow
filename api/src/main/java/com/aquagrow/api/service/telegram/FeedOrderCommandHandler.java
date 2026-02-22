package com.aquagrow.api.service.telegram;

import com.aquagrow.api.model.*;
import com.aquagrow.api.repository.FeedOrderRepository;
import com.aquagrow.api.repository.FeedStockRepository;
import com.aquagrow.api.repository.SupplierRepository;
import com.aquagrow.api.security.TenantContext;
import com.aquagrow.api.service.FeedOrderService;
import com.aquagrow.api.service.TelegramMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedOrderCommandHandler implements TelegramCommandHandler {

    private final FeedOrderService feedOrderService;
    private final SupplierRepository supplierRepository;
    private final FeedStockRepository feedStockRepository;
    private final FeedOrderRepository feedOrderRepository;
    private final TelegramMessageService telegramMessageService;

    // Feed Order: FO [Supplier] [FeedType] [Quantity]
    private static final Pattern FEED_ORDER_PATTERN = Pattern.compile("^FO\\s+(\\S+)\\s+(.+?)\\s+(\\d+(?:\\.\\d+)?)$",
            Pattern.CASE_INSENSITIVE);

    // Feed Confirm: FC [OrderID]
    private static final Pattern FEED_CONFIRM_PATTERN = Pattern.compile("^FC\\s+(\\S+)$", Pattern.CASE_INSENSITIVE);

    // Feed Receive: FR [OrderID]
    private static final Pattern FEED_RECEIVE_PATTERN = Pattern.compile("^FR\\s+(\\S+)$", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean canHandle(String text) {
        String cleanText = text.trim().toUpperCase();
        return cleanText.equals("FI") || cleanText.startsWith("FI ") ||
                cleanText.startsWith("FO ") ||
                cleanText.startsWith("FC ") ||
                cleanText.startsWith("FR ");
    }

    @Override
    public void handle(String chatId, String text) {
        String cleanText = text.trim();
        if (cleanText.equalsIgnoreCase("FI")) {
            handleFeedInventory(chatId);
        } else if (cleanText.startsWith("FO") || cleanText.startsWith("fo")) {
            handleFeedOrder(chatId, cleanText);
        } else if (cleanText.startsWith("FC") || cleanText.startsWith("fc")) {
            handleFeedConfirm(chatId, cleanText);
        } else if (cleanText.startsWith("FR") || cleanText.startsWith("fr")) {
            handleFeedReceive(chatId, cleanText);
        }
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
