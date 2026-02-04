package kcp.order.domain.product.dto;

import kcp.order.domain.product.entity.ProductCategoryJpaEntity;
import kcp.order.domain.product.entity.ProductJpaEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductDetail (
    List<Category> categories,
    Product product
){
    public static ProductDetail from(ProductJpaEntity product) {
        return new ProductDetail(
            product.getProductCategories()
                .stream().map(v -> new Category(
                    v.getCategory().getId(), v.getCategory().getName(), v.getCreatedAt())
                ).toList(),
            new Product(
                product.getId(), product.getName(), product.getPrice(), product.getStockQuantity(), product.getCreatedAt()
            )
        );
    }

    public record Category(Long id, String name, LocalDateTime createdAt){}

    public record Product(
        Long id,
        String name,
        BigDecimal price,
        int stockQuantity,
        LocalDateTime createdAt
    ) {}
}
