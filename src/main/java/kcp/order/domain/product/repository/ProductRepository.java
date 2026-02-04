package kcp.order.domain.product.repository;

import jakarta.validation.constraints.NotNull;
import kcp.order.domain.product.dto.ProductDetail;
import kcp.order.domain.product.dto.ProductSearchCmd;
import kcp.order.domain.product.entity.ProductJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Optional<ProductJpaEntity> findByIdWithLock(@NotNull(message = "상품 ID는 필수입니다.") Long productId);

    List<ProductJpaEntity> findByIdsWithLock(@NotNull(message = "상품 ID는 필수입니다.") List<Long> productId);

    Optional<ProductJpaEntity> findById(Long id);

    ProductJpaEntity save(ProductJpaEntity product);

    Page<ProductDetail> findSliceOrderByCreatedAt(ProductSearchCmd command, Pageable page);

    Long count(ProductSearchCmd command);
}
