package kcp.order.service.entity;

import java.util.Arrays;
import java.util.List;

public enum OrderStatus {
    WAIT("대기"),
    ACCEPTED("접수"),
    COMPLETED("완료"),
    CANCELED("취소");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }



    public static List<OrderStatus> of(String... statuses) {
        return Arrays.stream(statuses).map(OrderStatus::valueOf).toList();
    }
}