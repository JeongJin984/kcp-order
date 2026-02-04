package kcp.order.service.dto;

import kcp.order.service.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderSearchCmd (
    LocalDateTime orderDateSt,
    LocalDateTime orderDateEd,
    String productName,

    List<OrderStatus> orderStatus
) {
}
