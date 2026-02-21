package com.aquagrow.api.service;

import com.aquagrow.api.model.*;
import com.aquagrow.api.repository.BatchRepository;
import com.aquagrow.api.repository.EmployeeRepository;
import com.aquagrow.api.repository.FishTypeRepository;
import com.aquagrow.api.security.TenantContext;
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
public class TelegramDataEntryService {

    private final EmployeeRepository employeeRepository;
    private final BatchRepository batchRepository;
    private final FishTypeRepository fishTypeRepository;
    private final FeedService feedService;
    private final SampleService sampleService;
    private final TelegramMessageService telegramMessageService;

    // F [BatchName] [FeedType] [Amount]
    private static final Pattern FEED_PATTERN = Pattern.compile("^F\\s+(\\S+)\\s+(\\S+)\\s+(\\d+(\\.\\d+)?)$",
            Pattern.CASE_INSENSITIVE);
    // S [BatchName] [FishType] [Count] [TotalWeight]
    private static final Pattern SAMPLE_PATTERN = Pattern
            .compile("^S\\s+(\\S+)\\s+(\\S+)\\s+(\\d+)\\s+(\\d+(\\.\\d+)?)$", Pattern.CASE_INSENSITIVE);

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
                        "- <b>Feed</b>: <code>F [BatchName] [FeedType] [Amount]</code>\n" +
                        "  <i>Example: F B1 Grow1 50</i>\n\n" +
                        "- <b>Sample</b>: <code>S [BatchName] [FishType] [Pieces] [TotalWeight]</code>\n" +
                        "  <i>Example: S B1 Tilapia 10 2.5</i>");
            } else if (cleanText.startsWith("F") || cleanText.startsWith("f")) {
                handleFeed(chatId, cleanText);
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
}
