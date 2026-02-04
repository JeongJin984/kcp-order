package kcp.order.infra.rdb.product;

import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kcp.order.domain.product.dto.ProductDetail;
import kcp.order.domain.product.dto.ProductSearchCmd;
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
        // 1단계: 상품 기본 정보 조회 (페이징)
        List<ProductRow> productRows = queryFactory
            .select(Projections.constructor(ProductRow.class,
                productJpaEntity.id,
                productJpaEntity.name,
                productJpaEntity.price,
                productJpaEntity.stockQuantity,
                productJpaEntity.createdAt
            ))
            .from(productJpaEntity)
            .where(ProductSearchPredict.from(command))
            .orderBy(productJpaEntity.createdAt.desc(), productJpaEntity.id.desc())
            .offset(page.getOffset())
            .limit(page.getPageSize())
            .fetch();

        if (productRows.isEmpty()) return Page.empty();

        List<Long> productIds = productRows.stream().map(ProductRow::id).toList();

        // 2단계: 카테고리 정보 조회 (transform 대신 일반 fetch 사용)
        // NoSuchMethodError를 피하기 위해 ResultTransformer를 타지 않습니다.
        List<CategoryRow> categoryRows = queryFactory
            .select(Projections.constructor(CategoryRow.class,
                categoryJpaEntity.id,
                categoryJpaEntity.name,
                categoryJpaEntity.createdAt,
                productCategoryJpaEntity.product.id // 그룹핑을 위한 상품 ID 포함
            ))
            .from(productCategoryJpaEntity)
            .join(productCategoryJpaEntity.category, categoryJpaEntity)
            .where(productCategoryJpaEntity.product.id.in(productIds))
            .fetch();

        // 3단계: 자바 메모리에서 그룹핑 수행
        Map<Long, List<ProductDetail.Category>> categoriesByProductId = categoryRows.stream()
            .collect(Collectors.groupingBy(
                CategoryRow::productId,
                Collectors.mapping(r -> new ProductDetail.Category(
                    r.id(), r.name(), r.createdAt()
                ), Collectors.toList())
            ));

        // 4단계: 최종 DTO 조립
        List<ProductDetail> products = productRows.stream()
            .map(row -> new ProductDetail(
                categoriesByProductId.getOrDefault(row.id(), List.of()),
                new ProductDetail.Product(
                    row.id(), row.name(), row.price(), row.stockQuantity(), row.createdAt()
                )
            ))
            .toList();

        // 5단계: Count 쿼리 최적화
        JPAQuery<Long> countQuery = queryFactory
            .select(productJpaEntity.count())
            .from(productJpaEntity)
            .where(ProductSearchPredict.from(command));

        return PageableExecutionUtils.getPage(products, page, countQuery::fetchOne);
    }

    @Override
    public Long count(ProductSearchCmd command) {
        return queryFactory
            .select(productJpaEntity.count())
            .from(productJpaEntity)
            .where(ProductSearchPredict.from(command)) // byCategoryId는 EXISTS 권장 (아래 참고)
            .fetchOne();
    }

    /**
     * Book 1차 조회용 row
     */
    public record ProductRow(
        Long id,
        String name,
        BigDecimal price,
        int stockQuantity,
        LocalDateTime createdAt
    ) {}

    /**
     * Category 2차 조회용 row
     */
    public record CategoryRow(
        Long id,
        String name,
        LocalDateTime createdAt,
        Long productId
    ) {}
}
