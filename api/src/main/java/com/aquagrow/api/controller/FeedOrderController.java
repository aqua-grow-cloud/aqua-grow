package com.aquagrow.api.controller;

import java.util.Objects;
import java.util.List;

import com.aquagrow.api.dto.OrderMessageResponse;
import com.aquagrow.api.model.FeedOrder;
import com.aquagrow.api.model.PaymentStatus;
import com.aquagrow.api.repository.FeedOrderRepository;
import com.aquagrow.api.repository.SupplierRepository;
import com.aquagrow.api.service.FeedOrderService;
import com.aquagrow.api.security.TenantAuthorizationService;
import com.aquagrow.api.service.WhatsAppMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feed-orders")
@CrossOrigin(origins = "*")
public class FeedOrderController {

    @Autowired
    private FeedOrderService feedOrderService;

    @Autowired
    private FeedOrderRepository feedOrderRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private WhatsAppMessageService whatsAppMessageService;

    @Autowired
    private TenantAuthorizationService tenantAuthorizationService;

    @GetMapping
    public List<FeedOrder> getAllOrders() {
        return feedOrderService.getAllOrders();
    }

    @PostMapping
    public FeedOrder createOrder(@RequestBody FeedOrder order) {
        tenantAuthorizationService.requireRootTenant();
        return feedOrderService.createOrder(order);
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
        return feedOrderService.confirmOrder(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/receive")
    public ResponseEntity<FeedOrder> receiveOrder(@PathVariable String id) {
        return feedOrderService.receiveOrder(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<FeedOrder> updatePaymentStatus(
            @PathVariable String id,
            @RequestParam PaymentStatus status) {
        return feedOrderService.updatePaymentStatus(id, status)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable String id) {
        return feedOrderService.cancelOrder(id)
                .map(v -> ResponseEntity.ok().<Void>build())
                .orElse(ResponseEntity.notFound().build());
    }
}
