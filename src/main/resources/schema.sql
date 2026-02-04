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
                         name VARCHAR(100) NOT NULL,
                         price BIGINT NOT NULL CHECK (price >= 0),
                         stock_quantity INT NOT NULL CHECK (stock_quantity >= 0),
                         category_id BIGINT,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(id)
);

-- 연결 테이블 추가
CREATE TABLE product_category (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  product_id BIGINT NOT NULL,
                                  category_id BIGINT NOT NULL,
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  CONSTRAINT fk_pc_product FOREIGN KEY (product_id) REFERENCES product(id),
                                  CONSTRAINT fk_pc_category FOREIGN KEY (category_id) REFERENCES category(id)
);


-- 3. 주문 테이블 (order는 예약어이므로 orders 사용)
CREATE TABLE orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        status VARCHAR(20) NOT NULL, -- WAIT, ACCEPTED, COMPLETED, CANCELED
                        order_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT chk_order_status CHECK (status IN ('WAIT', 'ACCEPTED', 'COMPLETED', 'CANCELED'))
);

-- 4. 주문 상품 테이블 (N:M 해소 및 이력 보존)
CREATE TABLE order_item (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            order_id BIGINT NOT NULL,
                            product_id BIGINT NOT NULL,
                            order_price BIGINT NOT NULL, -- 주문 당시 가격 보존
                            count INT NOT NULL CHECK (count > 0),
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id),
                            CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product(id)
);

-- [Product] 최신순 페이징 및 카테고리 필터 최적화
CREATE INDEX idx_product_created_id ON product(created_at DESC, id DESC);
-- 만약 단일 카테고리 필터링 조회가 잦다면 아래 인덱스 추가
CREATE INDEX idx_product_category_created_id ON product(category_id, created_at DESC, id DESC);

-- [ProductCategory] 중복 방지 및 조인 성능 향상
CREATE UNIQUE INDEX uk_product_category ON product_category(product_id, category_id);
CREATE INDEX idx_pc_category_product ON product_category(category_id, product_id);

-- [Orders] 상태별 목록 조회 및 최신순 정렬
CREATE INDEX idx_orders_status_date_id ON orders(status, order_date DESC, id DESC);

-- [OrderItem] 조인 성능 향상
CREATE INDEX idx_order_item_order_id ON order_item(order_id);
CREATE INDEX idx_order_item_product_id ON order_item(product_id);