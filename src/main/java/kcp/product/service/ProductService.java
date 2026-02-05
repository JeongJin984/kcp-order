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
import java.util.Set;
import java.util.stream.Collectors;

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
        // 1. 수정 대상 상품 조회
        ProductJpaEntity product = productRepository.findById(cmd.productId())
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND,
                "존재하지 않는 상품입니다: id = {}", cmd.productId()));

        // 2. 카테고리 ID 중복 제거 및 일괄 조회
        List<Long> requestedCategoryIds = cmd.categoryIds().stream()
            .distinct()
            .toList();

        List<CategoryJpaEntity> categories = categoryRepository.findAllByIds(requestedCategoryIds);

        // 3. 존재하지 않는 카테고리 ID 추출 (주문 등록 로직과 통일)
        if (categories.size() != requestedCategoryIds.size()) {
            Set<Long> foundIds = categories.stream()
                .map(CategoryJpaEntity::getId)
                .collect(Collectors.toSet());

            List<Long> missingIds = requestedCategoryIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND,
                "존재하지 않는 카테고리 ID가 포함되어 있습니다: {}", missingIds);
        }

        // 4. 상품 정보 업데이트 및 N:M 관계 갱신
        product.updateProductNotNull(cmd.name(), cmd.price(), cmd.stockQuantity(), categories);

        // 5. 변경 감지(Dirty Checking)를 통한 업데이트 및 결과 반환
        // JPA는 트랜잭션 종료 시 변경 사항을 자동 반영하므로 save 호출이 필수는 아니나,
        // 명시성을 위해 남겨두거나 Repository 구조에 따라 유지합니다.
        return ProductDetail.from(productRepository.save(product));
    }
}
