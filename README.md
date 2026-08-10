# Zenith Commerce CX (z-comm-cx)

배포: **https://z-comm-cx.onrender.com** (Render + Supabase, 자세한 내용은 [배포](#배포-프로덕션) 참고)

`ai-review-management`(AI 리뷰 매니지먼트)와 `ai-cs-auto-resolver`(AI CS 자동 해결 트랜잭션 에이전트)
두 조별과제를 하나의 커머스 고객경험(CX) 플랫폼으로 통합한 프로젝트. 상품을 등록하면 고객이 그
상품에 리뷰를 남기고, 주문하고, 주문 관련 CS를 AI와 채팅으로 처리할 수 있다. 두 원본 프로젝트는
읽기 전용으로만 참고했고 전혀 수정하지 않았다 — 앞으로 추가할 과제도 이 프로젝트 위에 얹는다.

## 아키텍처

### 상품(Product) — 통합의 핵심

두 원본 프로젝트가 각자 갖고 있던 `Product` 엔티티(외부 쇼핑몰 URL 스크래핑 결과)를 하나로
합쳤다. `ai-cs-auto-resolver` 쪽 `Product`는 원래 `ai-review-management`의 스크래퍼를 그대로
포팅한 것이라 필드가 동일했다 — 지금은 `product`/`product_image_url` 테이블 하나만 존재하고,
리뷰(`Review`)와 재고/주문(`Inventory`/`Order`)이 이 하나의 `Product`를 함께 참조한다.

- `Review`는 원래부터 `productCode`(String) 느슨한 키로 `Product`를 참조했다 (DB FK 없음).
  다른 바운디드 컨텍스트와 공존할 수 있도록 의도적으로 느슨하게 설계된 것이라 통합 후에도
  그대로 유지했다.
- `Inventory`/`OrderItem`은 `Product`를 실제 FK(`product_id`)로 참조한다.
- 상품 등록(`POST /api/products`) 시 최초 등록이면: (1) 외부 리뷰 자동 임포트
  (`ExternalReviewImportService`, zerogram/bylynn 지원), (2) 재고를 0으로 생성
  (`InventoryService#createForProduct`) — 두 가지를 모두 수행한다. 재등록(같은 productCode)
  시에는 상품 정보만 갱신하고 둘 다 다시 수행하지 않는다.
- 관리자 상품관리 화면(`/admin/products`)에서 등록·검색·재고 입고/품절을 한 화면에서 처리한다.
- 고객 상품상세 화면(`/products/:id`)에서 리뷰(AI 요약/베스트리뷰/목록)와 주문하기를 함께
  제공한다.

### 리뷰 분석 파이프라인 (`review` 패키지, ai-review-management 포팅)

- 리뷰 저장과 AI 분석을 분리 — 저장 트랜잭션 커밋 후 이벤트를 발행하고 `@Async` 리스너가 별도
  스레드 풀(`aiTaskExecutor`)에서 Gemini를 호출한다. 리뷰 상태: `PENDING_AI` → `ANALYZED`/`FAILED`.
- AI가 공개여부(`visible`)/분류(`classification`)/감성(`sentiment`)을 독립적으로 판단한다.
  관리자가 공개여부/분류를 수동으로 override할 수 있고, override 후에도 AI의 원래 판단은
  감사 추적용으로 보존된다.
- `FAILED` 리뷰는 관리자가 "재시도" 버튼으로 재분석을 트리거할 수 있다
  (`POST /api/reviews/{id}/reanalyze`, `FAILED` 상태에서만 가능).
- 베스트 리뷰는 상품별 주간 숏리스트(`BestReviewShortlistEntry`)로 선정하며, 매주 월요일
  자동 생성 + 관리자 수동 재생성을 지원한다.

### CS 자동 해결 에이전트 (`order`/`ticket`/`chat`/`guardrail`/`gemini` 패키지, ai-cs-auto-resolver 포팅)

- 고객의 자연어 CS 요청을 Gemini Function Calling으로 해석해 주문 조회/취소/배송지 변경/반품
  접수/상담원 이관 등 실제 트랜잭션을 처리한다 (`POST /api/cs/chat`).
- 하드 룰(Guardrail): 배송 시작 후 취소 요청, 반품 기한(배송완료 후 `guardrail.return-window-days`일)
  경과는 AI가 직접 처리하지 않고 즉시 CS 티켓으로 이관한다.
- 리뷰 쪽 Gemini 클라이언트(`review.ai.GeminiClient`, JSON 스키마 응답 모드)와 CS 쪽 Gemini
  클라이언트(`gemini.client.*`, Function Calling 모드)는 사용 패턴이 달라 통합하지 않고
  패키지를 분리해 각자 유지한다 (스프링 빈 이름 충돌 방지를 위해 리뷰 쪽은
  `@Component("reviewGeminiClient")`로 명시).

### 로그인 — 2단계

외부에서 접속 가능하게 배포했기 때문에, 로그인을 목적이 다른 두 단계로 나눴다.

1. **사이트 로그인 (`com.zcommcx.auth` 패키지)** — Google 계정으로 로그인해야 사이트 전체
   (운영자어드민 + 고객화면 FO)에 들어올 수 있다. "이 배포 인스턴스에 들어올 자격이 있는가"를
   검사하는 관문이다.
   - 프론트: Google Identity Services 표준 버튼(`GoogleSignInButton.vue`)에서 ID 토큰을 받아
     `POST /api/auth/google`로 보낸다.
   - 백엔드: `GoogleTokenVerifier`가 Google의 `tokeninfo` 엔드포인트로 토큰을 검증(서명/만료는
     Google이 검사, `aud`/`email_verified`만 추가 확인)하고, `allowed_google_account` 테이블에
     있는 이메일인지 확인한 뒤 `HttpSession`에 이메일을 저장한다. Spring Security는 도입하지
     않고 `SiteAuthFilter`(서블릿 `Filter`) 하나로 `/api/**` 전체를 막는다
     (`/api/auth/**`만 예외).
   - 허용 이메일 관리 UI는 없다 — `psql`로 직접 `allowed_google_account`에 넣는다 (아래 참고).
     `email` 컬럼에는 전체 이메일(`zenith@g1project.net`)이나 `@도메인` 형태(`@g1project.net`,
     해당 도메인 전체 허용)를 둘 다 넣을 수 있다 — 정확히 일치하는 이메일을 먼저 확인하고,
     없으면 `@도메인` 항목이 있는지 확인한다 (`SiteAuthService#isAllowed`).
   - 로그인 성공 시 프론트가 항상 `/admin/products`로 이동한다 (`App.vue#handleGoogleCredential`)
     — 이전에 어떤 화면을 보고 있었든 상관없이 사이트 로그인 직후 첫 화면은 관리자 상품관리다.
   - "사이트 로그아웃" 버튼을 누르면 사이트 세션뿐 아니라 FO 회원 로그인(`stores/session.ts`)도
     함께 로그아웃된다 (`App.vue#handleSiteLogout`) — 같은 브라우저를 다음 사람이 쓸 때 이전
     회원 세션이 남아있지 않도록.
2. **FO 회원 로그인 (`stores/session.ts`, 기존)** — 이름+전화번호만 입력하는 간단 로그인
   (`localStorage` 세션 — 실제 인증 없음). 사이트 로그인을 통과한 뒤, 리뷰 작성/주문/CS채팅 등
   고객화면 기능을 쓸 때 필요한 별도의 고객 신원이다. 리뷰 작성도 이 로그인이 필요하도록 확장했다
   (원래 `ai-review-management`는 회원ID를 자유 입력받았지만, 통합 후에는 로그인한 이름을
   자동으로 사용한다). 백엔드 인증은 없으므로 API 계약에는 영향 없음.

## 데이터베이스

새 데이터베이스로 시작했다 (기존 `reviewdb`/`csdb`는 건드리지 않음, 데이터 이전 없음).

| 테이블 | 출처 |
|---|---|
| `product`, `product_image_url` | review-management (공유, order/inventory가 FK로 참조) |
| `review`, `best_review_shortlist_entry` | review-management |
| `cs_inventory`, `cs_order`, `cs_order_item`, `cs_ticket` | cs-auto-resolver |
| `allowed_google_account` | 신규 (사이트 로그인 허용 이메일 목록) |

`spring.jpa.hibernate.ddl-auto: update`라 테이블은 애플리케이션 기동 시 자동 생성/갱신된다.

## API 키 설정

Gemini API 키는 git에 추적되지 않는 `src/main/resources/application-local.yml`에 넣는다.

```yaml
gemini:
  api-key: <your-gemini-api-key>
```

## Google 로그인 설정 (사이트 로그인)

1. https://console.cloud.google.com/ 에서 프로젝트 생성 (없으면)
2. "API 및 서비스 → OAuth 동의 화면" 설정 (외부/테스트 사용자로 시작 가능)
3. "API 및 서비스 → 사용자 인증 정보 → OAuth 클라이언트 ID 만들기" → 애플리케이션 유형
   **웹 애플리케이션**
4. **승인된 JavaScript 원본**에 `http://localhost:15173` 추가 (배포 도메인도 끝에 `/` 없이
   추가 — 예: `https://z-comm-cx.onrender.com`)
5. 생성된 클라이언트 ID(비밀키 아님, `...apps.googleusercontent.com`)를
   `application-local.yml`의 `google.client-id`에 넣는다:
   ```yaml
   google:
     client-id: <your-client-id>.apps.googleusercontent.com
   ```
6. 로그인을 허용할 이메일(또는 도메인 전체)을 DB에 직접 추가한다 (관리 UI 없음):
   ```bash
   # 특정 이메일만 허용
   psql -h localhost -U zcommcx_app -d zcommcx -c \
     "INSERT INTO allowed_google_account (email, note, created_at) VALUES ('you@gmail.com', '팀원', now());"

   # 도메인 전체 허용 (예: g1project.net 이메일이면 전부 통과)
   psql -h localhost -U zcommcx_app -d zcommcx -c \
     "INSERT INTO allowed_google_account (email, note, created_at) VALUES ('@g1project.net', '팀 도메인', now());"
   ```

클라이언트 ID를 넣지 않으면 로그인 화면에 "GOOGLE_CLIENT_ID가 설정되지 않았습니다" 안내가 뜬다.
허용 목록에 없는 이메일로 로그인하면 백엔드가 403을 반환하고 프론트에 "허용되지 않은 계정입니다"
메시지가 뜬다. (배포 환경 DB에 등록하려면 `psql` 대신 Supabase 대시보드의 SQL Editor에서
같은 INSERT문을 실행하면 된다 — [배포](#배포-프로덕션) 참고)

## 실행 방법

```bash
# PostgreSQL 최초 설정 (Homebrew 기준)
createdb zcommcx
psql -d zcommcx -c "CREATE ROLE zcommcx_app LOGIN PASSWORD 'zcommcx_app';"
psql -d zcommcx -c "GRANT ALL PRIVILEGES ON DATABASE zcommcx TO zcommcx_app;"
psql -d zcommcx -c "GRANT ALL ON SCHEMA public TO zcommcx_app;"

# 백엔드 (8080)
./gradlew bootRun

# 프론트 (15173, /api 요청은 8080으로 proxy)
cd client && pnpm install && pnpm dev
```

## 배포 (프로덕션)

**https://z-comm-cx.onrender.com** — `main` 브랜치에 push하면 Render가 자동으로 재빌드/재배포한다.

| 구성요소 | 서비스 | 비고 |
|---|---|---|
| 코드 저장소 | GitHub | main 브랜치 push → Render 자동 재배포 트리거 |
| 백엔드 + 프론트엔드 | Render (Web Service, Docker) | 무료 플랜 — **15분간 요청 없으면 슬립**, 다음 요청 때 깨어나는 데 ~1분 걸림 |
| DB | Supabase (Postgres) | 무료 플랜 — **7일간 요청 없으면 자동 일시정지**, Supabase 대시보드에서 수동으로 "Restore" 필요 |

월 비용 $0. 위 표의 슬립/일시정지는 무료 플랜의 정상 동작이라 데모·포트폴리오 용도로는 충분하지만,
꾸준한 트래픽이 필요해지면 유료 플랜(Render Starter, Supabase Pro 등) 전환을 고려한다.

### 배포 방식 — 프론트+백엔드를 하나로

로컬 개발은 지금처럼 `bootRun`(8080)과 Vite dev server(15173)를 따로 띄우지만, 배포 시에는
`Dockerfile`이 프론트엔드를 Spring Boot 정적 리소스로 함께 패키징해 **서비스 하나, URL 하나**로
묶는다.

1. `frontend-build` 스테이지: Node 이미지에서 `client/`를 빌드(`pnpm build`) → `client/dist` 생성
2. `backend-build` 스테이지: `client/dist`를 `src/main/resources/static`으로 복사한 뒤
   `./gradlew bootJar`로 빌드 (산출물 이름은 `app.jar`로 고정하고 plain jar는 비활성화 —
   `build.gradle.kts` 참고. 안 그러면 `build/libs/`에 jar가 2개 생겨 Dockerfile의 복사 경로가
   모호해진다)
3. 최종 런타임 이미지: JRE 위에서 `app.jar` 실행

Vue Router가 history 모드라 `/products/1`처럼 실제 정적 파일이 없는 클라이언트 라우트에 직접
접근하면 Spring의 기본 정적 리소스 핸들러는 404를 반환한다 — `SpaWebConfig`
(`com.zcommcx.config`)가 요청한 파일이 없을 때 `index.html`로 대신 응답해 Vue Router가
라우팅을 이어받게 한다. `/api/**`는 `SiteAuthFilter`가 그보다 먼저 처리하므로 이 폴백과
겹치지 않는다.

### 환경변수 (Render)

로컬은 `application.yml`의 기본값(`localhost` DB 등)을 그대로 쓰고, 배포 환경에서만 아래
환경변수로 오버라이드한다.

| 변수 | 값 |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<host>:5432/postgres` — Supabase **Session pooler** 접속 정보 |
| `SPRING_DATASOURCE_USERNAME` | Session pooler의 `postgres.<project-ref>` |
| `SPRING_DATASOURCE_PASSWORD` | Supabase 프로젝트 생성 시 설정한 DB 비밀번호 |
| `GEMINI_API_KEY` | 로컬 `application-local.yml`과 동일한 값 |
| `GOOGLE_CLIENT_ID` | 로컬 `application-local.yml`과 동일한 값 |
| `PORT` | Render가 자동으로 주입 (직접 설정 불필요, `server.port: ${PORT:8080}`) |

### 배포 후에만 추가로 해야 하는 것

- **Google Cloud Console** OAuth 클라이언트의 "승인된 JavaScript 원본"에 배포 도메인 추가
  (위 "Google 로그인 설정" 참고).
- **로그인 허용 이메일**: 로컬처럼 `psql` 대신 Supabase 대시보드의 **SQL Editor**에서
  `allowed_google_account`에 INSERT (문법은 위 "Google 로그인 설정" 섹션과 동일).

### 배포하면서 겪은 함정

- **pnpm 버전 고정 필요**: `client/package.json`에 `packageManager` 필드로 pnpm 버전을
  고정해뒀다. 없으면 Render의 Docker 빌드 환경이 로컬과 다른 pnpm 버전을 받아오면서
  `pnpm-lock.yaml`(v9 포맷)과 안 맞아 `pnpm install --frozen-lockfile`이 실패한다.
- **Supabase Direct connection은 Render에서 안 통함**: Supabase의 Direct connection
  (`db.<project-ref>.supabase.co`)은 기본이 IPv6 전용이라, IPv4만 지원하는 Render 같은
  플랫폼에서는 연결 자체가 안 된다. 반드시 **Session pooler** 접속 정보(호스트가
  `aws-0-<region>.pooler.supabase.com` 형태, 유저명이 `postgres.<project-ref>` 형태)를
  써야 한다.

## API

### 상품 (통합)

```bash
# 상품 등록 (외부 리뷰 자동 임포트 + 재고 0으로 생성)
curl -X POST http://localhost:8080/api/products -H "Content-Type: application/json" \
  -d '{"url":"https://bylynn.shop/product/NNWBLQE640"}'

# 상품 목록 (서버 페이징, 브랜드/상품코드 필터 — 리뷰건수/재고 포함)
curl "http://localhost:8080/api/products?page=0&size=10"

# 상품 단건 조회 (주문/리뷰작성 화면에서 사용)
curl http://localhost:8080/api/products/1

# 재고 조회/입고/품절
curl http://localhost:8080/api/products/1/inventory
curl -X POST http://localhost:8080/api/products/1/inventory/restock -d '{"quantity":50}'
curl -X POST http://localhost:8080/api/products/1/inventory/out-of-stock
```

### 리뷰

```bash
curl -X POST http://localhost:8080/api/reviews -H "Content-Type: application/json" \
  -d '{"productCode":"NNWBLQE640","memberId":"홍길동","content":"...","rating":5,"hasPhoto":false}'
curl "http://localhost:8080/api/reviews?page=0&size=10"
curl -X POST http://localhost:8080/api/reviews/1/reanalyze   # FAILED 리뷰만 재분석 가능
curl -X PATCH http://localhost:8080/api/reviews/1/classification -d '{"visible":true,"classification":"RECOMMENDED","note":"..."}'
curl "http://localhost:8080/api/products/NNWBLQE640/reviews?page=0&size=10"
curl -X POST http://localhost:8080/api/products/NNWBLQE640/reviews/summary -d '{"query":"사이즈 팁만 요약해줘"}'
curl -X POST http://localhost:8080/api/best-review-shortlist/generate
```

### 주문 / CS

```bash
curl -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" \
  -d '{"customerName":"홍길동","customerPhone":"01011112222","recipientName":"홍길동","recipientPhone":"01011112222","zipcode":"12345","address1":"서울시 강남구","address2":"101동","items":[{"productId":1,"quantity":2}]}'
curl http://localhost:8080/api/orders
curl -X POST http://localhost:8080/api/cs/chat -d '{"message":"주문번호 ORD-XXXX 취소하고 싶어요"}'
curl http://localhost:8080/api/tickets
curl http://localhost:8080/api/guardrail
```

## 프론트엔드 (`/client`)

Vue 3 + Vite + `vue-router` SPA. 로그인 여부는 라우트 가드(`requiresAuth`)로 제어한다.

```
/                          → /admin/products로 리다이렉트 (사이트 로그인 직후 첫 화면)
/login
/products                  고객 상품목록 (브랜드필터/정렬)
/products/:id              상품상세 — 리뷰(AI요약/베스트리뷰/목록) + 주문하기
/products/:id/review       리뷰 작성 (로그인 필요, 작성자는 로그인 이름 자동 사용)
/orders, /orders/new       주문목록 / 주문서 작성
/chat                      CS 채팅
/admin/products            상품관리 (등록 + 목록 + 재고 입고/품절)
/admin/reviews              리뷰관리 (베스트 숏리스트 패널 포함)
/admin/orders                주문관리
/admin/tickets                CS 티켓관리
```

## 알려진 한계 / 다음 단계

- 무료 플랜으로 배포해서 Render는 15분 무요청 시 슬립, Supabase는 7일 무요청 시 일시정지된다
  (자세한 내용은 [배포](#배포-프로덕션) 참고) — 꾸준한 트래픽이 필요해지면 유료 플랜 전환 필요.
- 리뷰 쪽 Gemini 클라이언트와 CS 쪽 Gemini 클라이언트는 아직 분리되어 있다 (위 아키텍처 설명
  참고). 공통 저수준 HTTP 클라이언트로 합치는 건 후속 개선 과제.
- CS 채팅 세션 히스토리는 메모리에만 보관된다 (서버 재시작 시 초기화) — `ai-cs-auto-resolver`
  때부터 있던 한계로 이번 통합에서 손대지 않았다.
- `ai-review-management`의 다른 한계(고객 상품목록 클라이언트 사이드 페이징 등)도 그대로
  이어받았다.
