package kcp.product.service.dto;

public record ProductSearchCmd (
    Long categoryId,
    String productName
) {

}
