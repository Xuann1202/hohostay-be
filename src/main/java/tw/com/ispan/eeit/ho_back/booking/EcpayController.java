package tw.com.ispan.eeit.ho_back.booking;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tw.com.ispan.eeit.ho_back.properties.BookingStatusProperties;

@RestController
public class EcpayController {
    @Autowired
    EcpayService ecpayService;

    @Autowired
    BookingService bookingService;

    @Autowired
    BookingStatusProperties bookingStatusProperties;

    @PostMapping("api/ecpay/result")
    public String paymentResult(@RequestParam Map<String, String> params) {
        try {
            // 接收綠界回傳的所有參數
            System.out.println(params);

            // 驗證檢查碼
            if (!ecpayService.verifyCheckMacValue(params)) {
                System.err.println("檢查碼驗證失敗！");
                return "0|CheckMacValue Error";
            }

            // 取得付款結果
            String rtnCode = params.get("RtnCode"); // 1=成功
            String merchantTradeNo = params.get("MerchantTradeNo"); // 訂單編號
            String paymentDate = params.get("PaymentDate"); // 付款時間
            System.out.println("付款時間: " + paymentDate);
            // String merchantID = params.get("MerchantID");
            // String paymentType = params.get("PaymentType");
            // String tradNo = params.get("TradeNo");
            if ("1".equals(rtnCode)) {
                System.out.println("付款成功 - 訂單編號: " + merchantTradeNo);

                // 從交易編號中提取訂單 ID
                // 格式可能是: "4" 或 "B00041234567890"
                Integer bookingId = extractBookingIdFromTradeNo(merchantTradeNo);
                System.out.println("提取的訂單 ID: " + bookingId);

                Booking booking = bookingService.updateBookingStatusAfterPay(bookingId,
                        bookingStatusProperties.getPaid());
                System.out.println("booking" + booking);
                // booking.setEcpayPaymentType(paymentType);
                // booking.setEcpayMerchantTradeNo(merchantID);
                // booking.setPaymentDate(LocalDateTime.parse(paymentDate));
                // booking.setEcpayTradeNo(tradNo);
                return "1|OK";
            } else {
                System.out.println("付款失敗 - 訂單編號: " + merchantTradeNo);
                return "0|Payment Failed";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "0|Exception: " + e.getMessage();
        }
    }

    /**
     * 從交易編號中提取訂單 ID
     * 支援兩種格式：
     * 1. 直接是訂單 ID: "4"
     * 2. 格式化的交易編號: "B00041234567890" (B + 4位訂單ID + 時間戳)
     * 
     * @param merchantTradeNo 交易編號
     * @return 訂單 ID
     */
    private Integer extractBookingIdFromTradeNo(String merchantTradeNo) {
        if (merchantTradeNo == null || merchantTradeNo.trim().isEmpty()) {
            throw new IllegalArgumentException("交易編號不能為空");
        }

        // 如果以 "B" 開頭，表示是格式化的交易編號 "B0004..."
        if (merchantTradeNo.startsWith("B") && merchantTradeNo.length() >= 5) {
            // 提取 "B" 後面的 4 位數字（訂單 ID）
            String bookingIdStr = merchantTradeNo.substring(1, 5);
            try {
                return Integer.parseInt(bookingIdStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("無法從交易編號中提取訂單 ID: " + merchantTradeNo, e);
            }
        } else {
            // 直接是訂單 ID
            try {
                return Integer.parseInt(merchantTradeNo);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("無效的交易編號格式: " + merchantTradeNo, e);
            }
        }
    }
}