# Commerce

Spring Boot 기반 커머스 주문 시스템 백엔드입니다.  
회원 인증부터 장바구니, 주문, 동시성 제어까지 실무 수준의 기능을 단계적으로 구현했습니다.

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 4.0.6 |
| ORM | Spring Data JPA / Hibernate 7 |
| DB | MySQL 8 |
| Security | Spring Security 7 + JWT (jjwt 0.12.6) |
| Test | JUnit 5 + MockMvc |
| Build | Gradle |

## 주요 기능

### 회원 인증
- 회원가입 / 로그인
- JWT 기반 Stateless 인증 (액세스 토큰 30분 + 리프레시 토큰 7일)
- 리프레시 토큰으로 액세스 토큰 재발급 (로테이팅)
- 역할 기반 접근 제어 (USER / ADMIN)
- 비밀번호 변경

### 상품 / 카테고리 관리
- 상품 목록 조회 (페이지네이션)
- 키워드 검색, 카테고리·가격 범위 필터링
- ADMIN 전용: 카테고리 생성·수정·삭제, 상품 등록·수정·삭제(소프트 삭제)

### 장바구니
- 상품 담기 / 수량 수정 / 개별 삭제

### 주문
- 장바구니 기반 주문 생성
- 주문 상태 전이: `PENDING → PAID → SHIPPED → DELIVERED / CANCELLED`
- 결제, 취소 (취소 시 재고 자동 복구)
- ADMIN 전용: 주문 상태 직접 변경 (배송 처리 등)

### 동시성 제어
- 재고 차감 시 비관적 락(`SELECT ... FOR UPDATE`) 적용
- JPA L1 캐시 문제를 `EntityManager.refresh(entity, PESSIMISTIC_WRITE)`로 해결

## 프로젝트 구조

```
src/main/java/com/hjhan/commerce/
├── domain/
│   ├── member/     # 회원, 인증
│   ├── category/   # 카테고리
│   ├── product/    # 상품, 재고
│   ├── cart/       # 장바구니
│   └── order/      # 주문
└── global/
    ├── config/     # SecurityConfig
    ├── entity/     # BaseTimeEntity
    ├── exception/  # GlobalExceptionHandler, ErrorCode
    ├── response/   # ApiResponse
    └── security/   # JwtProvider, JwtAuthenticationFilter
```

## API 명세

모든 응답은 아래 형식을 따릅니다.

```json
{
  "success": true,
  "message": "...",
  "data": { ... }
}
```

### 인증 / 회원

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/auth/signup` | 불필요 | 회원가입 |
| POST | `/api/auth/login` | 불필요 | 로그인 → `accessToken` + `refreshToken` 발급 |
| POST | `/api/auth/refresh` | 불필요 | 리프레시 토큰으로 토큰 재발급 |
| GET | `/api/members/me` | 필요 | 내 정보 조회 |
| PUT | `/api/members/password` | 필요 | 비밀번호 변경 |

**로그인 응답 예시**
```json
{
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ..."
  }
}
```

**토큰 재발급 요청 예시**
```json
{ "refreshToken": "eyJ..." }
```

### 카테고리

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/categories` | ADMIN | 카테고리 생성 |
| GET | `/api/categories` | 불필요 | 카테고리 목록 |
| GET | `/api/categories/{id}` | 불필요 | 카테고리 단건 조회 |
| PUT | `/api/categories/{id}` | ADMIN | 카테고리 수정 |
| DELETE | `/api/categories/{id}` | ADMIN | 카테고리 삭제 |

### 상품

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/products` | ADMIN | 상품 등록 |
| GET | `/api/products` | 불필요 | 상품 목록 (검색 / 필터 / 페이지네이션) |
| GET | `/api/products/{id}` | 불필요 | 상품 단건 조회 |
| PUT | `/api/products/{id}` | ADMIN | 상품 수정 |
| DELETE | `/api/products/{id}` | ADMIN | 상품 삭제 (소프트) |

**검색 / 필터 쿼리 파라미터**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `keyword` | String | 상품명 검색 (부분 일치) |
| `categoryId` | Long | 카테고리 ID 필터 |
| `minPrice` | BigDecimal | 최소 가격 |
| `maxPrice` | BigDecimal | 최대 가격 |
| `page` | int | 페이지 번호 (0부터, 기본값 0) |
| `size` | int | 페이지 크기 (기본값 20) |

```
GET /api/products?keyword=나이키&categoryId=5&minPrice=10000&maxPrice=200000
```

### 장바구니

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| GET | `/api/cart` | 필요 | 장바구니 조회 |
| POST | `/api/cart/items` | 필요 | 상품 담기 (이미 있으면 수량 합산) |
| PATCH | `/api/cart/items/{cartItemId}` | 필요 | 수량 수정 |
| DELETE | `/api/cart/items/{cartItemId}` | 필요 | 항목 삭제 |

**수량 수정 요청 예시**
```json
{ "quantity": 3 }
```

### 주문

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/orders` | 필요 | 주문 생성 (장바구니 → 주문) |
| GET | `/api/orders` | 필요 | 내 주문 목록 |
| GET | `/api/orders/{orderId}` | 필요 | 주문 단건 조회 |
| POST | `/api/orders/{orderId}/pay` | 필요 | 결제 (`PENDING → PAID`) |
| POST | `/api/orders/{orderId}/cancel` | 필요 | 주문 취소 (재고 자동 복구) |
| PATCH | `/api/orders/{orderId}/status` | ADMIN | 주문 상태 변경 |

**주문 상태 변경 요청 예시 (ADMIN)**
```json
{ "status": "SHIPPED" }
```

**주문 상태 값**

| 값 | 설명 |
|----|------|
| `PENDING` | 주문 완료, 결제 대기 |
| `PAID` | 결제 완료 |
| `SHIPPED` | 배송 중 |
| `DELIVERED` | 배송 완료 |
| `CANCELLED` | 취소됨 |

## 실행 방법

**1. MySQL 데이터베이스 생성**

```sql
CREATE DATABASE commerce;
```

**2. `application.yml` 설정 확인**

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/commerce
    username: root
    password: root
```

**3. 애플리케이션 실행**

```bash
./gradlew bootRun
```

## 테스트 실행

통합 테스트는 별도의 `commerce_test` 데이터베이스를 사용합니다.

```sql
CREATE DATABASE commerce_test;
```

```bash
./gradlew test
```

### 테스트 목록

| 클래스 | 설명 |
|--------|------|
| `MemberIntegrationTest` | 회원가입, 로그인, 내 정보 조회 시나리오 |
| `OrderIntegrationTest` | 장바구니 담기, 주문 생성, 결제, 취소, 재고 검증 |
| `StockConcurrencyTest` | 재고 5개 상품에 10명 동시 주문 → 정확히 5명만 성공 검증 |
