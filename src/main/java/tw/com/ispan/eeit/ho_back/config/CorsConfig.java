package tw.com.ispan.eeit.ho_back.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS 跨域配置
 * 允許前端應用訪問後端 API
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允許的來源（前端應用的地址）
        config.addAllowedOrigin("http://192.168.25.152:3000");
        config.addAllowedOrigin("http://127.0.0.1:3000");
        config.addAllowedOrigin("http://192.168.25.152:5173"); // Vite 默認端口
        config.addAllowedOrigin("http://127.0.0.1:5173");

        config.addAllowedOrigin("http://192.168.25.152:5173");

        // 允許的 HTTP 方法
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("PATCH");

        // 允許的請求頭
        config.addAllowedHeader("*");

        // 是否允許發送 Cookie
        config.setAllowCredentials(true);

        // 預檢請求的有效期（秒）
        config.setMaxAge(3600L);

        // 註冊 CORS 配置
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
