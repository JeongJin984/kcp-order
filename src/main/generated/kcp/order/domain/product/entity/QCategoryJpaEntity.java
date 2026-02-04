package kcp.order.domain.product.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCategoryJpaEntity is a Querydsl query type for CategoryJpaEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCategoryJpaEntity extends EntityPathBase<CategoryJpaEntity> {

    private static final long serialVersionUID = 127696616L;

    public static final QCategoryJpaEntity categoryJpaEntity = new QCategoryJpaEntity("categoryJpaEntity");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> modifiedAt = createDateTime("modifiedAt", java.time.LocalDateTime.class);

    public final StringPath name = createString("name");

    public QCategoryJpaEntity(String variable) {
        super(CategoryJpaEntity.class, forVariable(variable));
    }

    public QCategoryJpaEntity(Path<? extends CategoryJpaEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCategoryJpaEntity(PathMetadata metadata) {
        super(CategoryJpaEntity.class, metadata);
    }

}

