package kcp.order.domain.order.dto;

import kcp.order.domain.order.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderSearchCmd (
    LocalDateTime orderDateSt,
    LocalDateTime orderDateEd,
    String productName,

    List<OrderStatus> orderStatus
) {
}
