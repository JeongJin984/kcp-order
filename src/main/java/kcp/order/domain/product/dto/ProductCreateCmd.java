package kcp.order.domain.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductCreateCmd (
    List<Long> categoryId,
    String productName,
    BigDecimal price,
    int stockQuantity
) {
}
