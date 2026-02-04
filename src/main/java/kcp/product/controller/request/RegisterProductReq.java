package kcp.product.controller.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record RegisterProductReq(
    @NotBlank(message = "상품명은 필수이며 공백일 수 없습니다.")
    @Size(max = 100, message = "상품명은 100자 이내로 입력해주세요.")
    String name,

    @NotNull(message = "가격은 필수입니다.")
    @PositiveOrZero(message = "가격은 0원 이상이어야 합니다.")
    BigDecimal price,

    @Min(value = 0, message = "재고 수량은 0개 이상이어야 합니다.")
    int stockQuantity,

    @NotEmpty(message = "최소 하나 이상의 카테고리 ID가 필요합니다.")
    List<Long> categoryIds
) {
    public RegisterProductReq {
        categoryIds = (categoryIds != null) ? List.copyOf(categoryIds) : List.of();
    }
}