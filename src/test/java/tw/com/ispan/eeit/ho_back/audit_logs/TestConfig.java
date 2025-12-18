package tw.com.ispan.eeit.ho_back.audit_logs;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import tw.com.ispan.eeit.ho_back.util.OwnerAuthHelper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public RequestContext testRequestContext() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        OwnerAuthHelper ownerAuthHelper = mock(OwnerAuthHelper.class);
        when(ownerAuthHelper.getUserIdFromRequest(request)).thenReturn(1);

        return new TestRequestContext(request, ownerAuthHelper);
    }

    // 測試用的 RequestContext 實作
    static class TestRequestContext extends RequestContext {
        public TestRequestContext(HttpServletRequest request, OwnerAuthHelper ownerAuthHelper) {
            super(request, ownerAuthHelper);
        }

        @Override
        public Long getCurrentUserId() {
            // 在測試中回傳 ID=1（通常是系統管理員帳號）
            // 如果資料庫中沒有 ID=1 的使用者，審計日誌會使用預設值
            return 1L;
        }
    }
}
