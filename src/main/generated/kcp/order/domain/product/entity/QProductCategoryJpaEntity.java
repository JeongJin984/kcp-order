package kcp.order.domain.product.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProductCategoryJpaEntity is a Querydsl query type for ProductCategoryJpaEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductCategoryJpaEntity extends EntityPathBase<ProductCategoryJpaEntity> {

    private static final long serialVersionUID = -1855931543L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProductCategoryJpaEntity productCategoryJpaEntity = new QProductCategoryJpaEntity("productCategoryJpaEntity");

    public final QCategoryJpaEntity category;

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> modifiedAt = createDateTime("modifiedAt", java.time.LocalDateTime.class);

    public final QProductJpaEntity product;

    public QProductCategoryJpaEntity(String variable) {
        this(ProductCategoryJpaEntity.class, forVariable(variable), INITS);
    }

    public QProductCategoryJpaEntity(Path<? extends ProductCategoryJpaEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProductCategoryJpaEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProductCategoryJpaEntity(PathMetadata metadata, PathInits inits) {
        this(ProductCategoryJpaEntity.class, metadata, inits);
    }

    public QProductCategoryJpaEntity(Class<? extends ProductCategoryJpaEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.category = inits.isInitialized("category") ? new QCategoryJpaEntity(forProperty("category")) : null;
        this.product = inits.isInitialized("product") ? new QProductJpaEntity(forProperty("product")) : null;
    }

}

