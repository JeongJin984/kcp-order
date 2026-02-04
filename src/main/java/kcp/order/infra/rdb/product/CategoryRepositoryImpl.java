package kcp.order.infra.rdb.product;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kcp.order.domain.product.dto.ProductSearchCmd;
import kcp.order.domain.product.entity.CategoryJpaEntity;
import kcp.order.domain.product.entity.ProductJpaEntity;
import kcp.order.domain.product.repository.CategoryRepository;
import kcp.order.infra.rdb.product.jpa.CategoryJpaRepository;
import kcp.order.infra.rdb.product.predict.ProductSearchPredict;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static kcp.order.domain.product.entity.QProductJpaEntity.productJpaEntity;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {
    private final CategoryJpaRepository categoryJpaRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<CategoryJpaEntity> findById(Long id) {
        return categoryJpaRepository.findById(id);
    }

    @Override
    public List<CategoryJpaEntity> findAllByIds(List<Long> ids) {
        return categoryJpaRepository.findAllById(ids);
    }


}
