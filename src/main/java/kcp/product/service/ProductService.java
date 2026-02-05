package kcp.product.service;

import kcp.common.exception.BusinessException;
import kcp.common.exception.ErrorCode;
import kcp.product.service.dto.ProductCreateCmd;
import kcp.product.service.dto.ProductDetail;
import kcp.product.service.dto.ProductSearchCmd;
import kcp.product.service.dto.ProductUpdateCmd;
import kcp.product.service.entity.CategoryJpaEntity;
import kcp.product.service.entity.ProductJpaEntity;
import kcp.product.service.repository.CategoryRepository;
import kcp.product.service.repository.ProductRepository;
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

    /**
     * 상품 조회(다건, 페이징)
     **/
    public Page<ProductDetail> getProducts(ProductSearchCmd command, Pageable pageable) {
        return productRepository.findSliceOrderByCreatedAt(command, pageable);
    }

    /**
     * 상품 조회(단건)
     **/
    public ProductDetail getProduct(Long productId) {
        ProductJpaEntity product = productRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "product not found: id = {}", productId));

        return ProductDetail.from(product);
    }


    @Transactional
    public ProductDetail updateProduct(ProductUpdateCmd cmd) {
        ProductJpaEntity product = productRepository.findById(cmd.productId())
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "product not found: id = {}", cmd.productId()));

        if (cmd.name() != null) product.setName(cmd.name());
        if (cmd.price() != null) product.setPrice(cmd.price());
        if (cmd.stockQuantity() != null) product.setStockQuantity(cmd.stockQuantity());

        return ProductDetail.from(productRepository.save(product));
    }
}
