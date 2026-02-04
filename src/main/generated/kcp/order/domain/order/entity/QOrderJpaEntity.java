package kcp.order.domain.order.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrderJpaEntity is a Querydsl query type for OrderJpaEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrderJpaEntity extends EntityPathBase<OrderJpaEntity> {

    private static final long serialVersionUID = 1896306279L;

    public static final QOrderJpaEntity orderJpaEntity = new QOrderJpaEntity("orderJpaEntity");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> modifiedAt = createDateTime("modifiedAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> orderDate = createDateTime("orderDate", java.time.LocalDateTime.class);

    public final ListPath<OrderItemJpaEntity, QOrderItemJpaEntity> orderItems = this.<OrderItemJpaEntity, QOrderItemJpaEntity>createList("orderItems", OrderItemJpaEntity.class, QOrderItemJpaEntity.class, PathInits.DIRECT2);

    public final EnumPath<OrderStatus> status = createEnum("status", OrderStatus.class);

    public QOrderJpaEntity(String variable) {
        super(OrderJpaEntity.class, forVariable(variable));
    }

    public QOrderJpaEntity(Path<? extends OrderJpaEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QOrderJpaEntity(PathMetadata metadata) {
        super(OrderJpaEntity.class, metadata);
    }

}

