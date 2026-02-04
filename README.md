지금까지 우리가 함께 설계하고 고민했던 기술적 의사결정, 트러블슈팅, 그리고 아키텍처 철학을 담은 고품질 README.md 초안입니다.

과제 제출용이나 포트폴리오용으로 사용할 수 있도록 **"어떤 문제를 만났고, 어떻게 해결했는지"**를 강조하여 작성했습니다.
📦 Order & Product Management System

Spring Boot 3.x와 JPA를 기반으로 구축한 주문 및 상품 관리 API 서버입니다. 대규모 트래픽을 고려한 인덱스 설계, Hibernate 6 환경에서의 QueryDSL 호환성 문제 해결, 그리고 견고한 예외 처리 전략에 중점을 두었습니다.
🛠 Tech Stack

    Java: 17

    Framework: Spring Boot 3.x

    ORM: Spring Data JPA, QueryDSL 5.0.0 (Jakarta)

    Database: H2 (Runtime), MySQL (Schema Compatible)

    Build Tool: Gradle

🏗 Key Architecture & Decisions
1. Hibernate 6 & QueryDSL 5.0 호환성 문제 해결 (Critical)

문제 상황: Spring Boot 3.x (Hibernate 6) 환경에서 QueryDSL 5.0의 transform() 및 groupBy() 사용 시 java.lang.NoSuchMethodError: org.hibernate.ScrollableResults.get(int) 런타임 에러가 발생하는 이슈를 확인했습니다. 이는 Hibernate 6에서 변경된 ScrollableResults API를 QueryDSL이 아직 반영하지 못해 발생하는 라이브러리 버그입니다.

해결 전략: 라이브러리 내부 버그에 의존하는 transform() 메서드 사용을 중단하고, Application Level Grouping 전략을 채택했습니다.

    fetch()를 통해 Flat한 데이터를 조회합니다.

    Java Stream API(Collectors.groupingBy)를 사용하여 메모리 내에서 객체 그래프(1:N 관계)를 재조립했습니다.

    이를 통해 런타임 안정성을 확보하고, 복잡한 ResultTransformer 로직을 제거하여 디버깅 용이성을 높였습니다.

2. "Soft Assertion" 예외 처리 전략

운영 환경의 가용성(Availability)과 개발 단계의 엄격함(Strictness)을 동시에 만족시키기 위해 하이브리드 예외 처리 전략을 수립했습니다.

    Developer Mistake (개발자 실수):

        BusinessException 생성 시 정의된 매핑 규칙(Map)을 위반할 경우, 시스템을 중단시키지 않고 ERROR 레벨 로그와 StackTrace를 남겨 개발자가 즉시 인지하도록 유도합니다.

        운영 환경에서는 ClassCastException 등의 2차 피해를 막기 위해 GlobalExceptionHandler에서 타입 안전성을 체크합니다.

    Client Response (사용자 응답):

        내부 로직이 어떻게 동작하든, 클라이언트에게는 항상 통일된 포맷(ErrorResponse)의 400/404/500 응답을 보장합니다.

3. 정교한 인덱스(Index) 설계

단순한 조회를 넘어, 페이징과 정렬 성능을 최적화하기 위해 복합 인덱스를 설계했습니다.

    상품 목록: (created_at DESC, id DESC) → 최신순 페이징 시 File Sort 제거

    카테고리 필터: (category_id, created_at DESC, id DESC) → 커버링 인덱스 고려

    주문 목록: (status, order_date DESC, id DESC) → 상태별 조회 최적화

4. 입력값 검증 및 Enum 매핑 처리

   Nested Validation: List<DTO> 내부 객체까지 검증하기 위해 @Valid와 @NotNull, @Min 등을 계층적으로 적용했습니다.

   Enum Handling:

        JSON Body: HttpMessageNotReadableException을 핸들링하여 JSON 파싱 실패 시에도 친절한 에러 메시지("허용된 값: [WAIT, ACCEPTED...]")를 반환합니다.

        URL Param: MethodArgumentTypeMismatchException을 핸들링하여 쿼리 파라미터 오타를 처리합니다.

📝 API Response Format

성공적인 응답 외에, 에러 발생 시 일관된 JSON 구조를 반환합니다.
Error Response Example
JSON

{
"code": "INVALID_INPUT_VALUE",
"message": "상품명은 필수이며 공백일 수 없습니다."
}

    code: 서버에서 정의한 에러 식별 코드 (예: STOCK_SHORTAGE, INVALID_INPUT_VALUE)

    message: 클라이언트에게 노출 가능한 상세 메시지

📂 Project Structure

src/main/java/com/kcp/order
├── common
│   ├── exception      # GlobalExceptionHandler, BusinessException, ErrorCode
│   └── response       # ErrorResponse, CommonResponse
├── domain
│   ├── order          # Order, OrderItem, Repository, Service
│   └── product        # Product, Category, Repository, Service
└── api                # Controllers, DTOs (Record pattern)

🚀 How to Run
Bash

# Build
./gradlew clean build

# Run
java -jar build/libs/order-system.jar

🧪 Test Strategy

    Architecture Test: 예외 클래스 생성 규칙 등 설계 원칙 검증

    Controller Test: 잘못된 Enum 값, 필수 값 누락 등 예외 핸들링 시나리오 검증

    Service Test: 비즈니스 로직 및 재고 차감 동시성 검증 (TODO)