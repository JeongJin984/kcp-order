package kcp.order.domain.order.dto;
import java.util.List;

public record OrderCreateCmd (
    List<OrderItem> items
) {
    public record OrderItem (Long productId, int count){}
}
