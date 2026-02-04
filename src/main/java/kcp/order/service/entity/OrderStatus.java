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

    // 비즈니스 규칙: 특정 상태에서 변경 가능한지 검증
    public boolean canTransitionTo(OrderStatus nextStatus) {
        return switch (this) {
            case WAIT -> nextStatus == ACCEPTED || nextStatus == CANCELED;
            case ACCEPTED -> nextStatus == COMPLETED || nextStatus == CANCELED;
            case COMPLETED -> nextStatus == CANCELED; // 이미 완료된 주문도 취소는 가능할 수 있음 (정책에 따라)
            case CANCELED -> false; // 취소된 주문은 상태 변경 불가
        };
    }

    public static List<OrderStatus> of(String... statuses) {
        return Arrays.stream(statuses).map(OrderStatus::valueOf).toList();
    }
}