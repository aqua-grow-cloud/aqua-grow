package com.aquagrow.api.service.telegram;

import com.aquagrow.api.model.Batch;
import com.aquagrow.api.model.FeedLog;
import com.aquagrow.api.repository.BatchRepository;
import com.aquagrow.api.security.TenantContext;
import com.aquagrow.api.service.FeedService;
import com.aquagrow.api.service.TelegramMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedCommandHandler implements TelegramCommandHandler {

    private final BatchRepository batchRepository;
    private final FeedService feedService;
    private final TelegramMessageService telegramMessageService;

    // Enhanced Feed Pattern: [Date] [BatchName] [FeedType] [Amount] [Unit] [Time]
    private static final Pattern ENHANCED_FEED_PATTERN = Pattern.compile(
            "^(?:(\\d{4}-\\d{2}-\\d{2})\\s+)?(\\S+)\\s+(.+?)\\s+(\\d+(?:\\.\\d+)?)\\s*(kg|g|KG|G)(?:\\s+(\\d{2}:\\d{2}))?$",
            Pattern.CASE_INSENSITIVE);

    // Old Feed Pattern (kept for backward compatibility or simple entries)
    private static final Pattern FEED_PATTERN = Pattern.compile("^F\\s+(\\S+)\\s+(\\S+)\\s+(\\d+(\\.\\d+)?)$",
            Pattern.CASE_INSENSITIVE);

    @Override
    public boolean canHandle(String text) {
        String cleanText = text.trim();
        String upperText = cleanText.toUpperCase();
        return upperText.startsWith("F ") || ENHANCED_FEED_PATTERN.matcher(cleanText).matches();
    }

    @Override
    public void handle(String chatId, String text) {
        String cleanText = text.trim();
        if (ENHANCED_FEED_PATTERN.matcher(cleanText).matches()) {
            handleEnhancedFeed(chatId, cleanText);
        } else {
            handleFeed(chatId, cleanText);
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
            return;
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
}
