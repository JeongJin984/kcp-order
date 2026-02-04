# Order & Product Management System

지금까지 우리가 함께 설계하고 고민했던 기술적 의사결정, 트러블슈팅, 그리고 아키텍처 철학을 담은 고품질 README.md 초안입니다.

과제 제출용이나 포트폴리오용으로 사용할 수 있도록 "**어떤 문제를 만났고, 어떻게 해결했는지**"를 강조하여 작성했습니다.

## 📦 Order & Product Management System

Spring Boot 3.x와 JPA를 기반으로 구축한 주문 및 상품 관리 API 서버입니다. 대규모 트래픽을 고려한 인덱스 설계, Hibernate 6 환경에서의 QueryDSL 호환성 문제 해결, 그리고 견고한 예외 처리 전략에 중점을 두었습니다.

## 🛠 Tech Stack

```
Java: 21

Framework: Spring Boot 3.x

ORM: Spring Data JPA, QueryDSL 5.0.0 (Jakarta)

Database: H2 (Runtime)

Build Tool: Gradle
```

## 📝 API Response Format

성공적인 응답 외에, 에러 발생 시 일관된 JSON 구조를 반환합니다.
Error Response Example
JSON

{
"code": "INVALID_INPUT_VALUE",
"message": "상품명은 필수이며 공백일 수 없습니다."
}

```
code: 서버에서 정의한 에러 식별 코드 (예: STOCK_SHORTAGE, INVALID_INPUT_VALUE)

message: 클라이언트에게 노출 가능한 상세 메시지
```

## 📂 Project Structure

```
src/main/java/com/kcp/order
├── common
│   ├── exception      # GlobalExceptionHandler, BusinessException, ErrorCode
│   └── response       # ErrorResponse, CommonResponse
├── domain
│   ├── order          # Order, OrderItem, Repository, Service
│   └── product        # Product, Category, Repository, Service
└── api                # Controllers, DTOs (Record pattern)
```


## 🏗 Key Architecture & Decisions

### 1. JPA 1:N 관계 페이징 성능 최적화 (Collection Paging)

**문제 배경:** 1:N 관계의 엔티티를 JOIN FETCH로 조회하면서 Pageable(limit, offset)을 적용하면 심각한 문제가 발생합니다.
- 현상: Distinct + Fetch Join는 데이터베이스 쿼리에 LIMIT가 적용되지 않고, 전체 데이터를 애플리케이션 메모리로 로딩한 뒤 메모리에서 페이징을 수행합니다.
- 위험: 데이터가 많아질 경우 **OOM(Out Of Memory)으로** 서버가 다운될 수 있습니다.

**해결 전략:** ToOne만 페치 조인 + 컬렉션은 지연 로딩 (Batch Size 자동 최적화)
- 엔티티 조회 후 DTO 변환 시점(Getter 호출 시)에 Batch Size 설정에 의해 최적화된 쿼리가 나갑니다.

**분석:** default_batch_fetch_size: 1000 설정 시, 페이지 사이즈가 10개라면 다음과 같이 쿼리가 실행
1. Main Query: SELECT * FROM orders LIMIT 10 (부모 조회)
2. Lazy Loading: 코드가 루프를 돌며 order.getOrderItems()에 접근하는 순간,
3. Batch Query: Hibernate가 메모리에 있는 10개의 Order ID를 모아서 단 1번의 IN 쿼리를 전송합니다.
   - SELECT * FROM order_items WHERE order_id IN (?, ?, ..., ?)

### 2. "Soft Assertion" 예외 처리 전략

운영 환경의 가용성(Availability)과 개발 단계의 엄격함(Strictness)을 동시에 만족시키기 위해 하이브리드 예외 처리 전략을 수립했습니다.

- **Developer Mistake (개발자 실수):** BusinessException 생성 시 Critical한 Exception(재고 이슈, 상태 전이)에 대해서는 정형화된 log를 기록하기 위해 상속 활용 
  - 정의된 매핑 규칙(Map)을 위반할 경우, ERROR 레벨 로그와 StackTrace를 남겨 개발자가 즉시 인지하도록 유도합니다. 
- **Client Response (사용자 응답):** 내부 로직이 어떻게 동작하든, 클라이언트에게는 항상 통일된 포맷(ErrorResponse)의 400/404/500 응답을 보장합니다.

### 3. 고성능 쿼리를 위한 인덱스(Index) 전략

단순한 조회를 넘어, 페이징과 정렬 성능을 최적화하기 위해 복합 인덱스를 설계했습니다.

- 상품 목록 (No-Offset Paging 최적화)
  - 인덱스: idx_product_created_id (created_at DESC, id DESC)
  - 의도: 전체 상품 조회 시 별도의 정렬 작업(File Sort) 없이 인덱스 스캔만으로 페이징을 처리하여 쿼리 비용을 최소화했습니다.

- 카테고리 필터 (N:M 관계 최적화)
  - 인덱스: idx_pc_category_product (category_id, product_id)
  - 의도: EXISTS 서브쿼리를 사용할 때, 연결 테이블(product_category)의 커버링 인덱스를 타게 하여 상품 테이블에 접근하지 않고도 필터링 여부를 판단하도록 최적화했습니다.

- 주문 목록 (동적 쿼리 대응)
  - 인덱스 A: idx_orders_status_date_id (status, order_date DESC, id DESC)
  - 인덱스 B: idx_orders_date_id (order_date DESC, id DESC)
  - 의도: 필터 조건(status) 유무에 따라 옵티마이저가 최적의 인덱스를 선택하도록 이원화했습니다. 상태 필터가 없을 때도 인덱스 B를 통해 Full Table Scan을 방지합니다.

- 동시성 제어 (Lock Escalation 방지)
  - 인덱스: idx_order_item_order_id, idx_order_item_product_id
  - 의도: 비관적 락(Pessimistic Lock) 사용 시, FK 컬럼에 인덱스가 없어 테이블 전체에 락이 걸리는 치명적인 문제를 방지했습니다.

### 4. 비관적 락(Pessimistic Lock)을 활용한 동시성 제어

주문 상태 변경(CANCEL, COMPLETED)과 같이 재고(Stock) 수량 변경이 수반되는 민감한 로직에는 **비관적 락**을 적용하여 데이터 정합성을 보장합니다.

- 동시성 이슈 방지: 여러 트랜잭션이 동시에 동일한 주문이나 상품의 상태를 변경하려고 할 때 발생하는 경쟁 조건(Race Condition)을 방지합니다.
- 원자성 보장: 재고 감소와 주문 상태 변경이 완벽하게 하나의 단위로 처리되도록 SELECT ... FOR UPDATE 쿼리를 사용하여 해당 로우를 물리적으로 잠금 처리합니다.

**주의사항**
1. 성능 영향: FOR UPDATE는 해당 로우에 접근하는 다른 트랜잭션을 대기시키므로, 트랜잭션 유지 시간을 최대한 짧게 가져가야 합니다. (외부 API 호출 등 시간이 오래 걸리는 작업은 락 구간 밖으로 빼야 합니다.)
2. 인덱스 활용: where 조건에 걸리는 컬럼(id)에 인덱스가 있어야 불필요한 테이블 풀스캔 및 광범위한 락을 방지할 수 있습니다.

### 5. 트랜잭션 설계를 통한 데이터 원자성, 정합성 보장

1. 원자성: @Transactional을 통해 주문 생성과 상세 아이템 저장이 하나의 단위로 수행되도록 하여, 부분 저장으로 인한 고아(Orphan) 데이터를 방지했습니다.
2. 정합성: 트랜잭션 내에서 엔티티를 조회 후 상태 변경 가능 여부를 검증하는 도메인 로직을 수행하여, 논리적으로 불가능한 상태 전이(예: 취소된 주문의 배송 처리)를 원천 차단했습니다.



