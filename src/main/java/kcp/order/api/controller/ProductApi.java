package kcp.order.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kcp.order.api.request.UpdateProductReq;
import kcp.order.api.request.RegisterProductReq;
import kcp.order.api.response.ProductResponse;
import kcp.order.common.response.PaginationResponse;
import kcp.order.domain.product.dto.ProductCreateCmd;
import kcp.order.domain.product.dto.ProductDetail;
import kcp.order.domain.product.dto.ProductSearchCmd;
import kcp.order.domain.product.dto.ProductUpdateCmd;
import kcp.order.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "제품 api", description = "제품 api")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Validated
public class ProductApi {
    private final ProductService productService;

    @Operation(summary = "상품(다건) 조회", description = "조회 조건을 바탕으로 상품을 페이지네이션하여 검색합니다.")
    @GetMapping
    public PaginationResponse<ProductResponse> getProducts(
        @Parameter(description = "상품명")
        @RequestParam(name = "productName", required = false) String productName,
        @RequestParam(name = "categoryId", required = false) Long categoryId,
        @Parameter(description = "페이지 번호")
        @RequestParam(name = "page", required = false, defaultValue = "0") @Min(0) int pageNum,
        @Parameter(description = "Page size")
        @RequestParam(name = "size", required = false, defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        Page<ProductResponse> products = productService
            .getProducts(new ProductSearchCmd(categoryId, productName), PageRequest.of(pageNum, size))
            .map(ProductResponse::from);

        return PaginationResponse.from(products);
    }

    @Operation(summary = "상품(단건) 조회", description = "상품을 id를 활용하여 한건 검색합니다.")
    @GetMapping("/{productId}")
    public ProductResponse getProduct(
        @PathVariable Long productId
    ) {
        ProductDetail product = productService.getProduct(productId);
        return ProductResponse.from(product);
    }

    @Operation(summary = "상품 등록", description = "상품을 등록합니다.")
    @PostMapping
    public ProductResponse registerProduct(
        @Valid @RequestBody RegisterProductReq body
    ) {
        ProductDetail product = productService.createProduct(new ProductCreateCmd(
            body.categoryIds(),
            body.name(),
            body.price(),
            body.stockQuantity()
        ));
        return ProductResponse.from(product);
    }

    @Operation(summary = "상품 수정", description = "상품을 수정합니다.")
    @PostMapping("/{productId}")
    public ProductResponse updateProduct(
        @PathVariable Long productId,
        @Valid @RequestBody UpdateProductReq body
    ) {
        ProductDetail product = productService.updateProduct(new ProductUpdateCmd(
            productId,
            body.name(),
            body.price(),
            body.stockQuantity(),
            body.categoryIds()
        ));
        return ProductResponse.from(product);
    }
}
