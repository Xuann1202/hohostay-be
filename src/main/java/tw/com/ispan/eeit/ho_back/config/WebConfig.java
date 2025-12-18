package tw.com.ispan.eeit.ho_back.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置類
 * 修改：配置靜態資源處理，讓上傳的圖片可以通過 URL 訪問
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${upload.base-path}")
    private String uploadBasePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置上傳文件的靜態資源處理
        // 訪問 /uploads/** 時，會從 uploads 目錄讀取文件
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadBasePath)
                .setCachePeriod(3600);

        // 修改：配置 room_images 目錄的靜態資源處理
        // 訪問 /room_images/** 時，會從 room_images 目錄讀取文件
        registry.addResourceHandler("/room_images/**")
                .addResourceLocations("file:room_images/")
                .setCachePeriod(3600);
    }
}
