package kcp.order.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public record RegisterOrderReq (
    @Valid List<OrderItemRequest> items
) {
    public record OrderItemRequest (
        @NotNull(message = "상품 ID는 필수입니다.") Long productId,
        @Min(value = 1, message = "주문 수량은 최소 1개여야 합니다.") int count
    ) {
    }
}