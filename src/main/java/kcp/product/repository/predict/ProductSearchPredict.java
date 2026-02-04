package kcp.product.repository.predict;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import kcp.product.service.dto.ProductSearchCmd;
import lombok.NoArgsConstructor;

import static kcp.product.service.entity.QProductCategoryJpaEntity.productCategoryJpaEntity;
import static kcp.product.service.entity.QProductJpaEntity.productJpaEntity;

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
