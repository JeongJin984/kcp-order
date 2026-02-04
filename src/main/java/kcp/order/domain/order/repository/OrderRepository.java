package kcp.order.domain.order.repository;

import kcp.order.domain.order.dto.OrderDetail;
import kcp.order.domain.order.dto.OrderSearchCmd;
import kcp.order.domain.order.entity.OrderJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository {
    Optional<OrderJpaEntity> findById(Long id);
    OrderJpaEntity save(OrderJpaEntity order);
    Optional<OrderJpaEntity> findByIdWithProductAndLock(Long id);
    Page<OrderDetail> findSliceOrderByOrderDate(OrderSearchCmd cmd, Pageable page);
    Long count(OrderSearchCmd command);
}
