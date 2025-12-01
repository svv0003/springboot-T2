package edu.thejoeun.common.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * @Configuration - 환경설정용 클래스임을 명시
 * 스프링부트는 프로젝트 실행할 때 @Configuration 어노테이션을 가장 먼저 확인
 * 객체로 생성해서 내부 코드를  서버 실행시 모두 실행
 * @Bean 개발자가 수동으로 생성한 객체의 관리를
 * 스프링부트에서 자체적으로 관리하라고 넘기는 어노테이션
 */
@Configuration
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())

                /**
                 * 권한 설정 - 모든 요청 허용
                 * 현재는 개발 단계이기 때문에 특정 api나 url 경로 설정에 대하여
                 * 접근 제한을 하지 않고 모두 아이디 비밀번호 없이 접근 가능하도록 설정하나,
                 * 모든 프로젝트를 마무리하고 배포작업을 진행하기 전에는
                 * 커스텀 URL 접근 설정을 진행해야한다.
                 */
                .authorizeHttpRequests(
                        auth -> auth
                        .anyRequest().permitAll())

                /**
                 * 특정 페이지는 아이디 비밀번호 없이 접근 가능하도록 권한 설정
                 * 정적 리소스는 인증 없이 접근 가능
                 * 리액트로 진행하는 프론트엔드 또한 빌드작업을 하여
                 * resource 와 static 으로 정적데이터를 가져오면
                 * 하위 폴더에 접근할 수 있는 권한이 존재해야하기 때문에 미리 작성

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/profile_images/**",
                                        "/css/**",
                                        "/js/**",
                                        "/images/**"
                                ).permitAll()
                        .anyRequest().authenticated()

                )
                */
                .formLogin(AbstractHttpConfigurer::disable)

                // HTTP Basic 인증 비활성화
                .httpBasic(basic -> basic.disable())

                // 세션 관리 설정 추가
                .sessionManagement(session -> session
                        .maximumSessions(1) // 동시 세션 1개만 허용
                        .maxSessionsPreventsLogin(false) // 새 로그인 시 기존 세 무효화
                );

                /*
                SecurityConfig는 모든 API 경로에서 최우선 권한을 갖고 있다.
                MemberAPIController에서 작동해야 하는 로그아웃 경로를
                Security가 가로챈다.
                -> MemberAPIController 로그아웃 동작하지 않는다.

               .logout(logout -> logout
                    .logoutUrl("/api/auth/logout")
                    .permitAll()
                );
                 */

        return http.build();
    }

}
