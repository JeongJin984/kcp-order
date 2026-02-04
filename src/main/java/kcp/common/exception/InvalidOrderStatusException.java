package kcp.common.exception;

import kcp.order.service.entity.OrderStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InvalidOrderStatusException extends BusinessException {
    public InvalidOrderStatusException(OrderStatus currentStatus, OrderStatus targetStatus) {
        super(ErrorCode.INVALID_ORDER_STATUS, "invalid order status transition : {} -> {}", currentStatus, targetStatus);
    }
}
