package com.aquagrow.api.controller;

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

    private String getTenantId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getClaimAsString("tenant_id");
    }

    @GetMapping
    public List<FeedOrder> getAllOrders(Authentication authentication) {
        String tenantId = getTenantId(authentication);
        return feedOrderRepository.findByTenantIdOrderByOrderDateDesc(tenantId);
    }

    @PostMapping
    public FeedOrder createOrder(@RequestBody FeedOrder order, Authentication authentication) {
        String tenantId = getTenantId(authentication);
        order.setTenantId(tenantId);
        order.setStatus(OrderStatus.PENDING);
        return feedOrderRepository.save(order);
    }

    @GetMapping("/{id}/message")
    public ResponseEntity<OrderMessageResponse> getOrderMessage(
            @PathVariable String id,
            @RequestParam(required = false) String contactPerson,
            @RequestParam(required = false) String location,
            Authentication authentication) {
        String tenantId = getTenantId(authentication);

        return feedOrderRepository.findById(id)
                .filter(order -> order.getTenantId().equals(tenantId))
                .flatMap(order -> supplierRepository.findById(order.getSupplierId())
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
    public ResponseEntity<FeedOrder> confirmOrder(@PathVariable String id, Authentication authentication) {
        String tenantId = getTenantId(authentication);

        return feedOrderRepository.findById(id)
                .filter(order -> order.getTenantId().equals(tenantId))
                .map(order -> {
                    order.setStatus(OrderStatus.CONFIRMED);
                    order.setConfirmationDate(LocalDateTime.now());
                    return ResponseEntity.ok(feedOrderRepository.save(order));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/receive")
    public ResponseEntity<FeedOrder> receiveOrder(@PathVariable String id, Authentication authentication) {
        String tenantId = getTenantId(authentication);

        return feedOrderRepository.findById(id)
                .filter(order -> order.getTenantId().equals(tenantId))
                .map(order -> {
                    order.setStatus(OrderStatus.RECEIVED);
                    order.setReceivedDate(LocalDateTime.now());

                    // Update stock levels
                    feedStockRepository.findByTenantId(tenantId).stream()
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable String id, Authentication authentication) {
        String tenantId = getTenantId(authentication);

        return feedOrderRepository.findById(id)
                .filter(order -> order.getTenantId().equals(tenantId))
                .map(order -> {
                    order.setStatus(OrderStatus.CANCELLED);
                    feedOrderRepository.save(order);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
