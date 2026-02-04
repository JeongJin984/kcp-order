package kcp.order.domain.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductUpdateCmd(
    Long productId,          // 수정할 대상 ID
    String name,            // (Optional) 변경할 이름
    BigDecimal price,             // (Optional) 변경할 가격
    Integer stockQuantity,  // (Optional) 변경할 재고
    List<Long> categoryIds  // (Optional) 변경할 카테고리 목록
) {
}