package com.aquagrow.api.service.telegram;

import com.aquagrow.api.model.Batch;
import com.aquagrow.api.model.FishType;
import com.aquagrow.api.model.SampleLog;
import com.aquagrow.api.repository.BatchRepository;
import com.aquagrow.api.repository.FishTypeRepository;
import com.aquagrow.api.security.TenantContext;
import com.aquagrow.api.service.SampleService;
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
public class SampleCommandHandler implements TelegramCommandHandler {

    private final BatchRepository batchRepository;
    private final FishTypeRepository fishTypeRepository;
    private final SampleService sampleService;
    private final TelegramMessageService telegramMessageService;

    // S [BatchName] [FishType] [Count] [TotalWeight]
    private static final Pattern SAMPLE_PATTERN = Pattern
            .compile("^S\\s+(\\S+)\\s+(\\S+)\\s+(\\d+)\\s+(\\d+(\\.\\d+)?)$", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean canHandle(String text) {
        String upperText = text.trim().toUpperCase();
        return upperText.startsWith("S ");
    }

    @Override
    public void handle(String chatId, String text) {
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
