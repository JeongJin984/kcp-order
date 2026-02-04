package kcp.order.domain.product.dto;

public record ProductSearchCmd (
    Long categoryId,
    String productName
) {

}
