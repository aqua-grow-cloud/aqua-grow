package com.aquagrow.api.service;

import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.FeedOrder;
import com.aquagrow.api.model.OrderStatus;
import com.aquagrow.api.model.PaymentStatus;
import com.aquagrow.api.repository.FeedOrderRepository;
import com.aquagrow.api.repository.FeedStockRepository;
import com.aquagrow.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedOrderService {

    private final FeedOrderRepository feedOrderRepository;
    private final FeedStockRepository feedStockRepository;

    @Transactional(readOnly = true)
    public List<FeedOrder> getAllOrders() {
        return feedOrderRepository.findAll();
    }

    @Transactional
    public FeedOrder createOrder(FeedOrder order) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }
        order.setTenantId(tenantId);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setOrderDate(LocalDateTime.now());
        return feedOrderRepository.save(order);
    }

    @Transactional
    public Optional<FeedOrder> confirmOrder(String id) {
        return feedOrderRepository.findById(id)
                .map(order -> {
                    order.setStatus(OrderStatus.CONFIRMED);
                    order.setConfirmationDate(LocalDateTime.now());
                    log.info("Confirmed order: {} for tenant: {}", id, order.getTenantId());
                    return feedOrderRepository.save(order);
                });
    }

    @Transactional
    public Optional<FeedOrder> receiveOrder(String id) {
        return feedOrderRepository.findById(id)
                .map(order -> {
                    if (order.getStatus() == OrderStatus.RECEIVED) {
                        return order;
                    }

                    order.setStatus(OrderStatus.RECEIVED);
                    order.setReceivedDate(LocalDateTime.now());

                    // Update stock levels
                    feedStockRepository.findByTenantIdAndFeedType(order.getTenantId(), order.getFeedType())
                            .ifPresentOrElse(stock -> {
                                stock.setCurrentStock(stock.getCurrentStock() + order.getQuantity());
                                feedStockRepository.save(stock);
                                log.info("Updated stock for {}: +{} kg (New balance: {} kg)",
                                        order.getFeedType(), order.getQuantity(), stock.getCurrentStock());
                            }, () -> {
                                log.warn("Feed stock record not found for type: {}. Cannot auto-update inventory.",
                                        order.getFeedType());
                            });

                    return feedOrderRepository.save(order);
                });
    }

    @Transactional
    public Optional<FeedOrder> updatePaymentStatus(String id, PaymentStatus status) {
        return feedOrderRepository.findById(id)
                .map(order -> {
                    order.setPaymentStatus(status);
                    return feedOrderRepository.save(order);
                });
    }

    @Transactional
    public Optional<Void> cancelOrder(String id) {
        return feedOrderRepository.findById(id)
                .map(order -> {
                    order.setStatus(OrderStatus.CANCELLED);
                    feedOrderRepository.save(order);
                    return null;
                });
    }
}
