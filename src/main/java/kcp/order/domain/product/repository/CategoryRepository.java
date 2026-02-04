package kcp.order.domain.product.repository;

import kcp.order.domain.product.entity.CategoryJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository {
    Optional<CategoryJpaEntity> findById(Long id);
    List<CategoryJpaEntity> findAllByIds(List<Long> ids);
}
