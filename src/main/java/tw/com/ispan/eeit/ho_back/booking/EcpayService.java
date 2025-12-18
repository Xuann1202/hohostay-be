package tw.com.ispan.eeit.ho_back.booking;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

@Service
@PropertySource("classpath:hotel_platform.properties")
public class EcpayService {
    @Value("${ecpay.merchant.id}")
    private String merchantId;
    @Value("${ecpay.hash.key}")
    private String hashKey;
    @Value("${ecpay.hash.iv}")
    private String hashIv;
    @Value("${ecpay.payment.url}")
    private String paymentUrl;
    @Value("${ecpay.return.url}")
    private String returnUrl;
    @Value("${ecpay.client.back.url}")
    private String clientBackURL;
    @Value("${ecpay.order.result.url}")
    private String orderResultUrl;

    public Map<String, String> createPayment(String orderId, Integer totalAmount, String itemName) {
        Map<String, String> params = new TreeMap<>();
        params.put("MerchantID", merchantId);
        params.put("MerchantTradeNo", orderId);
        params.put("MerchantTradeDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
        params.put("PaymentType", "aio");
        params.put("TotalAmount", String.valueOf(totalAmount));
        params.put("TradeDesc", "預訂房間");
        params.put("ItemName", itemName);
        params.put("ReturnURL", returnUrl);
        params.put("ChoosePayment", "ALL");
        params.put("EncryptType", "1");
        params.put("ClientBackURL", clientBackURL);
        // params.put("OrderResultURL", orderResultUrl);
        params.put("PaymentUrl", paymentUrl);
        String checkMacValue = generateCheckMacValue(params);
        params.put("CheckMacValue", checkMacValue);
        return params;
    }

    public String generateCheckMacValue(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        // 將參數用&連接
        sb.append("HashKey=").append(hashKey);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!"PaymentUrl".equals(entry.getKey()) && !"CheckMacValue".equals(entry.getKey())) {
                sb.append("&").append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        sb.append("&HashIV=").append(hashIv);

        // 將整串字串進行URL encode並轉為小寫
        String encodeString = URLEncoder.encode(sb.toString(), StandardCharsets.UTF_8)
                .replace("%2d", "-")
                .replace("%5f", "_")
                .replace("%2e", ".")
                .replace("%21", "!")
                .replace("%2a", "*")
                .replace("%28", "(")
                .replace("%29", ")")
                .toLowerCase();
        // sha-256加密
        String checkMacValue = sha256(encodeString).toUpperCase();
        return checkMacValue;
    }

    public String sha256(String value) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA256 加密失敗", e);
        }
    }

    public boolean verifyCheckMacValue(Map<String, String> params) {
        String receivedCheckMacValue = params.get("CheckMacValue");
        if (receivedCheckMacValue == null) {
            return false;
        }

        // 移除 CheckMacValue 後重新計算
        Map<String, String> paramsForCheck = new TreeMap<>(params);
        paramsForCheck.remove("CheckMacValue");

        String calculatedCheckMacValue = generateCheckMacValue(paramsForCheck);

        return receivedCheckMacValue.equalsIgnoreCase(calculatedCheckMacValue);
    }
}
