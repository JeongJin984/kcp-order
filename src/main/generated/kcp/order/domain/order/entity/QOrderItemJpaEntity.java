package kcp.order.domain.order.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrderItemJpaEntity is a Querydsl query type for OrderItemJpaEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrderItemJpaEntity extends EntityPathBase<OrderItemJpaEntity> {

    private static final long serialVersionUID = -1787218924L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrderItemJpaEntity orderItemJpaEntity = new QOrderItemJpaEntity("orderItemJpaEntity");

    public final NumberPath<Integer> count = createNumber("count", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> modifiedAt = createDateTime("modifiedAt", java.time.LocalDateTime.class);

    public final QOrderJpaEntity order;

    public final NumberPath<java.math.BigDecimal> orderPrice = createNumber("orderPrice", java.math.BigDecimal.class);

    public final kcp.order.domain.product.entity.QProductJpaEntity product;

    public QOrderItemJpaEntity(String variable) {
        this(OrderItemJpaEntity.class, forVariable(variable), INITS);
    }

    public QOrderItemJpaEntity(Path<? extends OrderItemJpaEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrderItemJpaEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrderItemJpaEntity(PathMetadata metadata, PathInits inits) {
        this(OrderItemJpaEntity.class, metadata, inits);
    }

    public QOrderItemJpaEntity(Class<? extends OrderItemJpaEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.order = inits.isInitialized("order") ? new QOrderJpaEntity(forProperty("order")) : null;
        this.product = inits.isInitialized("product") ? new kcp.order.domain.product.entity.QProductJpaEntity(forProperty("product")) : null;
    }

}

