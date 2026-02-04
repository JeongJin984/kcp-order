package kcp.order.infra.rdb.order;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import kcp.order.domain.order.dto.OrderDetail;
import kcp.order.domain.order.dto.OrderSearchCmd;
import kcp.order.domain.order.entity.OrderJpaEntity;
import kcp.order.domain.order.entity.OrderStatus;
import kcp.order.domain.order.repository.OrderRepository;
import kcp.order.domain.product.dto.ProductDetail;
import kcp.order.infra.rdb.order.jpa.OrderJpaRepository;
import kcp.order.infra.rdb.order.predicate.OrderSearchPredicate;
import kcp.order.infra.rdb.product.ProductRepositoryImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.list;
import static kcp.order.domain.order.entity.QOrderItemJpaEntity.orderItemJpaEntity;
import static kcp.order.domain.order.entity.QOrderJpaEntity.orderJpaEntity;
import static kcp.order.domain.product.entity.QProductJpaEntity.productJpaEntity;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {
    private final OrderJpaRepository orderJpaRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<OrderJpaEntity> findById(Long id) {
        return orderJpaRepository.findById(id);
    }

    @Override
    public OrderJpaEntity save(OrderJpaEntity order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<OrderJpaEntity> findByIdWithProductAndLock(Long id) {
        return Optional.ofNullable(
            queryFactory
                .selectFrom(orderJpaEntity)
                .join(orderJpaEntity.orderItems, orderItemJpaEntity).fetchJoin()
                .join(orderItemJpaEntity.product, productJpaEntity)
                .where(orderJpaEntity.id.eq(id))
                .distinct()
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint("javax.persistence.lock.timeout", 3000)
                .fetchOne()
        );
    }

    @Override
    public Page<OrderDetail> findSliceOrderByOrderDate(OrderSearchCmd cmd, Pageable page) {
        // 1) Book slice (정렬/커서/limit은 여기서만)
        List<Long> orderIds = queryFactory
            .select(orderJpaEntity.id)
            .from(orderJpaEntity)
            .where(OrderSearchPredicate.from(cmd)) // byCategoryId는 EXISTS 권장 (아래 참고)
            .orderBy(orderJpaEntity.orderDate.desc(), orderJpaEntity.id.desc())
            .offset(page.getOffset())
            .limit(page.getPageSize())
            .fetch();

        if (orderIds.isEmpty()) return Page.empty();

// 2단계: transform 대신 fetch() 사용
        List<OrderProductRow> orderRows = queryFactory
            .select(Projections.constructor(OrderProductRow.class,
                orderJpaEntity.id,
                orderJpaEntity.status,
                orderJpaEntity.orderDate,
                productJpaEntity.id,
                productJpaEntity.name,
                orderItemJpaEntity.count,
                orderItemJpaEntity.orderPrice
            ))
            .from(orderJpaEntity)
            .innerJoin(orderJpaEntity.orderItems, orderItemJpaEntity)
            .innerJoin(orderItemJpaEntity.product, productJpaEntity)
            .where(orderJpaEntity.id.in(orderIds))
            .fetch(); // transform()을 제거하고 List로 직접 받음

        // 3단계: 자바 Stream으로 그룹화 (기존 transform이 하던 일을 자바가 수행)
        Map<Long, List<OrderDetail.OrderItem>> itemsMap = orderRows.stream()
            .collect(Collectors.groupingBy(
                OrderProductRow::orderId,
                Collectors.mapping(r -> new OrderDetail.OrderItem(
                    r.productId(), r.productName(), r.count(), r.orderPrice()
                ), Collectors.toList())
            ));

        List<OrderDetail> orders = orderIds.stream()
            .map(id -> {
                OrderProductRow first = orderRows.stream()
                    .filter(r -> r.orderId().equals(id)).findFirst().orElse(null);
                return first == null ? null : new OrderDetail(
                    first.orderId(), first.status(), first.orderDate(), itemsMap.getOrDefault(id, List.of())
                );
            })
            .filter(Objects::nonNull)
            .toList();

        // 4) Count 쿼리 최적화 및 Page 객체 생성
        // PageableExecutionUtils는 내부적으로 (첫 페이지 & 결과 < pageSize)인 경우 count 쿼리를 생략합니다.
        JPAQuery<Long> countQuery = queryFactory
            .select(orderJpaEntity.count())
            .from(orderJpaEntity)
            .where(OrderSearchPredicate.from(cmd));

        return PageableExecutionUtils.getPage(orders, page, countQuery::fetchOne);
    }

    @Override
    public Long count(OrderSearchCmd command) {
        return queryFactory
            .select(orderJpaEntity.count())
            .from(orderJpaEntity)
            .where(OrderSearchPredicate.from(command)) // byCategoryId는 EXISTS 권장 (아래 참고)
            .fetchOne();
    }

    public record OrderProductRow(
        Long orderId,
        OrderStatus status,
        LocalDateTime orderDate,
        Long productId,
        String productName,
        int count,
        BigDecimal orderPrice
    ) {}
}
