# Order & Product Management System

## 📦 Order & Product Management System

Spring Boot 3.5.10와 JPA를 기반으로 구축한 주문 및 상품 관리 API 서버입니다. 대규모 트래픽을 고려한 인덱스 설계, 비관적 락을 활용한 동시성 제어, 그리고 견고한 예외 처리 전략에 중점을 두었습니다.

### 실행 방법

1. intellij를 활용하여 KcpOrderApplication을 실행합니다.

혹은,

1. gradle build
2. java -jar build/libs/kcp-order-0.0.1-SNAPSHOT.jar


## 🛠 Tech Stack

```
Java: 21

Framework: Spring Boot 3.5.10

ORM: Spring Data JPA, QueryDSL 5.0.0 (Jakarta)

Database: H2 (Runtime, in-memory)

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
kcp
├─ common
│  ├─ exception
│  └─ response
├─ config
├─ order
│  ├─ controller
│  ├─ repository
│  │  ├─ jpa
│  │  └─ predicate
│  └─ service
│     ├─ dto
│     ├─ entity
│     └─ repository
└─ product 
   ├─ controller
   ... (order와 같음)
```


## 🏗 Key Architecture & Decisions

### 1. JPA 1:N 관계 페이징 성능 최적화 (Collection Paging)

**문제 배경:** 1:N 관계의 엔티티를 JOIN FETCH로 조회하면서 Pageable(limit, offset)을 적용하면 심각한 문제가 발생합니다.
- 현상: Distinct + Fetch Join는 데이터베이스 쿼리에 LIMIT가 적용되지 않고, 전체 데이터를 애플리케이션 메모리로 로딩한 뒤 메모리에서 페이징을 수행합니다.
- 위험: 데이터가 많아질 경우 **OOM(Out Of Memory)으로** 서버가 다운될 수 있습니다.

**해결 전략:** ToOne만 페치 조인 + 컬렉션은 지연 로딩 (Batch Size 자동 최적화)
- 엔티티 조회 후 DTO 변환 시점(Getter 호출 시)에 Batch Size 설정에 의해 최적화된 쿼리가 나갑니다.

**분석:** default_batch_fetch_size: 100 설정 시, 페이지 사이즈가 10개라면 다음과 같이 쿼리가 실행
1. Main Query: SELECT * FROM orders LIMIT 10 (부모 조회)
2. Lazy Loading: 코드가 루프를 돌며 order.getOrderItems()에 접근하는 순간,
3. Batch Query: Hibernate가 메모리에 있는 10개의 Order ID를 모아서 단 1번의 IN 쿼리를 전송합니다.
   - SELECT * FROM order_items WHERE order_id IN (?, ?, ..., ?)

### 2. "Soft Assertion" 예외 처리 전략

운영 환경의 가용성(Availability)과 개발 단계의 엄격함(Strictness)을 동시에 만족시키기 위해 하이브리드 예외 처리 전략을 수립했습니다.

- **Developer Mistake (개발자 실수):** BusinessException 생성 시 Critical한 Exception(재고 이슈, 상태 전이)에 대해서는 정형화된 log를 기록하기 위해 상속 활용 
  - 정의된 매핑 규칙(Map)을 위반할 경우, ERROR 레벨 로그와 StackTrace를 남겨 개발자가 즉시 인지하도록 유도합니다. 
- **Client Response (사용자 응답):** 내부 로직이 어떻게 동작하든, 클라이언트에게는 항상 통일된 포맷(ErrorResponse)의 400/404/500 응답을 보장합니다.

예로 BusinessException을 상속한 OutOfStockException, InvalidOrderStatusException은 Exception 발생 시 자동으로 log를 작성하기 위해 상속을 하였으며 Exception 생성, 로그 작성에 필요한 파라미터를 생성자에 넣어 가용성과 엄격함을 만족하였습니다. 

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
  - 의도: 비관적 락(Pessimistic Lock) 사용 시, FK 컬럼에 인덱스가 없어 테이블 전체에 락이 걸리는 치명적인 문제를 방지했습니다.(H2에서는 Join, where in 에 대한 비관적 락이 정상적으로 작동하지 않아 단일 조회로 락을 획득하여 필요 없지만 운영환경(MySQL, PostgreSQL...)에서 필요)

- 상품명 검색이 빈번하다면 CREATE INDEX idx_product_name_created ON product(name, created_at DESC) 같은 검색 전용 인덱스가 추가로 필요

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

## 🛡️ 추가 문제 분석 및 개선 아이디어

### 1. 대용량 트래픽 환경에서의 주문 중복 방지 전략 (Redis Idempotency)

주문 요청이 급증하는 상황에서 발생할 수 있는 데이터 정합성 문제를 분석하고, Redis를 활용한 멱등성(Idempotency) 시스템을 구축하여 안정성을 확보할 수 있습니다.

**1. 문제 분석**

대규모 트래픽 상황이나 네트워크 불안정 환경에서는 다음과 같은 원인으로 동일한 주문 요청이 서버에 중복 도달할 수 있습니다.

- **Network Timeouts:** 클라이언트가 요청을 보냈으나 서버 응답(ACK)을 받지 못해 재시도(Retry) 로직을 수행하는 경우.
- **User Behavior:** 사용자가 결제 버튼을 실수로 연타(Double Click) 하는 경우.
- **Distributed System:** MSA 환경에서 메시지 큐의 At-least-once 특성으로 인해 메시지가 중복 전달되는 경우.


🚨발생 가능한 위험:
- 중복 결제: 하나의 주문에 대해 돈이 두 번 빠져나감.
- 재고 오류: 실제 상품은 1개인데 재고가 2개 차감됨.
- 데이터 오염: 동일한 주문 데이터가 DB에 중복 적재됨.


**2. 개선방안** : Redis 기반 멱등성(Idempotency) 보장

동일한 요청(Request)이 여러 번 수행되더라도 결과가 달라지지 않는 성질인 멱등성을 보장하기 위해, 고속의 In-Memory DB인 Redis를 **멱등성 키 저장소(Idempotency Key Store)로** 활용할 수 있습니다.
 
1. Key 생성: 클라이언트는 주문 요청 시 고유한 Idempotency-Key(UUID 등)를 헤더에 포함하여 전송합니다.
   - 키는 계층 형태로 구성하여 조회 성능을 최적화합니다.
2. 중복 검사 (Atomic Operation): 서버는 요청을 받자마자 Redis의 SETNX (Set if Not Exists) 명령어를 사용하여 키 저장을 시도합니다.
   - SETNX key value (TTL 설정 포함)
3. 로직 수행:
   - 성공 (1): 최초 요청임이 확인되면 주문 로직 및 재고 차감을 수행합니다.
   - 실패 (0): 이미 처리 중이거나 완료된 요청이므로, 비즈니스 로직을 수행하지 않고 이전 결과를 반환하거나 **에러(409 Conflict)를** 응답합니다.

### 2. 대용량 트래픽 환경에서의 재고 데이터 전략 

비관적 락(Pessimistic Lock)은 데이터 정합성을 보장하는 가장 확실한 방법 중 하나이지만, 대규모 트래픽이 발생하는 서비스에서는 시스템 전반의 성능을 저하시키는 원인이 되기도 합니다.

**1. 문제 분석**

비관적 락은 데이터베이스의 SELECT ... FOR UPDATE 구문을 사용하여 레코드에 물리적인 잠금을 겁니다. 이 과정에서 다음과 같은 문제가 발생합니다.

1. 데이터베이스 커넥션 고갈 : 비관적 락은 트랜잭션이 시작되어 커밋되거나 롤백될 때까지 락을 유지합니다.
   - 트랜잭션 내에 외부 API 호출이나 무거운 로직이 포함될 경우, 해당 커넥션은 락을 쥔 채로 대기하게 됩니다.
   - 후속 요청들이 커넥션을 얻지 못하고 대기하면서 전체 시스템의 스루풋(Throughput)이 급격히 저하됩니다.
2. 데드락(Deadlock) 위험 증가 : 여러 엔티티를 동시에 수정하는 복잡한 비즈니스 로직에서 락 획득 순서가 꼬일 경우 데드락이 발생합니다.
   - 트랜잭션 A가 상품 1의 락을 쥐고 상품 2를 기다릴 때, 트랜잭션 B가 상품 2의 락을 쥐고 상품 1을 기다리는 상황.
3. 확장성(Scalability) 저하 
   - 데이터베이스 서버 리소스(CPU, Memory)를 사용하여 잠금을 관리하므로, 사용자가 늘어날수록 DB 서버에 가해지는 부하가 기하급수적으로 증가합니다. 이는 DB가 시스템의 단일 병목 지점(SPOF)이 되게 만듭니다.

**2. 개선방안** : Redisson 라이브러리를 사용하여 DB가 아닌 Redis에서 락을 관리합니다.

1. Pub/Sub 기반의 대기 큐 (Spin Lock 방지) : 불필요한 네트워크 트래픽이 획기적으로 줄어듭니다.
   - 락이 해제되면 Redis가 "락이 풀렸어!"라고 메시지를 발행(Publish)합니다.
   - 대기 중인 다른 프로세스들은 그 메시지를 받았을 때만 락 획득을 시도(Subscribe)합니다.
2. Watchdog (락 유효 시간 자동 연장) : 로직 수행 도중 락이 풀려 데이터 정합성이 깨지는 현상을 방지합니다.
   - Watchdog이라 불리는 내부 타이머가 있어, 트랜잭션이 아직 끝나지 않았다면 락의 유효 시간을 주기적으로 연장해 줍니다.

**3. 대략적 흐름**

```java
@Transactional
public OrderResponse createOrder(OrderRequest request, String idempotencyKey) {
    // 1. 멱등성 체크 (입구 컷)
    String idempotencyLockKey = "idempotency:order:" + idempotencyKey;
    if (!redisTemplate.opsForValue().setIfAbsent(idempotencyLockKey, "PROCESSING", Duration.ofMinutes(30))) {
        throw new BusinessException(ErrorCode.DUPLICATE_REQUEST);
    }

    // 2. 상품 ID 리스트 정렬 (데드락 방지의 핵심)
    // 요청에 들어온 상품 ID들을 항상 일정한 순서(오름차순)로 정렬하여 락을 획득해야 합니다.
    List<Long> productIds = request.getItems().stream()
        .map(OrderItemRequest::getProductId)
        .distinct()
        .sorted()
        .toList();

    // 3. MultiLock 생성
    RLock[] locks = productIds.stream()
        .map(id -> redissonClient.getLock("lock:product:" + id))
        .toArray(Object[]::new);

    RLock multiLock = redissonClient.getMultiLock(locks);

    try {
        // 모든 상품에 대한 락을 동시에 획득 시도
        if (multiLock.tryLock(10, 5, TimeUnit.SECONDS)) {
            try {
                // 4. 실제 비즈니스 로직 (여러 상품의 재고 차감 및 주문 생성)
                return processMultiOrderLogic(request);
            } finally {
                multiLock.unlock();
            }
        } else {
            throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
    } catch (Exception e) {
        // 실패 시 멱등성 키 삭제 (재시도 허용)
        redisTemplate.delete(idempotencyLockKey);
        throw e;
    }
}
```
 
**4. 장기 대책 : Write Back 전략에 대해서**

데이터를 변경할 때 캐시(Cache)에만 먼저 반영하고, 실제 데이터베이스(DB)에는 나중에 배치(Batch)로 모아서 업데이트하는 방식입니다. 

1. 요청 수신: 사용자가 데이터 변경(예: 재고 차감)을 요청합니다.
2. 캐시 업데이트: 캐시(주로 Redis)의 데이터를 즉시 수정합니다. 이때 수정된 데이터는 'Dirty(변경됨)' 상태로 마킹됩니다.
3. 즉시 응답: DB 기록을 기다리지 않고 사용자에게 바로 완료 응답을 보냅니다. (매우 빠른 응답 속도)
4. 비동기 지연 기록: 일정 시간 간격이나 데이터가 일정량 쌓였을 때, 백그라운드 프로세스가 캐시의 변경 사항을 모아 DB에 한 번에 UPDATE 쿼리를 날립니다.

🚨 치명적인 단점과 위험 요소

1. 데이터 유실 위험 : DB에 반영되기 전 캐시 서버가 다운되거나 전원이 꺼지면, 메모리에만 있던 데이터는 영구적으로 사라집니다. 재고 시스템이라면 "물건은 팔렸는데 기록이 없는" 최악의 상황이 발생합니다.
2. 정합성 관리의 복잡도 : 장애 발생 시 어디까지 DB에 반영되었는지 추적하기 어렵고, 복구 로직이 매우 복잡해집니다.

🛡️ 보완 로직

1. 메시지 큐(Kafka/RabbitMQ) 도입 : 캐시를 업데이트함과 동시에 변경 이력을 메시지 큐에 넣습니다.
   - 서버가 다운되어도 큐에 데이터가 남아있으므로 유실을 방지할 수 있습니다.
2. Write-Through와 혼합
   - 아주 중요한 데이터(결제 금액 등)는 Write-Through(즉시 DB 기록)를 사용