package kcp.order.domain.product.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProductJpaEntity is a Querydsl query type for ProductJpaEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductJpaEntity extends EntityPathBase<ProductJpaEntity> {

    private static final long serialVersionUID = 675402695L;

    public static final QProductJpaEntity productJpaEntity = new QProductJpaEntity("productJpaEntity");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> modifiedAt = createDateTime("modifiedAt", java.time.LocalDateTime.class);

    public final StringPath name = createString("name");

    public final NumberPath<java.math.BigDecimal> price = createNumber("price", java.math.BigDecimal.class);

    public final ListPath<ProductCategoryJpaEntity, QProductCategoryJpaEntity> productCategories = this.<ProductCategoryJpaEntity, QProductCategoryJpaEntity>createList("productCategories", ProductCategoryJpaEntity.class, QProductCategoryJpaEntity.class, PathInits.DIRECT2);

    public final NumberPath<Integer> stockQuantity = createNumber("stockQuantity", Integer.class);

    public QProductJpaEntity(String variable) {
        super(ProductJpaEntity.class, forVariable(variable));
    }

    public QProductJpaEntity(Path<? extends ProductJpaEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProductJpaEntity(PathMetadata metadata) {
        super(ProductJpaEntity.class, metadata);
    }

}

