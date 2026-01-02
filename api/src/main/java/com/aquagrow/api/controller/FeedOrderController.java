package com.aquagrow.api.controller;

import java.util.Objects;

import com.aquagrow.api.dto.OrderMessageResponse;
import com.aquagrow.api.model.FeedOrder;
import com.aquagrow.api.model.FeedStock;
import com.aquagrow.api.model.OrderStatus;
import com.aquagrow.api.model.Supplier;
import com.aquagrow.api.repository.FeedOrderRepository;
import com.aquagrow.api.repository.FeedStockRepository;
import com.aquagrow.api.repository.SupplierRepository;
import com.aquagrow.api.service.WhatsAppMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/feed-orders")
@CrossOrigin(origins = "*")
public class FeedOrderController {

    @Autowired
    private FeedOrderRepository feedOrderRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private FeedStockRepository feedStockRepository;

    @Autowired
    private WhatsAppMessageService whatsAppMessageService;

    @GetMapping
    public List<FeedOrder> getAllOrders() {
        return feedOrderRepository.findAll();
    }

    @PostMapping
    public FeedOrder createOrder(@RequestBody FeedOrder order) {
        String tenantId = com.aquagrow.api.security.TenantContext.getTenantId();
        order.setTenantId(tenantId);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(com.aquagrow.api.model.PaymentStatus.UNPAID);
        return feedOrderRepository.save(order);
    }

    @GetMapping("/{id}/message")
    public ResponseEntity<OrderMessageResponse> getOrderMessage(
            @PathVariable String id,
            @RequestParam(required = false) String contactPerson,
            @RequestParam(required = false) String location) {

        return feedOrderRepository.findById(id)
                .flatMap(order -> supplierRepository.findById(Objects.requireNonNull(order.getSupplierId()))
                        .map(supplier -> {
                            String message = whatsAppMessageService.generateOrderMessage(
                                    order,
                                    supplier,
                                    contactPerson != null ? contactPerson : "Manager",
                                    location != null ? location : "Farm");

                            String whatsappLink = whatsAppMessageService.getWhatsAppWebLink(
                                    supplier.getWhatsappNumber() != null ? supplier.getWhatsappNumber()
                                            : supplier.getPhone(),
                                    message);

                            OrderMessageResponse response = new OrderMessageResponse(
                                    message,
                                    whatsappLink,
                                    supplier.getWhatsappNumber() != null ? supplier.getWhatsappNumber()
                                            : supplier.getPhone());

                            return ResponseEntity.ok(response);
                        }))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<FeedOrder> confirmOrder(@PathVariable String id) {
        return feedOrderRepository.findById(id)
                .map(order -> {
                    order.setStatus(OrderStatus.CONFIRMED);
                    order.setConfirmationDate(LocalDateTime.now());
                    return ResponseEntity.ok(feedOrderRepository.save(order));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/receive")
    public ResponseEntity<FeedOrder> receiveOrder(@PathVariable String id) {
        return feedOrderRepository.findById(id)
                .map(order -> {
                    order.setStatus(OrderStatus.RECEIVED);
                    order.setReceivedDate(LocalDateTime.now());

                    // Update stock levels - rely on Filter to find stock for this tenant
                    feedStockRepository.findAll().stream()
                            .filter(stock -> stock.getFeedType().equals(order.getFeedType()))
                            .findFirst()
                            .ifPresent(stock -> {
                                stock.setCurrentStock(stock.getCurrentStock() + order.getQuantity());
                                feedStockRepository.save(stock);
                            });

                    return ResponseEntity.ok(feedOrderRepository.save(order));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<FeedOrder> updatePaymentStatus(
            @PathVariable String id,
            @RequestParam com.aquagrow.api.model.PaymentStatus status) {
        return feedOrderRepository.findById(id)
                .map(order -> {
                    order.setPaymentStatus(status);
                    return ResponseEntity.ok(feedOrderRepository.save(order));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable String id) {
        return feedOrderRepository.findById(id)
                .map(order -> {
                    order.setStatus(OrderStatus.CANCELLED);
                    feedOrderRepository.save(order);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
