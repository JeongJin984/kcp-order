-- 1. 카테고리 테이블
CREATE TABLE category (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          name VARCHAR(50) NOT NULL UNIQUE,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 2. 상품 테이블
CREATE TABLE product (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         name VARCHAR(255) NOT NULL, -- [Modified] 상품명은 길어질 수 있어 255로 확장 권장
                         price BIGINT NOT NULL CHECK (price >= 0),
                         stock_quantity INT NOT NULL CHECK (stock_quantity >= 0),
    -- [Modified] category_id 컬럼 및 FK 삭제 (product_category 테이블로 일원화)
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 3. 상품-카테고리 연결 테이블 (N:M)
CREATE TABLE product_category (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  product_id BIGINT NOT NULL,
                                  category_id BIGINT NOT NULL,
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  CONSTRAINT fk_pc_product FOREIGN KEY (product_id) REFERENCES product(id),
                                  CONSTRAINT fk_pc_category FOREIGN KEY (category_id) REFERENCES category(id)
);

-- 4. 주문 테이블
CREATE TABLE orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        status VARCHAR(20) NOT NULL,
                        order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT chk_order_status CHECK (status IN ('WAIT', 'ACCEPTED', 'COMPLETED', 'CANCELED'))
);

-- 5. 주문 상품 테이블
CREATE TABLE order_item (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            order_id BIGINT NOT NULL,
                            product_id BIGINT NOT NULL,
                            order_price BIGINT NOT NULL,
                            count INT NOT NULL CHECK (count > 0),
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id),
                            CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product(id),
    -- [Modified] 한 주문 내에서 동일 상품 중복 등록 방지 (데이터 정합성)
                            CONSTRAINT uk_order_item_order_product UNIQUE (order_id, product_id)
);

-- [INDEX 전략]

-- 1. Product: 전체 상품 최신순 조회용
CREATE INDEX idx_product_created_id ON product(created_at DESC, id DESC);

-- 2. ProductCategory:
--    A. 특정 카테고리의 상품 목록 조회 (JOIN 후 정렬은 쿼리 튜닝 필요, 우선 조인 성능 확보)
--    B. 중복 매핑 방지 (이미 PK가 있지만 논리적 유니크 필요)
CREATE UNIQUE INDEX uk_product_category_pc ON product_category(product_id, category_id);
CREATE INDEX idx_pc_category_product ON product_category(category_id, product_id);

-- 3. Orders: '내 주문 목록' or '관리자 주문 관리' (상태별 + 날짜순)
--    A. 상태 필터 없음 + 날짜 정렬 (status == null 대응)
CREATE INDEX idx_orders_status_date_id ON orders(status, order_date DESC, id DESC);
CREATE INDEX idx_orders_date_id ON orders(order_date DESC, id DESC);

-- 4. OrderItem: 조인 및 락 에스컬레이션 방지용 (필수)
CREATE INDEX idx_order_item_order_id ON order_item(order_id);
CREATE INDEX idx_order_item_product_id ON order_item(product_id);;