package kcp.order.repository;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import kcp.order.service.dto.OrderDetail;
import kcp.order.service.dto.OrderSearchCmd;
import kcp.order.service.entity.OrderJpaEntity;
import kcp.order.service.repository.OrderRepository;
import kcp.order.repository.jpa.OrderJpaRepository;
import kcp.order.repository.predicate.OrderSearchPredicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

import static kcp.order.service.entity.QOrderItemJpaEntity.orderItemJpaEntity;
import static kcp.order.service.entity.QOrderJpaEntity.orderJpaEntity;
import static kcp.product.service.entity.QProductJpaEntity.productJpaEntity;

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
                .join(orderItemJpaEntity.product, productJpaEntity).fetchJoin()
                .where(orderJpaEntity.id.eq(id))
                .distinct()
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint("jakarta.persistence.lock.timeout", 3000)
                .fetchOne()
        );
    }

    @Override
    public Page<OrderDetail> findSliceOrderByOrderDate(OrderSearchCmd cmd, Pageable page) {

        // 1) Order Entity 조회 (ToOne 관계만 Fetch Join 가능)
        // 컬렉션(OneToMany)은 Fetch Join 하지 않습니다 (데이터 뻥튀기 방지)
        List<OrderJpaEntity> orders = queryFactory
            .selectFrom(orderJpaEntity)
            .where(OrderSearchPredicate.from(cmd))
            .orderBy(orderJpaEntity.orderDate.desc(), orderJpaEntity.id.desc())
            .offset(page.getOffset())
            .limit(page.getPageSize())
            .fetch();

        // 2) Entity -> DTO 변환
        // 이 시점에 orders.stream().map(...)을 할 때,
        // order.getOrderItems()를 호출하는 순간 Hibernate가
        // 설정한 batch_size 만큼의 ID를 모아서 'IN' 쿼리로 한방에 가져옵니다.
        List<OrderDetail> content = orders.stream()
            .map(order -> new OrderDetail(
                order.getId(),
                order.getStatus(),
                order.getOrderDate(),
                order.getOrderItems().stream() // 여기서 배치 로딩 발동 (N+1 문제 해결)
                    .map(item -> new OrderDetail.OrderItem(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getCount(),
                        item.getOrderPrice()
                    ))
                    .collect(Collectors.toList())
            ))
            .collect(Collectors.toList());

        // 3) Count 쿼리
        JPAQuery<Long> countQuery = queryFactory
            .select(orderJpaEntity.count())
            .from(orderJpaEntity)
            .where(OrderSearchPredicate.from(cmd));

        return PageableExecutionUtils.getPage(content, page, countQuery::fetchOne);
    }

    @Override
    public Long count(OrderSearchCmd command) {
        return queryFactory
            .select(orderJpaEntity.count())
            .from(orderJpaEntity)
            .where(OrderSearchPredicate.from(command)) // byCategoryId는 EXISTS 권장 (아래 참고)
            .fetchOne();
    }
}
