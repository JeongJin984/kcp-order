package kcp.order.domain.product.service;

import kcp.order.common.exception.BusinessException;
import kcp.order.common.exception.ErrorCode;
import kcp.order.domain.product.dto.ProductCreateCmd;
import kcp.order.domain.product.dto.ProductDetail;
import kcp.order.domain.product.dto.ProductSearchCmd;
import kcp.order.domain.product.dto.ProductUpdateCmd;
import kcp.order.domain.product.entity.CategoryJpaEntity;
import kcp.order.domain.product.entity.ProductJpaEntity;
import kcp.order.domain.product.repository.CategoryRepository;
import kcp.order.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public ProductDetail createProduct(ProductCreateCmd cmd) {
        List<CategoryJpaEntity> category = categoryRepository.findAllByIds(cmd.categoryId());

        ProductJpaEntity product = ProductJpaEntity.createProduct(cmd.productName(), cmd.price(), cmd.stockQuantity(), category);

        return ProductDetail.from(productRepository.save(product));
    }

    // 카테고리별 목록 조회 (페이징)
    public Page<ProductDetail> getProducts(ProductSearchCmd command, Pageable pageable) {
        return productRepository.findSliceOrderByCreatedAt(command, pageable);
    }

    public ProductDetail getProduct(Long productId) {
        return productRepository.findById(productId)
            .map(ProductDetail::from)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "product not found: id = {}", productId));
    }

    public ProductDetail updateProduct(ProductUpdateCmd cmd) {
        ProductJpaEntity product = productRepository.findById(cmd.productId())
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "product not found: id = {}", cmd.productId()));

        if (cmd.name() != null) product.setName(cmd.name());
        if (cmd.price() != null) product.setPrice(cmd.price());
        if (cmd.stockQuantity() != null) product.setStockQuantity(cmd.stockQuantity());

        return ProductDetail.from(productRepository.save(product));
    }
}
