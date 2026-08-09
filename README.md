# LostFound

캠퍼스 분실물 통합 게시판입니다. 물건을 주운 사람은 "분실물 제보" 게시판에, 물건을
잃어버린 사람은 "분실물 찾기" 게시판에 글을 올리고, 지도에서 위치를 확인하고, 댓글로
소통할 수 있습니다. 관리자 승인을 거친 게시글만 공개되며, Spring Boot + Thymeleaf
기반 서버 렌더링 웹앱입니다.

## 주요 기능

- **2종 게시판**: 분실물 제보(주움, 기본 상태 "보관 중") / 분실물 찾기(잃어버림, 기본
  상태 "주인 찾는 중")로 나뉩니다. 각 게시판은 페이지네이션(5건 단위)과 물품 카테고리
  필터를 지원합니다.
- **캠퍼스 지도**: 목록 화면에 [Leaflet.js](https://leafletjs.com/) + OpenStreetMap
  타일로 지도를 띄우고, 좌표가 있는 게시글을 물품 카테고리별 색상 마커로 표시합니다.
- **물품 카테고리 전략 패턴**: 지갑/전자기기/신분증·카드/가방/의류/열쇠/기타 7종 각각
  뱃지 스타일, 설명 문구, 지도 마커 색상, 검색 보조 키워드(동의어)를 전략 클래스로
  분리해 관리합니다.
- **키워드 검색**: 제목/본문/위치명과 카테고리별 보조 키워드(예: 지갑 → "반지갑",
  "카드지갑", "wallet")를 함께 매칭합니다.
- **관리자 승인 워크플로우**: 새 게시글은 승인 전까지 비공개이며, 관리자가 승인
  대기 목록에서 AJAX로 즉시 승인 처리합니다. 승인 없이 삭제도 가능합니다.
- **상태 토글**: 승인된 게시글은 "반환 완료"와 원래 진행 상태(주인 찾는 중/보관 중)를
  오갈 수 있습니다.
- **댓글**: 로그인한 회원만 작성할 수 있고, 삭제는 작성자 본인 또는 관리자만 가능합니다
  (컨트롤러와 서비스 계층에서 이중으로 권한을 검사합니다).
- **이미지 업로드**: AWS S3에 저장하며, 업로드가 실패해도 텍스트 게시글은 그대로
  등록되도록 예외를 게시글 저장과 분리해서 처리합니다. jpg/jpeg/png/gif만 허용합니다.
- **중복 제출 방지**: 게시글 등록 폼에 세션 토큰을 발급해, 새로고침이나 더블클릭으로
  같은 글이 두 번 등록되지 않게 합니다.
- **연락처 정규화**: 입력된 전화번호를 숫자만 추출해 010-0000-0000 형식으로
  통일합니다.
- **회원가입/로그인**: Spring Security + BCrypt를 사용합니다. 로그인 후에는 원래
  보려던 페이지로 돌아가고(SavedRequest), 관리자는 승인 대기 목록으로 이동합니다.

## 기술 스택

Spring Boot 4.0.4 · Java 17 · Spring Data JPA · Spring Security · Thymeleaf ·
MySQL · Spring Cloud AWS(S3) · Lombok · Gradle · Leaflet.js(지도)

## 프로젝트 구조

```
src/main/java/com/example/lostfound/
├─ LostfoundApplication.java     스프링 부트 진입점입니다.
├─ config/
│  ├─ SecurityConfig.java        인증/인가 규칙, 로그인 성공 후 이동 경로, 비밀번호 인코더를 설정합니다.
│  └─ WebConfig.java             /uploads/** 정적 리소스 핸들러(로컬 업로드 디렉터리 매핑)를 설정합니다.
├─ controller/
│  ├─ AuthController.java        로그인/회원가입 화면과 가입 처리를 담당합니다.
│  ├─ ItemController.java        공개 목록/상세/등록/댓글 작성·삭제를 처리합니다(중복 제출 방지 토큰 포함).
│  └─ AdminController.java       승인 대기 목록, 승인(AJAX), 수정, 상태 토글, 삭제 등 운영 기능을 처리합니다.
├─ domain/
│  ├─ entity/
│  │  ├─ LostItem.java           게시글 엔티티입니다 — 카테고리/물품유형/상태/좌표/승인정보, 이미지·댓글 연관관계를 갖습니다.
│  │  ├─ LostItemImage.java      게시글에 첨부된 이미지 메타데이터(원본명/저장명/URL)를 담습니다.
│  │  ├─ Member.java             회원 엔티티입니다(아이디/비밀번호/표시이름/권한).
│  │  └─ Comment.java            게시글 댓글 엔티티입니다.
│  ├─ enums/
│  │  ├─ LostItemCategory.java   REPORT(제보)/SEARCH(찾기) — 게시판 종류별 기본 상태를 정의합니다.
│  │  ├─ LostItemStatus.java     SEARCHING(주인 찾는 중)/FOUND(보관 중)/RESOLVED(반환 완료)를 정의합니다.
│  │  ├─ LostItemType.java       지갑/전자기기/신분증·카드/가방/의류/열쇠/기타를 정의합니다.
│  │  └─ Role.java               USER/ADMIN을 정의합니다.
│  └─ repository/
│     ├─ LostItemRepository.java        승인 상태·카테고리·물품유형 조건으로 조회하고, 완료 항목을 목록 하단으로 정렬합니다.
│     ├─ CommentRepository.java         게시글에 속한 댓글을 조회합니다.
│     ├─ MemberRepository.java          아이디로 회원을 조회하거나 중복 여부를 확인합니다.
│     └─ LostItemImageRepository.java   게시글 이미지를 저장합니다.
├─ dto/
│  ├─ LostItemCreateForm.java / LostItemUpdateForm.java   게시글 작성/수정 입력 폼입니다(검증 어노테이션 포함).
│  ├─ LostItemDetailDto.java / LostItemListDto.java       상세/목록 화면 표시용 DTO입니다(라벨·CSS클래스·마커색 포함).
│  ├─ LostItemStatisticsDto.java   메인 화면 상태별 게시글 수 통계를 담습니다.
│  ├─ CommentCreateForm.java / CommentViewDto.java        댓글 작성 폼과 화면 표시용 DTO입니다.
│  └─ MemberSignupForm.java        회원가입 입력 폼입니다(비밀번호 확인 포함).
├─ initializer/
│  └─ DataInitializer.java       앱 기동 시 기본 관리자 계정이 없으면 생성합니다.
├─ service/
│  ├─ LostItemService.java             게시글/댓글/승인/상태전환/삭제 등 핵심 비즈니스 로직을 담당합니다.
│  ├─ MemberService.java               기본 관리자 계정을 생성합니다.
│  ├─ UserRegistrationService.java     회원가입을 처리합니다(아이디 중복 검사, 비밀번호 암호화).
│  ├─ CustomUserDetailsService.java    Spring Security용 회원 조회를 담당합니다(UserDetailsService 구현).
│  ├─ FileStoreService.java            이미지를 검증하고 S3에 업로드·삭제합니다.
│  ├─ ImageUploadFailedException.java  이미지 업로드 실패 전용 예외입니다.
│  └─ ItemCreateResult.java            게시글 생성 결과(ID, 이미지 업로드 경고 메시지)를 담습니다.
└─ strategy/itemtype/
   ├─ LostItemTypeStrategy.java              물품 카테고리별 규칙 인터페이스입니다(뱃지/설명/마커색/키워드/검색매칭).
   ├─ LostItemTypeStrategyResolver.java      물품 카테고리를 전략 구현체로 매핑합니다(EnumMap 사용).
   └─ WalletItemTypeStrategy.java 등 7종     카테고리별 전략 구현체입니다(지갑/전자기기/신분증/가방/의류/열쇠/기타).

src/main/resources/
├─ application.yml                서버 포트, JPA/Thymeleaf/멀티파트/S3 기본 설정을 담고 있습니다.
├─ static/css/common.css, static/js/common.js   공통 스타일과 CSRF 헤더 첨부·중복 제출 방지·승인 AJAX 스크립트입니다.
└─ templates/
   ├─ layout/base.html, fragments/(header, nav, footer)   공통 레이아웃 조각입니다.
   ├─ auth/(login, signup)                                로그인/회원가입 화면입니다.
   ├─ items/(list, detail, create, edit)                  목록(+지도)/상세/등록/수정 화면입니다.
   └─ admin/pending.html                                  승인 대기 목록 화면입니다.

src/test/java/com/example/lostfound/
├─ controller/(AdminControllerSecurityTest, ItemControllerSecurityTest)   접근 권한(인가) 테스트입니다.
├─ dto/LostItemCreateFormValidationTest.java                              입력 폼 검증 규칙 테스트입니다.
├─ service/LostItemServiceTest.java                                      핵심 서비스 로직 테스트입니다.
└─ strategy/itemtype/LostItemTypeStrategyResolverTest.java                전략 리졸버 테스트입니다.
```

## 실행 방법

1. MySQL 인스턴스를 준비하고, 커밋되지 않는 `src/main/resources/application-local.yml`
   또는 `config/application-local.yml`에 DB 접속 정보를 넣어줍니다(`application.yml`의
   `spring.config.import`가 이 파일을 optional로 불러옵니다).
2. AWS S3 자격 증명을 `.env.example`을 참고해 `.env`로 복사하고 값을 채워줍니다(이미지
   업로드에 필요하며, 없어도 텍스트 게시글 등록은 가능합니다).
3. 아래 명령으로 실행합니다.
   ```bash
   ./gradlew bootRun
   ```
   기본 포트는 5000번입니다(`SERVER_PORT` 또는 `PORT` 환경변수로 변경 가능합니다).
4. 앱이 처음 뜨면 기본 관리자 계정(`admin` / `admin123`)이 자동 생성됩니다. **로컬
   개발용 기본값이므로 배포 전에는 반드시 변경해야 합니다.**

## 테스트

```bash
./gradlew test
```
컨트롤러 접근 권한(인가), 입력 폼 검증, 핵심 서비스 로직, 전략 리졸버에 대한 테스트가
포함되어 있습니다.
