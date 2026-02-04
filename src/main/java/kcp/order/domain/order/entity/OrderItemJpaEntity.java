package kcp.order.domain.order.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import kcp.order.common.exception.BusinessException;
import kcp.order.common.exception.ErrorCode;
import kcp.order.domain.product.entity.ProductJpaEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class OrderItemJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductJpaEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderJpaEntity order;

    @Column(name = "order_price")
    private BigDecimal orderPrice; // 주문 당시 가격

    @Column(name = "count")
    private int count;       // 주문 수량

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    @LastModifiedDate
    private LocalDateTime modifiedAt;

    public static OrderItemJpaEntity createOrderItem(ProductJpaEntity product, BigDecimal price, int count) {
        if(count <= 0 || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "invalid order item: count = {}, price = {}", count, price);
        }
        OrderItemJpaEntity orderItem = new OrderItemJpaEntity();
        orderItem.product = product;
        orderItem.orderPrice = price;
        orderItem.count = count;
        return orderItem;
    }

    public void assignOrder(OrderJpaEntity order) {
        this.order = order;
    }

    public void reduceProductStock() {
        product.decreaseStock(count);
    }

    public void restoreProductStock() {
        product.increaseStock(count);
    }
}