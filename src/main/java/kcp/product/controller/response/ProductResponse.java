package kcp.product.controller.response;

import kcp.product.service.dto.ProductDetail;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record ProductResponse (
    Long id,
    String name,
    BigDecimal price,
    int stockQuantity,
    List<Category> categories
) {

    public static ProductResponse from(ProductDetail product) {
        return ProductResponse.builder()
                .id(product.product().id())
                .name(product.product().name())
                .price(product.product().price())
                .stockQuantity(product.product().stockQuantity())
                .categories(
                    product.categories().stream()
                        .map(c -> new Category(c.id(), c.name())).toList()
                )
                .build();
    }

    public record Category(Long id, String name){}
}