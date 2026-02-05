package kcp.order.service.repository;

import kcp.order.service.dto.OrderDetail;
import kcp.order.service.dto.OrderSearchCmd;
import kcp.order.service.entity.OrderJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository {
    Optional<OrderJpaEntity> findById(Long id);
    OrderJpaEntity save(OrderJpaEntity order);
    OrderJpaEntity saveAndFlush(OrderJpaEntity order);
    Optional<OrderJpaEntity> findByIdAndLock(Long id);
    Page<OrderDetail> findSliceOrderByOrderDate(OrderSearchCmd cmd, Pageable page);
    Long count(OrderSearchCmd command);
}
