package tw.com.ispan.eeit.ho_back.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class SupportTokenHelper {

    @Autowired
    private JsonWebTokenUtils jsonWebTokenUtils;

    /**
     * 從 Cookie 解析 userId
     */
    public Integer getUserId(HttpServletRequest request) {
        String jwtToken = getTokenFromCookie(request);
        if (jwtToken == null)
            return null;

        try {
            String userJson = jsonWebTokenUtils.validateEncryptedToken(jwtToken);
            if (userJson == null)
                return null;

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(userJson);
            return jsonNode.get("id").asInt();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 從 Cookie 擷取 token
     */
    private String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
            return null;

        for (Cookie cookie : cookies) {
            if ("token".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

}