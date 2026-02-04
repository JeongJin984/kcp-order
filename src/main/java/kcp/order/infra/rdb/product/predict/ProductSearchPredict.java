package kcp.order.infra.rdb.product.predict;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import kcp.order.domain.product.dto.ProductSearchCmd;
import lombok.NoArgsConstructor;

import static kcp.order.domain.product.entity.QProductJpaEntity.productJpaEntity;
import static kcp.order.domain.product.entity.QProductCategoryJpaEntity.productCategoryJpaEntity;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ProductSearchPredict {
    public static BooleanExpression[] from(ProductSearchCmd command) {
        return new BooleanExpression[] {
            byProductName(command),
            byCategoryId(command)
        };
    }

    private static BooleanExpression byProductName(ProductSearchCmd command) {
        return command.productName() == null ? null : productJpaEntity.name.startsWithIgnoreCase(command.productName());
    }

    private static BooleanExpression byCategoryId(ProductSearchCmd command) {
        if (command.categoryId() == null) return null;

        return JPAExpressions
            .selectOne()
            .from(productCategoryJpaEntity)
            .where(
                productCategoryJpaEntity.product.id.eq(productJpaEntity.id),
                productCategoryJpaEntity.category.id.eq(command.categoryId())
            )
            .exists();
    }
}
