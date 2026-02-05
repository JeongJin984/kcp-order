package kcp.order.service.entity;

import jakarta.persistence.*;
import kcp.common.exception.InvalidOrderStatusException;
import kcp.product.service.entity.ProductJpaEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static kcp.order.service.entity.OrderStatus.*;

@Entity
@Table(name = "orders") // order는 예약어이므로 테이블명 지정
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class OrderJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status; // WAIT, ACCEPTED, COMPLETED, CANCELED

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItemJpaEntity> orderItems = new ArrayList<>();

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    @LastModifiedDate
    private LocalDateTime modifiedAt;

    // 생성 메서드
    public static OrderJpaEntity createOrder(List<OrderItemJpaEntity> orderItems) {
        OrderJpaEntity orderJpaEntity = new OrderJpaEntity();
        orderJpaEntity.status = WAIT;
        orderJpaEntity.orderDate = LocalDateTime.now();
        for (OrderItemJpaEntity item : orderItems) {
            orderJpaEntity.addOrderItem(item);
        }
        return orderJpaEntity;
    }

    private void addOrderItem(OrderItemJpaEntity orderItem) {
        orderItems.add(orderItem);
        orderItem.assignOrder(this);
    }

    public void updateOrderStatus(OrderStatus nextStatus) {
        if(canTransitionTo(nextStatus)) {
            switch (nextStatus) {
                case ACCEPTED -> accept();
                case COMPLETED ->complete(); // 내부에서 product.decreaseStock() 호출
                case CANCELED -> cancel();    // 내부에서 product.increaseStock() 호출
            }
        } else {
            throw new InvalidOrderStatusException(this.status, nextStatus);
        }

    }

    // 비즈니스 로직: 주문 완료 (재고 차감 발생)
    private void complete() {
        if (this.status != ACCEPTED) {
            throw new InvalidOrderStatusException(this.status, COMPLETED);
        }
        orderItems.forEach(OrderItemJpaEntity::reduceProductStock);
        this.status = COMPLETED;
    }

    // 비즈니스 로직: 주문 취소 (완료 상태였다면 재고 복구)
    private void cancel() {
        if (this.status == COMPLETED) {
            orderItems.forEach(OrderItemJpaEntity::restoreProductStock);
        }
        this.status = OrderStatus.CANCELED;
    }

    private void accept() {
        if (this.status != WAIT) {
            throw new InvalidOrderStatusException(this.status, WAIT);
        }
        this.status = ACCEPTED;
    }

    // 비즈니스 규칙: 특정 상태에서 변경 가능한지 검증
    public boolean canTransitionTo(OrderStatus nextStatus) {
        return switch (this.status) {
            case WAIT -> nextStatus == ACCEPTED || nextStatus == CANCELED;
            case ACCEPTED -> nextStatus == COMPLETED || nextStatus == CANCELED;
            case COMPLETED -> nextStatus == CANCELED; // 이미 완료된 주문도 취소는 가능할 수 있음 (정책에 따라)
            case CANCELED -> false; // 취소된 주문은 상태 변경 불가
        };
    }
}