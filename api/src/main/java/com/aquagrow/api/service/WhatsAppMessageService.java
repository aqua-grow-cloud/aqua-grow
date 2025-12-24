package com.aquagrow.api.service;

import com.aquagrow.api.model.FeedOrder;
import com.aquagrow.api.model.Supplier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class WhatsAppMessageService {

    public String generateOrderMessage(FeedOrder order, Supplier supplier, String contactPerson, String location) {
        return String.format("""
                %s
                %s
                %s
                %.0f kg
                %s

                Cell - %s
                """,
                supplier.getName(),
                order.getFeedType(),
                contactPerson,
                order.getQuantity(),
                location,
                supplier.getWhatsappNumber() != null ? supplier.getWhatsappNumber() : supplier.getPhone());
    }

    public String getWhatsAppWebLink(String phoneNumber, String message) {
        // Format phone number (remove spaces, dashes, etc.)
        String formattedPhone = phoneNumber.replaceAll("[^0-9+]", "");

        // URL encode the message
        String encodedMessage = java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);

        return String.format("https://wa.me/%s?text=%s", formattedPhone, encodedMessage);
    }
}
