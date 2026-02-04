package kcp.order.domain.order.entity;

import jakarta.persistence.*;
import kcp.order.common.exception.InvalidOrderStatusException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
        orderJpaEntity.status = OrderStatus.WAIT;
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

    // 비즈니스 로직: 주문 완료 (재고 차감 발생)
    public void complete() {
        if (this.status != OrderStatus.ACCEPTED) {
            throw new InvalidOrderStatusException(this.status, OrderStatus.ACCEPTED);
        }
        this.status = OrderStatus.COMPLETED;
        orderItems.forEach(OrderItemJpaEntity::reduceProductStock);
    }

    // 비즈니스 로직: 주문 취소 (완료 상태였다면 재고 복구)
    public void cancel() {
        if (this.status == OrderStatus.COMPLETED) {
            orderItems.forEach(OrderItemJpaEntity::restoreProductStock);
        }
        this.status = OrderStatus.CANCELED;
    }

    public void accept() {
        if (this.status != OrderStatus.WAIT) {
            throw new InvalidOrderStatusException(this.status, OrderStatus.WAIT);
        }
        this.status = OrderStatus.ACCEPTED;
    }
}