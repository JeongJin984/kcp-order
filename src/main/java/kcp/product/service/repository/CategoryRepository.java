package kcp.product.service.repository;

import kcp.product.service.entity.CategoryJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository {
    Optional<CategoryJpaEntity> findById(Long id);
    List<CategoryJpaEntity> findAllByIds(List<Long> ids);
}
