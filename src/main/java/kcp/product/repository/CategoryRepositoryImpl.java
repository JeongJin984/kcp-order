package kcp.product.repository;

import kcp.product.service.entity.CategoryJpaEntity;
import kcp.product.repository.jpa.CategoryJpaRepository;
import kcp.product.service.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {
    private final CategoryJpaRepository categoryJpaRepository;

    @Override
    public Optional<CategoryJpaEntity> findById(Long id) {
        return categoryJpaRepository.findById(id);
    }

    @Override
    public List<CategoryJpaEntity> findAllByIds(List<Long> ids) {
        return categoryJpaRepository.findAllById(ids);
    }


}
