package edu.thejoeun.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*
BackEnd 와 FrontEnd 가 나누어 작업할 때 사용
corsConfigurer 설정은
WebConfig 이름을 사용하거나 CorsConfig 라는 명칭을 사용하기도 함
회사에서 지정한 명명규칙을 따를 것
*/

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // profile.upload.path
    @Value("${file.upload.path}")
    private String fileUploadPath;
    @Value("${file.product.upload.path}")
    private String productUploadPath;
    @Value("${file.board.upload.path}")
    private String boardUploadPath;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // REST API CORS 설정
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:3001","http://localhost:3000")
                        .allowCredentials(true)
                        .allowedMethods("GET","POST","PUT","DELETE","PATCH","OPTIONS")
                        .allowedHeaders("*");

                // WebSocket CORS 설정 추가
                registry.addMapping("/ws/**")
                        .allowedOrigins("http://localhost:3001","http://localhost:3000")
                        .allowCredentials(true)
                        .allowedMethods("GET","POST","PUT","DELETE","PATCH","OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }

    // 프로필 이미지 정적 리소스 매핑 추가
    // ctrl + o -> override

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /**
         * profile_images/** 로 요청이 오면 실제 파일 시스템 경로에서 이미지 가져오기
         * 폴더별로 registry 설정한다. registry 는 다수가 될 수 있다.
         */
         registry.addResourceHandler("/profile_images/**")
                 .addResourceLocations("file:"+fileUploadPath + "/"); // 폴더명칭뒤에 바로 이미지명칭 붙어서 에러 발생
/*
        // 게시물 이미지 폴더 수정
        registry.addResourceHandler("/profile_images/**")
                .addResourceLocations("file:"+fileUploadPath + "/");
        // 상품   이미지 폴더 수정
        registry.addResourceHandler("/profile_images/**")
                .addResourceLocations("file:"+fileUploadPath + "/");
 */
        /**
         * product_images/** 변수명으로 상품 이미지 데이터를 가져와서 활용하겠다는 요청이 들어오면
         * 실제 파일 시스템 경로에서 이미지를 가져와 사용한다.
         * 현재는 DB 명칭과 바탕화면 폴더 명칭을 동일하게 작성했짐나,
         * DB 명칭은 img_prd 작성하고, 바탕화면 폴더는 풀네임으로 작성하여,
         * 외부에서 함부로 데이터를 가져가거나 서버 정보를 노출하지 않도록 보호할 수 있다.
         *
         * 동작 방식
         * 클라이언트가 /product_images/제품번호/main.jpg 요청하면
         * -> 실제 파일을 바탕화면/product_images/제품번호/main.jpg 경로에서 반환한다.
         *
         * 주의사항
         * file: 뒤에 공백 없이 바로 경로 작성되도록 한다.
         * 경로 끝에 / 붙여서 경로 인식 오류 안 생기도록 작성한다.
         * 프로필/상품 이미지 경로가 각각 독립적으로 설정되어 있는지 확인한다.
         */
        registry.addResourceHandler("/product_images/**")
                .addResourceLocations("file:"+productUploadPath+"/");
        registry.addResourceHandler("/board_images/**")
                .addResourceLocations("file:"+boardUploadPath+"/");
    }
}

