package kcp.product.service.entity;

import jakarta.persistence.*;
import kcp.common.exception.BusinessException;
import kcp.common.exception.ErrorCode;
import kcp.common.exception.OutOfStockException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product")
@EntityListeners(AuditingEntityListener.class)
public class ProductJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Setter
    @Column(nullable = false, name = "name")
    private String name;

    @Setter
    @Column(nullable = false, name = "price")
    private BigDecimal price;

    @Setter
    @Column(nullable = false, name = "stock_quantity")
    private int stockQuantity;

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "modified_at")
    @LastModifiedDate
    private LocalDateTime modifiedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductCategoryJpaEntity> productCategories = new ArrayList<>();

    public static ProductJpaEntity createProduct(String name, BigDecimal price, int stockQuantity, List<CategoryJpaEntity> categories) {
        ProductJpaEntity product = new ProductJpaEntity();
        product.name = name;
        product.price = price;
        product.stockQuantity = stockQuantity;

        if (categories != null && !categories.isEmpty()) {
            for (CategoryJpaEntity category : categories) {
                product.addCategory(category);
            }
        } else {
            throw new BusinessException(ErrorCode.CATEGORY_REQUIRED, "상품 카테고리는 필수 입니다.");
        }

        return product;
    }

    public void addCategory(CategoryJpaEntity category) {
        ProductCategoryJpaEntity productCategory = ProductCategoryJpaEntity.createProductCategory(
            this, category
        );
        this.productCategories.add(productCategory);
    }

    // 비즈니스 로직: 재고 차감
    public void decreaseStock(int quantity) {
        int restStock = this.stockQuantity - quantity;
        if (restStock < 0) {
            throw new OutOfStockException(quantity, this.stockQuantity);
        }
        this.stockQuantity = restStock;
    }

    // 비즈니스 로직: 재고 복구
    public void increaseStock(int quantity) {
        this.stockQuantity += quantity;
    }

    public void updateProductNotNull(String name, BigDecimal price, Integer stockQuantity, List<CategoryJpaEntity> categories) {
        if(name != null) this.name = name;
        if(price != null) this.price = price;
        if(stockQuantity != null) this.stockQuantity = stockQuantity;
        if(categories != null) {
            if(this.productCategories == null) this.productCategories = new ArrayList<>();
            else this.productCategories.clear();
            categories.forEach(this::addCategory);
        }
    }
}