package com.aquagrow.api.service;

import com.aquagrow.api.model.Employee;
import com.aquagrow.api.repository.EmployeeRepository;
import com.aquagrow.api.security.TenantContext;
import com.aquagrow.api.service.telegram.TelegramCommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramDataEntryService {

    private final EmployeeRepository employeeRepository;
    private final TelegramMessageService telegramMessageService;
    private final List<TelegramCommandHandler> commandHandlers;

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
                sendHelpMessage(chatId);
            } else {
                boolean handled = false;
                for (TelegramCommandHandler handler : commandHandlers) {
                    if (handler.canHandle(cleanText)) {
                        handler.handle(chatId, cleanText);
                        handled = true;
                        break;
                    }
                }

                if (!handled) {
                    telegramMessageService.sendMessage(chatId, "❓ Unknown command. Type /help for instructions.");
                }
            }
        } catch (Exception e) {
            log.error("Error processing Telegram message: {}", text, e);
            telegramMessageService.sendMessage(chatId, "❌ Error processing message: " + e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    private void sendHelpMessage(String chatId) {
        telegramMessageService.sendMessage(chatId, "📖 <b>Available Commands</b>:\n\n" +
                "<b>Logging</b>:\n" +
                "- <b>Feed (Detailed)</b>: <code>[Date] [BatchName] [FeedType] [Amount] [Unit] [Time]</code>\n" +
                "  <i>Ex: 2026-02-10 2025-11-28 King Fish Feed 480 kg 07:00</i>\n" +
                "- <b>Sample</b>: <code>S [BatchName] [FishType] [Pieces] [TotalWeight]</code>\n" +
                "  <i>Ex: S B1 Tilapia 10 2.5</i>\n\n" +
                "<b>Feed Management</b>:\n" +
                "- <b>Inventory</b>: <code>FI</code>\n" +
                "- <b>Order</b>: <code>FO [Supplier] [FeedType] [Quantity]</code>\n" +
                "- <b>Confirm</b>: <code>FC [OrderID]</code>\n" +
                "- <b>Receive</b>: <code>FR [OrderID]</code>\n\n" +
                "<b>Financials</b>:\n" +
                "- <b>Log Expense</b>: <code>EX [Batch] [Category] [Amount] [Desc]</code>\n" +
                "- <b>Salary</b>: <code>PAY [Employee] [Base] [Bonus] [Desc]</code>\n" +
                "- <b>Summary</b>: <code>FIN [YYYY-MM]</code>\n\n" +
                "<b>AI Assistant</b>:\n" +
                "- <b>Ask AI</b>: <code>ASK [your question]</code>\n" +
                "  <i>Ex: ASK What is the ideal water temperature for tilapia?</i>\n\n" +
                "<b>Raw Data Log</b>:\n" +
                "- Paste a tab-separated row from your expense tracker directly.");
    }
}
