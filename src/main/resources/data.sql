-- 1. 카테고리 생성
INSERT INTO category (name) VALUES ('전자기기'); -- ID: 1
INSERT INTO category (name) VALUES ('생활가전'); -- ID: 2

-- 2. 상품 생성 (100건)
-- 전자기기 카테고리 상품 (50건)
INSERT INTO product (name, price, stock_quantity, created_at)
SELECT
    '전자기기 상품 ' || x,
    10000 + (x * 1000),
    50,
    DATEADD('SECOND', x, CURRENT_TIMESTAMP()) -- 등록일자 순서 부여 (커서 페이징 테스트용)
FROM SYSTEM_RANGE(1, 50);

-- 생활가전 카테고리 상품 (50건)
INSERT INTO product (name, price, stock_quantity, created_at)
SELECT
    '생활가전 상품 ' || x,
    5000 + (x * 500),
    30,
    DATEADD('SECOND', x + 50, CURRENT_TIMESTAMP())
FROM SYSTEM_RANGE(1, 50);

-- 3. 상품-카테고리 매핑 (N:M 구조)
-- 1~50번 상품은 전자기기(1)에 매핑
INSERT INTO product_category (product_id, category_id)
SELECT id, 1 FROM product WHERE id BETWEEN 1 AND 50;

-- 51~100번 상품은 생활가전(2)에 매핑
INSERT INTO product_category (product_id, category_id)
SELECT id, 2 FROM product WHERE id BETWEEN 51 AND 100;

-- 4. 샘플 주문 생성 (상태별 테스트용)
-- 대기 중인 주문
INSERT INTO orders (status, order_date) VALUES ('WAIT', CURRENT_TIMESTAMP());
INSERT INTO order_item (order_id, product_id, order_price, count) VALUES (1, 1, 11000, 2);

-- 완료된 주문
INSERT INTO orders (status, order_date) VALUES ('COMPLETED', DATEADD('HOUR', -1, CURRENT_TIMESTAMP()));
INSERT INTO order_item (order_id, product_id, order_price, count) VALUES (2, 2, 12000, 1);

-- 취소된 주문
INSERT INTO orders (status, order_date) VALUES ('CANCELED', DATEADD('DAY', -1, CURRENT_TIMESTAMP()));
INSERT INTO order_item (order_id, product_id, order_price, count) VALUES (3, 51, 5500, 1);