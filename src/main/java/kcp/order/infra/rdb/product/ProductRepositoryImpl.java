package kcp.order.infra.rdb.product;

import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kcp.order.domain.product.dto.ProductDetail;
import kcp.order.domain.product.dto.ProductSearchCmd;
import kcp.order.domain.product.entity.CategoryJpaEntity;
import kcp.order.domain.product.entity.ProductJpaEntity;
import kcp.order.domain.product.repository.ProductRepository;
import kcp.order.infra.rdb.product.jpa.ProductJpaRepository;
import kcp.order.infra.rdb.product.predict.ProductSearchPredict;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.list;
import static kcp.order.domain.product.entity.QCategoryJpaEntity.categoryJpaEntity;
import static kcp.order.domain.product.entity.QProductCategoryJpaEntity.productCategoryJpaEntity;
import static kcp.order.domain.product.entity.QProductJpaEntity.productJpaEntity;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {
    private final ProductJpaRepository productJpaRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<ProductJpaEntity> findByIdWithLock(Long productId) {
        return productJpaRepository.findByIdWithPessimisticLock(productId);
    }

    @Override
    public List<ProductJpaEntity> findByIdsWithLock(List<Long> productId) {
        return productJpaRepository.findByIdsWithPessimisticLock(productId);
    }

    @Override
    public Optional<ProductJpaEntity> findById(Long id) {
        return productJpaRepository.findById(id);
    }

    @Override
    public ProductJpaEntity save(ProductJpaEntity product) {
        return productJpaRepository.save(product);
    }

    @Override
    public Page<ProductDetail> findSliceOrderByCreatedAt(ProductSearchCmd command, Pageable page) {

        // 1) Product Entity 조회 (페이징 적용)
        // 컬렉션(OneToMany) 페치 조인 없이 본체만 가져옵니다.
        List<ProductJpaEntity> products = queryFactory
            .selectFrom(productJpaEntity)
            .where(ProductSearchPredict.from(command))
            .orderBy(productJpaEntity.createdAt.desc(), productJpaEntity.id.desc())
            .offset(page.getOffset())
            .limit(page.getPageSize())
            .fetch();

        // 2) Entity -> DTO 변환 (자동 최적화)
        List<ProductDetail> content = products.stream()
            .map(p -> new ProductDetail(
                // 여기서 p.getProductCategories()를 호출할 때 Batch Fetch 발동 (IN 쿼리)
                p.getProductCategories().stream()
                    .map(pc -> {
                        // 여기서 pc.getCategory()를 호출할 때도 Batch Fetch 발동 가능
                        // (이미 영속성 컨텍스트에 없다면 카테고리 ID들을 모아서 IN 쿼리 실행)
                        CategoryJpaEntity category = pc.getCategory();
                        return new ProductDetail.Category(
                            category.getId(),
                            category.getName(),
                            category.getCreatedAt()
                        );
                    })
                    .collect(Collectors.toList()),
                new ProductDetail.Product(
                    p.getId(), p.getName(), p.getPrice(), p.getStockQuantity(), p.getCreatedAt()
                )
            ))
            .collect(Collectors.toList());

        // 3) Count 쿼리
        JPAQuery<Long> countQuery = queryFactory
            .select(productJpaEntity.count())
            .from(productJpaEntity)
            .where(ProductSearchPredict.from(command));

        return PageableExecutionUtils.getPage(content, page, countQuery::fetchOne);
    }

    @Override
    public Long count(ProductSearchCmd command) {
        return queryFactory
            .select(productJpaEntity.count())
            .from(productJpaEntity)
            .where(ProductSearchPredict.from(command)) // byCategoryId는 EXISTS 권장 (아래 참고)
            .fetchOne();
    }
}
