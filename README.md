# java_spring-MSA
- 3.1. IPC 개요
    - IPC란?
        - 서로 다른 프로세스들끼리 데이터를 주고받는 행위 또는 그에 대한 방법론을 뜻합니다. MSA에서는 각 서비스가 독립적인 프로세스로 실행되므로, 이들 간의 데이터 교환을 위해 반드시 IPC 기술이 필요합니다.
    - IPC 필요성
        - 분산 환경의 필수 요소
            - 다른 서비스의 데이터가 필요할 때, 해당 서비스의 데이터베이스를 직접 조회하는 것은 MSA의 'Database per Service' 원칙을 위배합니다.
            - 반드시 다른 서비스가 제공하는 API를 통해 데이터를 받아야 합니다.
        - 추상화의 중요성
            - 네트워크 통신은 매우 복잡합니다. (직렬화, 역직렬화, 타임아웃, 로드밸런싱 등을 다루다보면 내가 인프라 개발자인지 백엔드 개발자인지 정신 없죠)
            - 이를 비즈니스 로직과 분리하여 마치 로컬 메서드를 호출하듯 쉽게 만드는 것이 생산성을 높이는데 중요한 요소입니다.
- 3.2. Spring Cloud OpenFeign
    - OpenFeign이란?
        - Netflix가 개발하고 현재는 Spring Cloud 프로젝트의 일부인 선언적(Declarative) HTTP 클라이언트입니다.
        - 선언적(Declarative) HTTP 클라이언트
            - 인터페이스와 어노테이션만으로 REST API 클라이언트를 구현할 수 있습니다.
            - 구현 코드를 일일이 작성하는 것이 아니라, "나는 이런 요청을 보내겠다!"라고 인터페이스와 어노테이션으로 정의(선언)만 하면, 실제 구현체는 Spring이 런타임에 자동으로 만들어준다는 뜻입니다.
            - 자바 인터페이스를 작성하고, `@FeignClient` 어노테이션을 붙이면 REST API를 호출하는 클라이언트가 자동으로 만들어집니다.
        
    - REST API 클라이언트 비교
        
        
        | 비교 항목 | RestTemplate | OpenFeign | WebClient |
        | --- | --- | --- | --- |
        | 통신 방식 | Blocking (동기) | Blocking (동기) | Non-Blocking (비동기) |
        | 구현 방식 | 템플릿 메서드 패턴
        (명령형) | 인터페이스 + 어노테이션 (선언형) | 함수형 스타일
        (Fluent API) |
        | 가독성 | 낮음 (반복적인 
        보일러플레이트 코드) | 매우 높음 (Controller와 유사한 문법) | 중간 (RxJava/Reactor 학습 필요) |
        | Spring 상태 | Maintenance Mode
        (유지보수만 함) | Active
        (표준으로 권장) | Active
        (권장) |
        | 주 사용처 | 레거시 시스템, 
        간단한 테스트 | Spring Cloud 기반 MSA (표준) | 고성능/대용량 트래픽 
        처리 (WebFlux) |
        | 의존성 | `spring-boot-starter-web` | `spring-cloud-starter-openfeign` | `spring-boot-starter-webflux` |
        
    - OpenFeign 장점
        1. 높은 가독성: `RestTemplate`의 복잡한 URL 구성과 파라미터 매핑 로직이 사라지고, 깔끔한 
        자바 인터페이스 형태만 남습니다.
        2. 생산성 향상: 통신을 위한 반복적인 보일러플레이트 코드(연결 설정 등) 작성할 필요가 없어 
        비즈니스 로직에 집중할 수 있습니다.
        3. 유지보수성**:** 코드가 간결하고 직관적이어서, 추후 API가 변경되더라도 인터페이스 명세만 수정하면 되므로 유지보수가 쉽습니다.
        4. Spring MVC와의 호환성: Spring MVC에서 컨트롤러를 만들 때 쓰던 `@GetMapping`, `@RequestParam` 등의 어노테이션을 그대로 사용할 수 있어 쉽게 배울 수 있습니다.
        5. MSA와의 통합: Spring Cloud LoadBalancer, Circuit Breaker, Eureka 등 MSA 필수 컴포넌트들과 매우 부드럽게 연동됩니다. IP 주소 대신 '서비스 이름'으로 호출이 가능합니다.
        
    - OpenFeign 동작 원리
        
        ```mermaid
        sequenceDiagram
            participant Client as Client Service
            participant Feign as Feign Client Interface
            participant Proxy as Feign Proxy (Runtime)
            participant Target as Target Service
        
            Client->>Feign: 메서드 호출 (getProduct(1))
            Note right of Client: 마치 일반 자바 메서드 호출처럼 보임
            Feign->>Proxy: 요청 가로채기
            Note over Proxy: 어노테이션 분석<br/>(@GetMapping, URL 등)
            Proxy->>Target: 실제 HTTP 요청 전송 (REST API)
            Target-->>Proxy: JSON 응답
            Proxy-->>Feign: Java 객체(DTO)로 변환
            Feign-->>Client: 결과 반환
        ```
        
        1. 개발자는 Interface만 정의합니다.
        2. 애플리케이션이 시작될 때(Runtime), Spring이 이 Interface의 Proxy(가짜 구현체)를 생성합니다 (Dynamic Proxy).
        3. 개발자가 메서드를 호출하면 Proxy가 요청을 가로채서 실제 HTTP 요청을 생성하여 보냅니다.
        
- 3.3. [예제] User(Provider) + Board(Consumer) IPC 구축
    - 구현 개요
        - 개요
            - 이번 실습은 "게시글(Board)을 작성하려면 반드시 유효한 사용자(User)여야 한다."는 
            비즈니스 규칙을 구현합니다.
            
        - 기본 구조 및 역할
            1. Eureka Server (8761)
                - 서비스들의 주소(IP:Port) 정보를 Eureka Server에서 중앙 관리합니다.
            2. Common Module
                - DTO를 공유합니다.
                - Exception 처리 방식과 공통 Exception을 공유합니다.
            3. User Service (8081)
                - Provider (데이터 제공)
                - 자기 위치를 Eureka Server에 등록하고, 사용자 정보를 조회하는 REST API 제공합니다.
            4. Board Service (8082)
                - Consumer (데이터 조회)
                - Eureka Server를 조회하여 Provider의 주소를 찾습니다.
                - 게시글 작성 시 User Service를 호출하여 검증하고, 조회 시 작성자 이름을 병합합니다.
            
        - 전체 프로젝트 구조
            
            ```
            openfeign-multi-module
            ├── build.gradle
            ├── gradlew
            ├── settings.gradle
            │
            ├── common-module
            │   ├── build.gradle
            │   └── src/main
            │       └── java
            │           └── com/example/common
            │               ├── dto
            │               │   ├── ErrorResponse.java
            │               │   └── UserResponse.java
            │               └── exception
            │                   ├── BusinessException.java
            │                   └── GlobalExceptionHandler.java
            │
            ├── discovery-service
            │   ├── build.gradle
            │   └── src/main
            │       ├── java
            │       │   └── com/example/discovery
            │       │       └── DiscoveryServiceApplication.java
            │       └── resources
            │           └── application.yml
            │
            ├── user-service
            │   ├── build.gradle
            │   └── src/main
            │       ├── java
            │       │   └── com/example/user
            │       │       ├── UserServiceApplication.java
            │       │       └── controller
            │       │           └── UserController.java
            │       └── resources
            │           └── application.yml
            │
            └── board-service
                ├── build.gradle
                └── src/main
                    ├── java
                    │   └── com/example/board
                    │       ├── BoardServiceApplication.java
                    │       ├── client
                    │       │   └── UserClient.java
                    │       ├── config
                    │       │   ├── OpenFeignConfig.java
                    │       │   └── OpenFeignErrorDecoder.java
                    │       ├── controller
                    │       │   └── BoardController.java
                    │       ├── dto
                    │       │   ├── BoardRequest.java
                    │       │   └── BoardResponse.java
                    │       ├── exception
                    │       │   ├── BoardExceptionHandler.java
                    │       │   └── UserNotFoundException.java
                    │       └── service
                    │           └── BoardService.java
                    └── resources
                        └── application.yml
            ```
            
        
    - 구현
        - Step 1: openfeign-multi-module
            1. `build.gradle`
                
                ```groovy
                // 1. 플러그인 설정
                plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.5.10' apply false
                  id 'io.spring.dependency-management' version '1.1.7' apply false
                }
                
                // 2. 모든 프로젝트(Root + 하위 모듈) 공통 설정
                allprojects {
                  group = 'com.example'
                  version = '0.0.1-SNAPSHOT'
                
                  repositories {
                    mavenCentral()
                  }
                }
                
                // 3. 자식 모듈들에게만 적용할 실질적 빌드 설정
                subprojects {
                  apply plugin: 'java-library'
                  apply plugin: 'io.spring.dependency-management'
                  // common-module을 제외한 나머지 모듈에만 Spring Boot 플러그인 적용
                  if (project.name != 'common-module') {
                    apply plugin: 'org.springframework.boot'
                  }
                  
                  java {
                    toolchain {
                      languageVersion = JavaLanguageVersion.of(17)
                    }
                  }
                
                  configurations {
                    compileOnly {
                      extendsFrom annotationProcessor
                    }
                  }
                
                  dependencies {
                    // Spring Boot Starter
                    implementation 'org.springframework.boot:spring-boot-starter'
                
                    // Lombok
                    compileOnly 'org.projectlombok:lombok'
                    annotationProcessor 'org.projectlombok:lombok'
                    
                    // Spring Configuration Processor
                    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
                
                    // Test
                    testImplementation 'org.springframework.boot:spring-boot-starter-test'
                    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
                  }
                
                  tasks.named('test') {
                    useJUnitPlatform()
                  }
                
                  // Spring Cloud BOM: 변수로 버전 관리
                  ext {
                    set('SpringCloudVersion', '2025.0.0')
                  }
                  dependencyManagement {
                    imports {
                      mavenBom "org.springframework.cloud:spring-cloud-dependencies:${SpringCloudVersion}"
                    }
                  }
                }
                ```
                
            2. `settings.gradle`
                
                ```groovy
                rootProject.name = 'openfeign-project'
                
                include 'common-module'
                include 'discovery-service'
                include 'user-service'
                include 'board-service'
                ```
                
        - Step 2: common-module
            1. `build.gradle`
                - 이 모듈은 실행 가능한 애플리케이션이 아니라 라이브러리(Jar) 형태여야 합니다.
                
                ```groovy
                // 실행 가능한 Boot Jar가 아니라 일반 라이브러리 Jar로 패키징
                jar {
                  enabled = true
                }
                
                dependencies {
                  // Validation: DTO 필드 검증용 (@NotNull, @Size, @Email 등)
                  implementation 'org.springframework.boot:spring-boot-starter-validation'
                
                  // JSON & Time: ObjectMapper, @JsonFormat, LocalDateTime 직렬화 지원
                  // Spring Starter Web보다 가벼운 라이브러리 (Tomcat 없음)
                  implementation 'org.springframework.boot:spring-boot-starter-json'
                }
                
                // 버전 관리가 빠지는 경우 대비(https://docs.spring.io/spring-boot/gradle-plugin/managing-dependencies.html)
                dependencyManagement {
                  imports {
                    mavenBom org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES
                  }
                }
                ```
                
            2. `ErrorResponse.java`
                - 모든 서비스가 공통으로 사용할 에러 JSON 포맷 정의합니다.
                
                ```java
                package com.example.common.dto;
                
                import java.time.LocalDateTime;
                
                // 모든 서비스가 이 포맷으로 에러를 반환하기로 약속합니다.
                public record ErrorResponse(
                  String message, 
                  String code, 
                  LocalDateTime timestamp
                ) {
                  public static ErrorResponse of(String message, String code) {
                    return new ErrorResponse(message, code, LocalDateTime.now());
                  }
                }
                ```
                
            3. `UserResponse.java`
                - 서비스 간 데이터 교환을 위한 규격(Spec)입니다.
                - Provider와 Consumer가 공유할 메시지 포맷(Message Format)입니다.
                
                ```java
                package com.example.common.dto;
                
                public record UserResponse(
                  Long id,
                  String name,
                  String email
                ) {}
                ```
                
            4. `BusinessException.java`
                - 모든 예외가 사용할 기본 예외 클래스입니다.
                - 모든 예외가 공통으로 가질 데이터를 정의합니다.
                - 다른 모든 예외들은 이 예외를 상속 받아 생성합니다.
                
                ```java
                package com.example.common.exception;
                
                import lombok.Getter;
                
                @Getter
                public class BusinessException extends RuntimeException {
                  // 모든 예외가 공통으로 가질 에러 코드
                  private final String errorCode;
                
                  public BusinessException(String message, String errorCode) {
                    super(message);
                    this.errorCode = errorCode;
                  }
                }
                ```
                
            5. `GlobalExceptionHandler.java`
                - 추상화 된 예외 처리 핸들러입니다.
                - 모든 서비스가 공통으로 사용할 예외 처리 핸들러를 정의해 두고, 나머지는 서비스마다 직접 만들어 사용합니다.
                
                ```java
                package com.example.common.exception;
                
                import com.example.common.dto.ErrorResponse;
                import org.springframework.http.HttpStatus;
                import org.springframework.http.ResponseEntity;
                import org.springframework.web.bind.MethodArgumentNotValidException;
                import org.springframework.web.bind.annotation.ExceptionHandler;
                // import org.springframework.web.bind.annotation.RestControllerAdvice;
                
                // 'abstract'로 만들어서 직접 쓰지 말고 상속받아 쓰라고 명시하는 것이 좋습니다.
                
                // 하지만 편의상 @RestControllerAdvice를 붙여서 공통으로 동작하게 할 수도 있습니다.
                // @RestControllerAdvice
                public abstract class GlobalExceptionHandler {
                
                  // 1. 공통: 알 수 없는 서버 에러 (500)
                  @ExceptionHandler(Exception.class)
                  public ResponseEntity<ErrorResponse> handleException(Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ErrorResponse.of(e.getMessage(), "INTERNAL_SERVER_ERROR"));
                  }
                
                  // 2. 공통: 입력값 검증 실패 (400)
                  @ExceptionHandler(MethodArgumentNotValidException.class)
                  public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
                    String errorMessage = e.getBindingResult().getFieldError().getDefaultMessage();
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ErrorResponse.of(e.getMessage(), "INVALID_INPUT"));
                  }
                }
                ```
                
        - Step 3: discovery-service
            1. `build.gradle`
                
                ```groovy
                dependencies {
                  // Spring Cloud Starter Netflix Eureka Server
                  implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-server'
                }
                ```
                
            2. `application.yml`
                
                ```yaml
                server:
                  port: 8761
                
                spring:
                  application:
                    name: discovery-service
                
                eureka:
                  client:
                    register-with-eureka: false
                    fetch-registry: false
                  server:
                    enable-self-preservation: false
                    eviction-interval-timer-in-ms: 3000
                ```
                
            3. `DiscoveryServiceApplication.java`
                
                ```java
                package com.example.discovery;
                
                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;
                import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
                
                @SpringBootApplication
                @EnableEurekaServer
                public class DiscoveryServiceApplication {
                  public static void main(String[] args) {
                    SpringApplication.run(DiscoveryServiceApplication.class, args);
                  }
                }
                ```
                
        - Step 4: user-service
            1. `build.gradle`
                
                ```groovy
                dependencies {
                  // Spring Cloud Starter Netflix Eureka Client
                  implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
                
                  // Spring Starter Web
                  implementation 'org.springframework.boot:spring-boot-starter-web'
                  
                  // common-module
                  implementation project(':common-module')
                }
                ```
                
            2. `application.yml`
                
                ```yaml
                server:
                  port: 8081
                
                spring:
                  application:
                    name: user-service  # Eureka Server에 USER-SERVICE로 등록
                
                eureka:
                  client:
                    service-url:
                      defaultZone: http://localhost:8761/eureka/
                ```
                
            3. `UserController.java`
                
                ```java
                package com.example.user.controller;
                
                import com.example.common.dto.UserResponse;
                import org.springframework.http.HttpStatus;
                import org.springframework.http.ResponseEntity;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PathVariable;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;
                import org.springframework.web.server.ResponseStatusException;
                
                import jakarta.annotation.PostConstruct;
                import java.util.HashMap;
                import java.util.Map;
                
                @RestController
                @RequestMapping("/api/users")
                public class UserController {
                
                  // DB 대용 Mock Data
                  private final Map<Long, UserResponse> userMap = new HashMap<>();
                
                  @PostConstruct
                  public void init() {
                    userMap.put(1L, new UserResponse(1L, "홍길동", "hong@test.com"));
                    userMap.put(2L, new UserResponse(2L, "김철수", "kim@test.com"));
                    userMap.put(3L, new UserResponse(3L, "이영희", "lee@test.com"));
                  }
                
                  @GetMapping("/{userId}")
                  public ResponseEntity<UserResponse> getUser(@PathVariable("userId") Long userId) {
                    // 존재하는 사용자 ID이면 정상 데이터 반환
                    if (userMap.containsKey(userId)) {
                      return ResponseEntity.ok(userMap.get(userId));
                    }
                    // 그 외는 404 반환
                    // Consumer(board-service)의 FeignClient가 에러를 감지하고 ErrorDecoder가 처리
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
                  }
                }
                ```
                
            4. `UserServiceApplication.java`
                
                ```java
                package com.example.user;
                
                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;
                import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
                
                @SpringBootApplication
                @EnableDiscoveryClient
                public class UserServiceApplication {
                  public static void main(String[] args) {
                    SpringApplication.run(UserServiceApplication.class, args);
                  }
                }
                ```
                
        - Step 5: board-service
            1. `build.gradle`
                
                ```groovy
                dependencies {
                  // Spring Cloud Starter Netflix Eureka Client
                  implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
                
                  // Spring Starter Web
                  implementation 'org.springframework.boot:spring-boot-starter-web'
                  
                  // common-module
                  implementation project(':common-module')
                  
                  // Spring Cloud Starter OpenFeign
                  implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'
                }
                ```
                
            2. `application.yml`
                
                ```yaml
                server:
                  port: 8082
                
                spring:
                  application:
                    name: board-service
                
                eureka:
                  client:
                    service-url:
                      defaultZone: http://localhost:8761/eureka/
                
                logging:
                  level:
                    '[com.example.board.client]': DEBUG
                ```
                
            3. `BoardRequest.java`
                
                ```java
                package com.example.board.dto;
                
                public record BoardRequest(
                	String title, 
                	String content, 
                	Long userId
                ) { }
                ```
                
            4. `BoardResponse.java`
                
                ```java
                package com.example.board.dto;
                
                public record BoardResponse(
                	Long boardId, 
                	String title, 
                	String content, 
                	String writerName  // user-sevice로부터 받아서 채우는 데이터
                ) { }
                ```
                
            5. `UserClient.java`
                
                ```java
                package com.example.board.client;
                
                import com.example.common.dto.UserResponse;
                import org.springframework.cloud.openfeign.FeignClient;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PathVariable;
                
                // 인터페이스 + 어노테이션 방식의 선언적 HTTP 클라이언트 방식
                
                // name: 호출하려는 Eureka Service ID (user-service의 spring.application.name)
                @FeignClient(name = "user-service") 
                public interface UserClient {
                
                  // 호출하려는 API 명세와 정확히 일치시켜야 함
                  @GetMapping("/api/users/{userId}")
                  UserResponse getUser(@PathVariable("userId") Long userId);
                }
                ```
                
            6. `OpenFeignConfig.java`
                - OpenFeign 클라이언트가 동작할 때의 세부 설정을 정의하는 Config 파일입니다.
                - 가장 대표적인 역할은 Logger Level 설정입니다.
                    - OpenFeign은 기본적으로 로그를 남기지 않습니다(`NONE`).
                    - 로그 설정을 Full로 설정하면 HTTP 요청과 응답의 모든 명세(Headers, Body, Status Code)를 콘솔에서 확인할 수 있습니다.
                - OpenFeign 로그 레벨 종류
                    1. NONE (기본값): 로그를 남기지 않습니다. 성능상 운영 환경에 적합할 수 있습니다.
                    2. BASIC: 요청 메서드, URL, 응답 상태 코드, 실행 시간만 남깁니다.
                    3. HEADERS: BASIC 정보 + 요청/응답 헤더(Header) 정보를 남깁니다.
                    4. FULL: 요청/응답의 헤더, 바디(Body), 메타데이터 등 모든 정보를 남깁니다. 개발 환경이나 통신 디버깅이 필요할 때 가장 유용합니다.
                
                ```java
                package com.example.board.config;
                
                import feign.Logger;
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                
                @Configuration
                public class OpenFeignConfig {
                
                  // Feign 클라이언트의 로그 레벨을 설정합니다.
                  @Bean
                  Logger.Level feignLoggerLevel() {
                    return Logger.Level.FULL;
                  }
                }
                ```
                
            7. `UserNotFoundException.java`
                - 사용자 번호에 해당하는 사용자 정보가 없는 경우(404)를 처리하는 예외 클래스입니다.
                
                ```java
                package com.example.board.exception;
                
                import com.example.common.exception.BusinessException;
                
                public class UserNotFoundException extends BusinessException {
                
                  public UserNotFoundException(String message) {
                    super(message, "USER_NOT_FOUND"); 
                  }
                }
                ```
                
            8. `OpenFeignErrorDecoder.java`
                - OpenFeign은 기본적으로 요청이 실패(2xx 범위 아님)하면 `FeignException`이라는 일반적인 예외를 발생시킵니다.
                - `ErrorDecoder`는 HTTP 응답(Response)을 가로채서, 개발자가 정의한 구체적인 Java Exception으로 변환하여 다시 던져주는 인터페이스입니다.
                    - 없다면: User Service 404 발생 → `FeignException$NotFound` 발생 → 컨트롤러에서 try-catch로 직접 잡거나, 500 에러로 둔갑
                    - 있다면: User Service 404 발생 → Decoder가 `UserNotFoundException`으로 변환 → `GlobalExceptionHandler`가 잡아서 404 처리
                
                ```java
                package com.example.board.config;
                
                import com.example.board.exception.UserNotFoundException;
                import feign.Response;
                import feign.codec.ErrorDecoder;
                import org.springframework.stereotype.Component;
                
                @Component
                public class OpenFeignErrorDecoder implements ErrorDecoder {
                
                  /**
                   * Feign Client 요청 중 에러(200번대가 아닌 응답)가 발생했을 때 자동으로 호출되는 메서드입니다.
                   * <p>
                   * 상대방 서비스(User Service)가 보내온 HTTP 상태 코드(Status Code)를 분석하여,
                   * 우리 시스템의 비즈니스 로직에 맞는 Java 예외(Exception)로 변환하는 역할을 합니다.
                   * </p>
                   * <p>
                   * 예: 404 상태 코드를 받으면 {@link UserNotFoundException}을 생성하여 반환합니다.
                   * 여기서 반환된 예외 객체는 Feign 프레임워크가 받아서, 최종적으로 메서드를 호출한 곳(Service)에 던져(throw) 줍니다.
                   * </p>
                   *
                   * @param methodKey 호출된 Feign Client 인터페이스의 메서드 이름 (예: UserClient#getUser(Long))
                   * - 어떤 메서드를 실행하다가 에러가 났는지 식별할 때 사용합니다.
                   * @param response  서버로부터 받은 원본 HTTP 응답 객체
                   * - 응답 상태 코드(status), 헤더, 본문(body) 등의 상세 정보가 포함되어 있습니다.
                   * @return 호출자에게 던져질 구체적인 예외 객체 (Exception)
                   */
                  @Override
                  public Exception decode(String methodKey, Response response) {
                    // User Service에서 404가 넘어왔을 때 처리
                    if (response.status() == 404) {
                      return new UserNotFoundException("해당 사용자를 찾을 수 없습니다.");
                    }
                    return new Exception("Generic error");
                  }
                }
                ```
                
            9. `BoardExceptionHandler.java`
                - `OpenFeignErrorDecoder`에서 던진 예외를 잡아서 클라이언트에게 전달합니다.
                
                ```java
                package com.example.board.exception;
                
                import com.example.common.dto.ErrorResponse;
                import com.example.common.exception.GlobalExceptionHandler;
                import org.springframework.http.HttpStatus;
                import org.springframework.http.ResponseEntity;
                import org.springframework.web.bind.annotation.ExceptionHandler;
                import org.springframework.web.bind.annotation.RestControllerAdvice;
                
                @RestControllerAdvice
                public class BoardExceptionHandler extends GlobalExceptionHandler {
                
                  // Board 서비스만의 예외: FeignClient 통신 중 사용자 없음
                  @ExceptionHandler(UserNotFoundException.class)
                  public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException e) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                      .body(ErrorResponse.of(e.getMessage(), e.getErrorCode()));
                  }
                }
                ```
                
            10. `BoardService.java`
                - `UserClient`를 이용하여 user-service로부터 유저 정보를 조회합니다.
                
                ```java
                package com.example.board.service;
                
                import com.example.board.client.UserClient;
                import com.example.board.dto.BoardRequest;
                import com.example.board.dto.BoardResponse;
                import com.example.common.dto.UserResponse;
                import lombok.RequiredArgsConstructor;
                import org.springframework.stereotype.Service;
                
                @Service
                @RequiredArgsConstructor
                public class BoardService {
                
                  private final UserClient userClient;
                
                  public BoardResponse createBoard(BoardRequest request) {
                    // 1. FeignClient를 통해 User Service 호출 (동기 방식)
                    // 사용자가 없으면 ErrorDecoder에 의해 UserNotFoundException 발생
                    UserResponse user = userClient.getUser(request.userId());
                    
                    // 2. 사용자 검증 완료 후 게시글 저장 로직 (여기서는 생략하고 바로 응답 생성)
                    // 실제로는 DB에 save(board) 하는 로직이 들어감
                    Long generatedBoardId = 100L; 
                
                    // 3. 작성자 이름(user.name())을 포함하여 응답 반환
                    return new BoardResponse(
                      generatedBoardId,
                      request.title(),
                      request.content(),
                      user.name() // user-service에서 받아온 데이터 활용
                    );
                  }
                }
                ```
                
            11. `BoardController.java`
                
                ```java
                package com.example.board.controller;
                
                import com.example.board.dto.BoardRequest;
                import com.example.board.dto.BoardResponse;
                import com.example.board.service.BoardService;
                import lombok.RequiredArgsConstructor;
                import org.springframework.http.ResponseEntity;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;
                
                @RestController
                @RequiredArgsConstructor
                @RequestMapping("/api/boards")
                public class BoardController {
                
                  private final BoardService boardService;
                
                  @PostMapping
                  public ResponseEntity<BoardResponse> createBoard(@RequestBody BoardRequest request) {
                    BoardResponse response = boardService.createBoard(request);
                    return ResponseEntity.ok(response);
                  }
                }
                ```
                
            12. `BoardServiceApplication.java`
                
                ```java
                package com.example.board;
                
                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;
                import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
                import org.springframework.cloud.openfeign.EnableFeignClients;
                
                @SpringBootApplication
                @EnableDiscoveryClient
                @EnableFeignClients  // Feign Client를 스캔하고 사용할 수 있도록 설정
                public class BoardServiceApplication {
                  public static void main(String[] args) {
                    SpringApplication.run(BoardServiceApplication.class, args);
                  }
                }
                ```
                
        - 실행 및 테스트
            1. common-module 빌드
                - Root에서 실행
                - 다른 서비스가 사용할 수 있도록 컴파일해서 `.jar` 파일로 만드는 과정
                - `common-module/build/libs/` 경로에 `common-module-0.0.1.jar` 파일 생성 확인
                
                ```bash
                # common-module 빌드하고 싶을 때
                ./gradlew :common-module:clean :common-module:build
                ```
                
            2. discovery-service 실행
                - Root에서 실행
                - 포트 `8761`
                - `http://localhost:8761` 접속하여 대시보드 확인
                
                ```bash
                ./gradlew :discovery-service:bootRun
                ```
                
            3. user-service 실행
                - Root에서 실행
                - 포트 `8081`
                - 대시보드에서 `USER-SERVICE` 등록 확인
                
                ```bash
                ./gradlew :user-service:bootRun
                ```
                
            4. board-service 실행
                - Root에서 실행
                - 포트 `8082`
                - 대시보드에서 `BOARD-SERVICE` 등록 확인
                
                ```bash
                ./gradlew :board-service:bootRun
                ```
                
            5. Postman 테스트
                - 성공 케이스: `POST http://localhost:8082/api/boards`
                    - Body: `{"title": "안녕", "content": "내용", "userId": 1}`
                    - 결과: `writerName`에 "홍길동"이 병합되어 반환
                - 실패 케이스: userId를 `999`로 전송
                    - 결과: `404 Not Found`와 함께 "해당 사용자를 찾을 수 없습니다." 메시지 반환
                
- 3.4. gRPC
    - 공식 홈페이지
        - https://grpc.io/
    - gRPC란?
        - gRPC는 Google Remote Procedure Call의 줄임말입니다. Google에서 개발한 고성능 원격 프로시저 호출(RPC) 프레임워크를 의미합니다.
        - 오픈소스 RPC 프레임워크입니다. HTTP/2를 기반으로 하며, 데이터 포맷으로 JSON 대신 Protocol Buffers(Protobuf)를 사용합니다.
        - gRPC는 CNCF의 공식 프로젝트이며, 2017년부터 현재까지 Incubating 단계입니다.
            - CNCF: Cloud Native Computing Foundation 줄임말로 클라우드 네이티브 프로젝트(Kubernetes, Prometheus 등)를 관리하는 재단
            - CNCF 프로젝트 분류
                - Sandbox → Incubating → Graduated
    
    - gRPC 도입 이유
        1. 성능
            - JSON은 텍스트 기반이라 무겁고 파싱 비용(Jackson의 ObjectMapper 등)이 듭니다. 그에 비해 Protobuf는 이진(Binary) 데이터로 직렬화하여 크기가 작고 통신 속도가 더 빠릅니다.
        2. 정확한 명세
            - JSON은 별도의 스키마 강제성이 없어 API 변경 시 문서와 코드가 불일치하거나 런타임에 오류를 발견할 위험이 있는 반면, gRPC는 `.proto` 파일을 통해 인터페이스를 정의하므로 클라이언트와 서버 간의 명세가 명확하게 유지됩니다.
                
                
    - Protocol Buffers (`.proto`)
        - JSON은 사람이 읽을 수 있는 텍스트지만, 컴퓨터가 파싱하기엔 느립니다. gRPC는 Interface Definition Language (IDL)로 `.proto` 파일을 사용합니다.
        - 역할: API 명세서이자 데이터 스키마입니다.
        - 컴파일: `protoc` 컴파일러가 이 파일을 읽어서 Java, Python, Go 등 원하는 언어의 코드(Stub)를 자동으로 생성해 줍니다. 개발자는 이 생성된 코드를 가져다 쓰기만 하면 됩니다.
            
            
    - HTTP/2 기반의 전송
        - 멀티플렉싱 (Multiplexing): 하나의 TCP 연결로 여러 요청을 동시에 처리합니다. (Head-of-Line Blocking 해결)
        - 헤더 압축 (HPACK): 무거운 헤더의 크기를 줄여 네트워크 비용을 절감합니다.
            
            
    - 아키텍처 이해하기
        
        ```mermaid
        sequenceDiagram
            participant ClientApp as Client Application
            participant Stub as Client Stub (Generated)
            participant Network as HTTP/2 Network
            participant Skeleton as Server Skeleton (Generated)
            participant ServerImpl as Server Implementation
        
            Note over ClientApp, Stub: 메서드 호출하듯이 사용
            ClientApp->>Stub: getUser(request)
            Stub->>Network: Protobuf 직렬화 (Binary) & 전송
            Network->>Skeleton: 데이터 수신
            Skeleton->>ServerImpl: Protobuf 역직렬화 & 함수 호출
            ServerImpl-->>Skeleton: 결과 반환 (Response)
            Skeleton-->>Network: Protobuf 직렬화 & 응답
            Network-->>Stub: 데이터 수신
            Stub-->>ClientApp: 결과 객체 반환
        ```
        
    - gRPC 주소 체계 (Name Resolver)
        
        
        | 스킴 (Scheme) | 예시 주소 | 의미 및 동작 방식 |
        | --- | --- | --- |
        | static:// | `static://127.0.0.1:9090`  | 고정 주소
        (개발용) |
        | discovery:// | `discovery://inventory-service` | 서비스 디스커버리: Eureka Server에 등록한 이름
        (MSA 운영용) |
        | dns:// | `dns://google.com` | DNS 조회: DNS 서버로 도메인의 IP 찾기
        (일반적인 인터넷 통신) |
- 3.5. [예제] gRPC 통신 시스템 구축
    - 구현 개요
        - 개요
            - gRPC 방식으로 서버 간 통신을 구현합니다.
            - `shop-service`가 `inventory-service`에게 상품 ID를 보내고, 재고 수량을 gRPC로 조회합니다.
            - `inventory-service`는 gRPC 서버이고, `shop-service`는 gRPC 클라이언트입니다.
            
        - 전체 프로젝트 구조
            
            ```
            grpc-multi-module
            ├── build.gradle
            ├── gradlew
            ├── settings.gradle
            │
            ├── common-proto
            │   ├── build.gradle
            │   └── src/main/proto
            │       └── inventory.proto  # 서비스 및 메시지 정의 (IDL: Interface Definition Language)
            │
            ├── inventory-service  (gRPC Server)
            │    ├── build.gradle
            │    └── src/main
            │        ├── java
            │        │   └── com/example/inventory
            │        │       ├── InventoryServiceApplication.java
            │        │       └── service
            │        │           └── InventoryService.java
            │        └── resources
            │            └── application.yml
            │
            └── shop-service  (gRPC Client + REST Controller)
                ├── build.gradle
                └── src/main
                    ├── java
                    │   └── com/example/shop
                    │       ├── ShopServiceApplication.java
                    │       └── controller
                    │           └── ShopController.java
                    └── resources
                        └── application.yml
            ```
            
        
    - 구현
        - Step 1: grpc-multi-module
            1. `build.gradle`
                
                ```groovy
                // 1. 플러그인 설정
                plugins {
                  id 'java'
                  id 'org.springframework.boot' version '3.5.10' apply false
                  id 'io.spring.dependency-management' version '1.1.7' apply false
                }
                
                // 2. 모든 프로젝트(Root + 하위 모듈) 공통 설정
                allprojects {
                  group = 'com.example'
                  version = '0.0.1-SNAPSHOT'
                
                  repositories {
                    mavenCentral()
                  }
                }
                
                // 3. 자식 모듈들에게만 적용할 실질적 빌드 설정
                subprojects {
                  apply plugin: 'java-library'
                  apply plugin: 'io.spring.dependency-management'
                  apply plugin: 'org.springframework.boot'
                  
                  java {
                    toolchain {
                      languageVersion = JavaLanguageVersion.of(17)
                    }
                  }
                
                  configurations {
                    compileOnly {
                      extendsFrom annotationProcessor
                    }
                  }
                
                  dependencies {
                    // Spring Boot Starter
                    implementation 'org.springframework.boot:spring-boot-starter'
                
                    // Lombok
                    compileOnly 'org.projectlombok:lombok'
                    annotationProcessor 'org.projectlombok:lombok'
                    
                    // Spring Configuration Processor
                    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
                
                    // Test
                    testImplementation 'org.springframework.boot:spring-boot-starter-test'
                    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
                  }
                
                  tasks.named('test') {
                    useJUnitPlatform()
                  }
                
                  // Spring Cloud BOM: 변수로 버전 관리
                  ext {
                    set('SpringCloudVersion', '2025.0.0')
                  }
                  dependencyManagement {
                    imports {
                      mavenBom "org.springframework.cloud:spring-cloud-dependencies:${SpringCloudVersion}"
                    }
                  }
                }
                ```
                
            2. `settings.gradle`
                
                ```groovy
                rootProject.name = 'grpc-multi-module'
                
                include 'common-proto'
                include 'inventory-service'
                include 'shop-service'
                ```
                
        - Step 2: common-proto
            1. `build.gradle`
                - Protobuf 컴파일을 위한 플러그인을 등록합니다.
                    - 사이트 접속: https://plugins.gradle.org/
                    - 검색: `protobuf` 검색
                    - 확인: `com.google.protobuf` 클릭
                - gRPC 및 Protobuf 라이브러리를 등록합니다.
                
                ```groovy
                // 1. 플러그인 설정
                plugins {
                  // Gradle이 .proto 파일을 인식하고 컴파일할 수 있도록 기능을 추가합니다.
                  // 이 플러그인은 '관리자' 역할이며, 실제 컴파일은 아래 'protobuf' 블록에서 지정한 도구가 수행합니다.
                  id 'com.google.protobuf' version '0.9.6'
                }
                
                // 2. 패키징 설정
                // Spring Boot의 실행 가능한 Fat Jar(bootJar)는 끄고, 일반 라이브러리 Jar(jar)만 만듭니다.
                bootJar { enabled = false }
                jar { enabled = true }
                
                // 3. 의존성 관리
                dependencies {
                  // 3-1. gRPC 통신 엔진 (Netty Shaded)
                  // 일반 Netty 대신 Shaded 버전을 사용하여, 다른 라이브러리의 Netty 버전과 충돌을 방지합니다.
                  implementation 'io.grpc:grpc-netty-shaded:1.69.0'
                
                  // 3-2. Protobuf 연동 라이브러리
                  // .proto 파일로 생성된 메시지 객체를 직렬화/역직렬화하는 기능을 제공합니다.
                  // (내부적으로 grpc-api를 포함하고 있어 별도로 적지 않습니다.)
                  implementation 'io.grpc:grpc-protobuf:1.69.0'
                
                  // 3-3. 클라이언트/서버 스텁 (Stub)
                  // 실제 서비스 인터페이스와 비동기/동기 호출 코드를 지원합니다.
                  implementation 'io.grpc:grpc-stub:1.69.0'
                
                  // 3-4. 어노테이션 지원
                  // 생성된 코드에 붙는 @javax.annotation.Generated 등의 어노테이션을 처리합니다.
                  // Tomcat 등 다른 웹 서버 라이브러리와의 충돌을 막기 위해 compileOnly로 설정합니다.
                  compileOnly 'org.apache.tomcat:annotations-api:6.0.53'
                }
                
                // 4. Protobuf 컴파일 상세 설정
                protobuf {
                  // 4-1. 컴파일러 지정 (작업자)
                  // 플러그인(관리자)에게 이 버전의 protoc를 다운받아 컴파일하라고 지시합니다.
                  protoc {
                    def protocVersion = '3.25.5'
                    artifact = "com.google.protobuf:protoc:${protocVersion}"
                  }
                
                  // 4-2. 코드 생성 플러그인
                  // protoc는 기본적으로 메시지(DTO)만 만듭니다.
                  // gRPC 서비스 코드(InventoryServiceGrpc, 통신 클래스 (Service/Stub))를 만들기 위해서는 별도의 플러그인(protoc-gen-grpc-java)이 필요합니다.
                  plugins {
                    grpc {
                      artifact = 'io.grpc:protoc-gen-grpc-java:1.69.0'
                    }
                  }
                
                  // 4-3. 태스크 연결
                  // 빌드 과정에서 'grpc' 플러그인을 모든 프로토 생성 작업에 적용합니다.
                  // 이 설정이 없으면 서비스 코드(InventoryServiceGrpc, 통신 클래스 (Service/Stub)) 코드가 생성되지 않습니다.
                  generateProtoTasks {
                    all()*.plugins {
                      grpc {}
                    }
                  }
                }
                ```
                
            2. `inventory.proto`
                - API 명세서 역할을 합니다.
                - Gradle Build 실행하면 `build/generated/sources/proto` 경로에 Java 파일이 생성됩니다.
                - 주요 자료형
                    
                    
                    | **Proto 타입** | **Java 타입** | **설명** | **비고 및 예시 코드** |
                    | --- | --- | --- | --- |
                    | double | `double` | 64비트 실수 |  |
                    | float | `float` | 32비트 실수 |  |
                    | int32 | `int` | 32비트 정수 | 일반적인 정수 |
                    | int64 | `long` | 64비트 정수 | DB의 BIGINT, ID 값에 주로 사용 |
                    | bool | `boolean` | 참/거짓 |  |
                    | string | `String` | 문자열 | 항상 UTF-8 인코딩 (최대 2GB) |
                    | repeated | `List<T>` | 배열/리스트 | `repeated string tags = 1;` |
                    | map<K, V> | `Map<K, V>` | 키-값 쌍 | `map<string, int32> inventory = 2;` |
                
                ```protobuf
                // IDL
                
                // 버전 지정
                syntax = "proto3";
                
                // 생성될 Protobuf용 패키지명
                // 메시지를 구분하기 위한 논리적 Namespace로 Java 파일에 영향 없음
                // gRPC 통신 시 내부적으로 사용하는 요청의 이름으로 사용 (com.example.proto.InventoryService/CheckStock)
                package com.example.proto;
                
                // true로 설정하면 자바 파일이 서비스, DTO 별로 낱개로 생성됩니다. (안 쓰면 하나로 뭉침)
                option java_multiple_files = true;
                
                // 생성된 자바 파일이 위치할 실제 패키지 경로입니다.
                option java_package = "com.example.common.inventory";
                
                // 서비스 정의 (Interface)
                service InventoryService {
                  // Proto 정의
                  rpc CheckStock (ProductRequest) returns (StockResponse);
                  // Java 변환 시 모습 (StockResponse 응답은 responseObserver가 처리)
                  // public void checkStock(
                  //   ProductRequest request, 
                  //   StreamObserver<StockResponse> responseObserver) { }
                }
                
                // 메시지 정의
                // 이건 자바도 아니고 파이썬도 아니고 그냥 "IDL"입니다.
                // Java 변환 시 DTO 처럼 동작하나, Builder 패턴으로 생성해야 합니다.
                // 각 필드는 Setter 형식의 메서드가 생성됩니다. (productId -> setProductId())
                // 값을 꺼낼 때는 Getter 형식의 메서드를 호출하면 됩니다.
                message ProductRequest {
                  string productId = 1;  // 1은 필드 고유 태그 번호 (식별자)
                }
                
                message StockResponse {
                  string productId = 1;
                  int32 quantity = 2;
                  bool inStock = 3;
                }
                ```
                
        - Step 3: inventory-service
            1. `build.gradle`
                
                ```groovy
                dependencies {
                  // common-proto
                  implementation project(':common-proto')
                  
                  // gRPC Server: Spring Boot에서 gRPC 서버를 쉽게 띄워주는 스타터 (net.devh)
                  implementation 'net.devh:grpc-server-spring-boot-starter:3.1.0.RELEASE' 
                }
                ```
                
            2. `application.yml`
                
                ```yaml
                # inventory-service
                
                # 웹 포트 (포트 충돌 방지, 모니터링 및 헬스체크(Actuator), 유레카 등록용)
                server:
                  port: 8081
                
                # gRPC 서버 포트 (데이터 주고 받는 용도)
                grpc:
                  server:
                    port: 9090
                ```
                
            3. `InventoryService.java`
                - `@GrpcService` 어노테이션을 추가하여 gRPC 서버에 등록합니다.
                - `InventoryServiceGrpc.InventoryServiceImplBase` 상속 받아 `.proto` 파일에 정의한 API 설계도(인터페이스)를 실제로 구현(Implementation)합니다.
                    - `InventoryServiceGrpc.InventoryServiceImplBase` 클래스
                        - `inventory.proto` 파일을 컴파일러(protoc)가 읽어서 자동으로 만든 추상 클래스
                
                ```java
                package com.example.inventory.service;
                
                import lombok.extern.slf4j.Slf4j;
                import net.devh.boot.grpc.server.service.GrpcService;
                
                import com.example.common.inventory.InventoryServiceGrpc;
                import com.example.common.inventory.ProductRequest;
                import com.example.common.inventory.StockResponse;
                
                import io.grpc.stub.StreamObserver;
                
                @Slf4j
                @GrpcService // 이 어노테이션이 있어야 gRPC 서버에 등록됨 (스프링 부트 실행 시 자동으로 gRPC 서버(9090 포트)가 올라감)
                public class InventoryService extends InventoryServiceGrpc.InventoryServiceImplBase {
                
                  @Override
                  public void checkStock(ProductRequest request, StreamObserver<StockResponse> responseObserver) {
                    log.info("gRPC 요청 수신: Product ID = {}", request.getProductId());
                
                    // 1. 비즈니스 로직 (Mock Data)
                    // 제품ID 999 : 재고 0
                    // 그 외      : 재고 100
                    int stock = 100;
                    if ("999".equals(request.getProductId())) {
                      stock = 0;
                    }
                
                    // 2. 응답 객체 생성 (Builder 패턴 사용 - Protobuf 자동 생성 기능)
                    StockResponse response = StockResponse.newBuilder()
                      .setProductId(request.getProductId())
                      .setQuantity(stock)
                      .setInStock(stock > 0)
                      .build();
                
                    // 3. 클라이언트로 응답 전송 (비동기 콜백 방식)
                    responseObserver.onNext(response); // 데이터 전달
                    responseObserver.onCompleted();    // 처리 완료 신호
                  }
                }
                ```
                
            4. `InventoryServiceApplication.java`
                
                ```java
                package com.example.inventory;
                
                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;
                
                @SpringBootApplication
                public class InventoryServiceApplication {
                  public static void main(String[] args) {
                    SpringApplication.run(InventoryServiceApplication.class, args);
                  }
                }
                ```
                
        - Step 4: shop-service
            1. `build.gradle`
                
                ```groovy
                dependencies {
                  // common-proto
                  implementation project(':common-proto')
                  
                  // Spring Starter Web
                  implementation 'org.springframework.boot:spring-boot-starter-web'
                  
                  // gRPC Client
                  implementation 'net.devh:grpc-client-spring-boot-starter:3.1.0.RELEASE'
                }
                ```
                
            2. `application.yml`
                
                ```yaml
                # shop-service
                
                server:
                  port: 8080
                
                # gRPC Client 정보
                grpc:
                  client:
                    inventory-service:  # 클라이언트 이름
                      address: static://localhost:9090  # gRPC 서버 주소 필요
                      negotiation-type: plaintext  # SSL 없이 통신 (개발용). Production이라면 TLS (암호화. 인증서 필요)
                ```
                
            3. `ShopController.java`
                
                ```java
                package com.example.shop.controller;
                
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PathVariable;
                import org.springframework.web.bind.annotation.RestController;
                
                import com.example.common.inventory.InventoryServiceGrpc;
                import com.example.common.inventory.ProductRequest;
                import com.example.common.inventory.StockResponse;
                
                import net.devh.boot.grpc.client.inject.GrpcClient;
                
                // HTTP/1.1 REST 요청을 받지만, 내부적으로는 HTTP/2 gRPC 고속 통신을 수행합니다.
                
                @RestController
                public class ShopController {
                
                  // @GrpcClient: application.yml에 정의된 inventory-service 설정을 주입받음
                  // BlockingStub: 동기(Synchronous) 방식의 클라이언트
                  @GrpcClient("inventory-service")
                  private InventoryServiceGrpc.InventoryServiceBlockingStub inventoryStub;
                
                  @GetMapping("/shop/{productId}")
                  public String checkProductStock(@PathVariable String productId) {
                    
                    // 1. gRPC 요청 객체 생성
                    ProductRequest request = ProductRequest.newBuilder()
                      .setProductId(productId)
                      .build();
                
                    // 2. gRPC 호출 (마치 로컬 메서드 호출하듯이 사용)
                    StockResponse response = inventoryStub.checkStock(request);
                
                    // 3. 결과 반환
                    return String.format("상품: %s, 재고: %d, 판매가능: %s", 
                      response.getProductId(), response.getQuantity(), response.getInStock());
                  }
                }
                ```
                
            4. `ShopServiceApplication.java`
                
                ```java
                package com.example.shop;
                
                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;
                
                @SpringBootApplication
                public class ShopServiceApplication {
                  public static void main(String[] args) {
                    SpringApplication.run(ShopServiceApplication.class, args);
                  }
                }
                ```
                
        - 실행 및 테스트
            1. common-proto 빌드
                - Root에서 실행
                - 실행 후 `common-proto/build/generated/sources/proto/main/…` 경로에 파일들이 생성되었는지 확인
                    
                    ```bash
                    # 문법: ./gradlew :{모듈명}:{태스크명}
                    ./gradlew :common-proto:clean :common-proto:build
                    ```
                    
                
            2. inventory-service 실행
                - gRPC 서버 실행
                - 포트 `9090`
                    
                    ```bash
                    ./gradlew :inventory-service:bootRun
                    ```
                    
                
            3. shop-service 실행
                - gRPC 클라이언트 실행
                - 포트 `8080`
                    
                    ```bash
                    ./gradlew :shop-service:bootRun
                    ```
                    
                
            4. 테스트
                - 브라우저나 Postman으로 `http://localhost:8080/shop/123` 호출
                    - 결과: "상품: 123, 재고: 100, 판매가능: true" 확인
                - 브라우저나 Postman으로 `http://localhost:8080/shop/999` 호출
                    - 결과: "상품: 999, 재고: 0, 판매가능: false" 확인
        
- 3.6. [실습] IPC 통합 활용 시스템 구축
    - 요구사항
        - 프로젝트 개요
            - 간단한 금융 시스템을 구축합니다. 사용자가 송금 요청을 보냈을 때 다음 두 가지 작업을 
            순차적으로 처리해야 합니다.
                1. 이체: 실제로 돈을 이체하는 작업. 매우 빠르고 정확한 명세가 필요하므로 
                gRPC로 통신합니다. (당연하게도 실제 이체는 일어나지 않습니다.)
                2. 알림: 이체 결과를 사용자에게 알림톡(SMS) 형식으로 전송합니다. 일반적인 
                REST로 통신합니다. (역시 실제 SMS 보낼 필요는 없습니다.)
        
        - 프로젝트 구성 요소
            - common-proto
                - gRPC 통신을 위한 메시지와 서비스를 정의합니다.
            - discovery-service
                - 모든 서비스의 주소를 관리하는 서비스 레지스트리입니다.
            - banking-service
                - gRPC Server
                - 이체 요청을 처리하고 트랜잭션-ID(UUID) 반환합니다.
            - notification-service
                - REST Server
                - 이체 성공 메시지를 받아 로그를 출력합니다.
            - transfer-service
                - 사용자의 이체 요청을(`POST /transfer`) 받아 처리합니다.
                - banking-service 호출하여 이체를 수행합니다. (gRPC)
                - 이체 성공 시 notification-service 호출하여 알림을 보냅니다. (OpenFeign)
                - 최종적으로  transfer-service는 사용자에게 JSON 응답을 반환합니다.
            
        - 전체 프로젝트 구조
            
            ```
            ipc-multi-module
            ├── gradlew
            ├── build.gradle
            ├── gradlew
            ├── settings.gradle
            ├── common-proto          [gRPC IDL 정의 및 자동 생성된 Java 클래스 공유]
            ├── discovery-service     [Eureka Server - 서비스 레지스트리]
            ├── banking-service       [gRPC Server - 고속 이체 트랜잭션 처리]
            ├── notification-service  [REST Server - OpenFeign이 호출할 알림 API 제공]
            └── transfer-service      [Client - gRPC Client와 OpenFeign Client 동시 활용]
            ```
            
        - common-proto 모듈 구조
            
            ```
            common-proto
            ├── build.gradle
            └── src/main/proto
                └── banking.proto
            ```
            
        - discovery-service 모듈 구조
            
            ```
            discovery-service
            ├── build.gradle
            └── src/main
                ├── java
                │   └── com/example/discovery
                │       └── DiscoveryServiceApplication.java
                └── resources
                    └── application.yml
            ```
            
        - banking-service 모듈 구조
            
            ```
            bank-service
            ├── build.gradle
            └── src/main
                ├── java
                │   └── com/example/banking
                │       ├── BankingServiceApplication.java
                │       └── service
                │           └── BankingService.java
                └── resources
                    └── application.yml
            ```
            
        - notification-service 모듈 구조
            
            ```
            notification-service
            ├── build.gradle
            └── src/main
                ├── java
                │   └── com/example/notify
                │       ├── NotificationServiceApplication.java
                │       ├── controller
                │       │   └── NotificationController.java
                │       └── dto
                │           └── NotificationRequest.java
                └── resources
                    └── application.yml
            ```
            
        - transfer-service 모듈 구조
            
            ```
            transfer-service
            ├── build.gradle
            └── src/main
                ├── java
                │   └── com/example/transfer
                │       ├── TransferServiceApplication.java
                │       ├── client
                │       │   └── NotificationClient.java
                │       ├── controller
                │       │   └── TransferController.java
                │       └── dto
                │           ├── NotificationDto.java
                │           └── UserTransferRequest.java
                └── resources
                    └── application.yml
            ```
