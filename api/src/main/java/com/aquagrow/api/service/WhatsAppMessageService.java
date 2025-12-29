package com.aquagrow.api.service;

import com.aquagrow.api.model.FeedOrder;
import com.aquagrow.api.model.Supplier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class WhatsAppMessageService {

    public String generateOrderMessage(FeedOrder order, Supplier supplier, String contactPerson, String location) {
        String template = supplier.getOrderTemplate();
        if (template == null || template.trim().isEmpty()) {
            template = """
                    {{supplierName}}
                    {{feedType}}
                    {{contactPerson}}
                    {{quantity}} kg
                    {{location}}

                    Cell - {{phone}}
                    """;
        }

        return template
                .replace("{{supplierName}}", supplier.getName() != null ? supplier.getName() : "")
                .replace("{{feedType}}", order.getFeedType() != null ? order.getFeedType() : "")
                .replace("{{contactPerson}}", contactPerson != null ? contactPerson : "")
                .replace("{{quantity}}", order.getQuantity() != null ? String.format("%.0f", order.getQuantity()) : "0")
                .replace("{{location}}", location != null ? location : "")
                .replace("{{phone}}",
                        (supplier.getWhatsappNumber() != null ? supplier.getWhatsappNumber()
                                : (supplier.getPhone() != null ? supplier.getPhone() : "")))
                .replace("{{date}}", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    public String getWhatsAppWebLink(String phoneNumber, String message) {
        // Format phone number (remove spaces, dashes, etc.)
        String formattedPhone = phoneNumber.replaceAll("[^0-9+]", "");

        // URL encode the message
        String encodedMessage = java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);

        return String.format("https://wa.me/%s?text=%s", formattedPhone, encodedMessage);
    }
}
