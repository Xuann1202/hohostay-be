package tw.com.ispan.eeit.ho_back.booking;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import tw.com.ispan.eeit.ho_back.bookingInventory.BookingResponseDTO;
import tw.com.ispan.eeit.ho_back.properties.BookingStatusProperties;

@RestController
public class BookingController {

    @Autowired
    BookingService bookingService;

    @Autowired
    EcpayService ecpayService;

    // ------------------------
    // 1. 建立訂單 + 取得綠界參數
    // ------------------------
    @PostMapping("api/user/booking")
    public ResponseEntity<?> createBooking(@RequestBody BookingDto bookingDto) {
        try {
            Booking booking = bookingService.createBooking(bookingDto);

            List<String> roomNames = bookingDto.getRoomName();
            String itemName = String.join("#", roomNames);

            long millis = System.currentTimeMillis();
            String bookingIdStr = String.format("%04d", booking.getId());
            String tradeNo = "B" + bookingIdStr + String.valueOf(millis);

            Map<String, String> params = ecpayService.createPayment(
                    tradeNo,
                    booking.getTotalPrice(),
                    itemName);

            return ResponseEntity.status(HttpStatus.CREATED).body(params);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("error: " + e.getMessage());
        }
    }

    // ------------------------
    // 2. 會員查詢自己的所有訂單
    // ------------------------
    @GetMapping("/api/user/booking/list")
    public ResponseEntity<?> getCustomerBookings(
            @RequestHeader(value = "userId", required = false) Integer userId) {

        try {
            List<CustomerBookingDTO> bookings = bookingService.findBookingsByCustomerId(userId);
            return ResponseEntity.ok(bookings);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("查詢訂單失敗：" + e.getMessage());
        }
    }

    // ------------------------
    // 3. 會員依訂單 ID 查詢單筆訂單（含權限檢查）
    // ------------------------
    @GetMapping("/api/user/booking/{bookingId}")
    public ResponseEntity<?> getBookingById(
            @RequestHeader(value = "userId", required = true) Integer userId,
            @PathVariable Integer bookingId) {

        try {
            CustomerBookingDTO booking = bookingService.findBookingById(bookingId, userId);
            if (booking == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("找不到訂單或無權限查看");
            }
            return ResponseEntity.ok(booking);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("查詢訂單失敗：" + e.getMessage());
        }
    }

    // ------------------------
    // 4. 飯店業者查詢歷史訂單（Elina）
    // ------------------------
    @GetMapping("/api/findBookingDetails")
    public List<BookingResponseDTO> findBookingDetails(
            @RequestHeader(value = "userId", required = true) Integer userId) {
        try {
            return bookingService.findBookingDetailsByHotelOwner(userId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "查詢訂單失敗");
        }
    }

    // ------------------------
    // 5. 發起綠界支付（根據訂單 ID）
    // ------------------------
    @GetMapping(value = "/api/payment/initiate/{bookingId}")
    public ResponseEntity<?> initiatePayment(
            @PathVariable Integer bookingId,
            @RequestHeader(value = "userId", required = false) Integer userId,
            @RequestHeader(value = "Accept", required = false) String acceptHeader) {

        try {
            // 驗證 userId
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("<html><body><h1>錯誤</h1><p>請先登入</p></body></html>");
            }

            // 查找訂單（確保訂單屬於該用戶）
            Optional<Booking> bookingOpt = bookingService.findBookingByIdAndUserId(bookingId, userId);
            if (bookingOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("<html><body><h1>錯誤</h1><p>找不到訂單或無權限查看</p></body></html>");
            }

            Booking booking = bookingOpt.get();

            // 檢查訂單狀態（應該為未付款）
            if (booking.getStatus() != null &&
                    booking.getStatus().equals(bookingService.getBookingStatusProperties().getPaid())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("<html><body><h1>錯誤</h1><p>此訂單已完成付款</p></body></html>");
            }

            // 構建商品名稱（從訂單的房間資訊）
            String itemName = "住宿訂單";
            if (booking.getBookingInventories() != null && !booking.getBookingInventories().isEmpty()) {
                List<String> roomNames = new java.util.ArrayList<>();
                for (var bi : booking.getBookingInventories()) {
                    if (bi.getInventory() != null &&
                            bi.getInventory().getRoom() != null &&
                            bi.getInventory().getRoom().getName() != null) {
                        roomNames.add(bi.getInventory().getRoom().getName());
                    }
                }
                if (!roomNames.isEmpty()) {
                    itemName = String.join("#", roomNames);
                }
            }

            // 生成交易編號（根據綠界要求：只能包含數字或英文字母）
            // 格式：B + 訂單ID（補零到4位） + 時間戳（後10位）
            // 例如：B0004 + 1234567890 = B00041234567890
            long millis = System.currentTimeMillis();
            // 取時間戳的後10位數字（避免過長）
            String timestamp = String.valueOf(millis).substring(Math.max(0, String.valueOf(millis).length() - 10));
            // 訂單ID補零到4位
            String bookingIdStr = String.format("%04d", bookingId);
            // 組合：B + 訂單ID + 時間戳（只使用數字和英文字母，符合綠界要求）
            String tradeNo = "B" + bookingIdStr + timestamp;

            // 使用 EcpayService 生成支付參數
            // 根據綠界官方文件：https://developers.ecpay.com.tw/?p=2856
            Map<String, String> paymentParams = ecpayService.createPayment(
                    tradeNo,
                    booking.getTotalPrice(),
                    itemName);

            // 檢查 Accept header，如果請求 JSON 則返回 JSON，否則返回 HTML
            if (acceptHeader != null && acceptHeader.contains("application/json")) {
                // 返回 JSON 格式的支付參數（前端會用此生成表單並提交）
                // 注意：PaymentUrl 包含在返回的 Map 中，但不會作為表單欄位
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(paymentParams);
            } else {
                // 返回 HTML 表單（用於直接瀏覽器訪問，例如直接輸入 URL）
                String html = generatePaymentForm(paymentParams);
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(html);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<html><body><h1>錯誤</h1><p>發起支付失敗: " + e.getMessage() + "</p></body></html>");
        }
    }

    /**
     * 生成自動提交到綠界的 HTML 表單
     */
    private String generatePaymentForm(Map<String, String> params) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>正在跳轉到付款頁面...</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }");
        html.append(".loading { font-size: 18px; color: #666; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='loading'>正在跳轉到綠界金流付款頁面，請稍候...</div>");
        html.append("<form id='ecpayForm' method='POST' action='");
        html.append(params.get("PaymentUrl"));
        html.append("'>");

        // 添加所有參數（除了 PaymentUrl）
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!"PaymentUrl".equals(entry.getKey())) {
                html.append("<input type='hidden' name='");
                html.append(escapeHtml(entry.getKey()));
                html.append("' value='");
                html.append(escapeHtml(entry.getValue()));
                html.append("'>");
            }
        }

        html.append("</form>");
        html.append("<script>");
        html.append("document.getElementById('ecpayForm').submit();");
        html.append("</script>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * HTML 轉義工具方法
     */
    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // ------------------------
    // 6. 取消訂單
    // ------------------------
    @PutMapping("/api/user/booking/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Integer bookingId,
            @RequestHeader(value = "userId", required = true) Integer userId) {

        try {
            // 驗證 userId
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("請先登入");
            }

            // 調用服務層取消訂單
            Booking cancelledBooking = bookingService.cancelBooking(bookingId, userId);

            return ResponseEntity.ok("訂單已成功取消");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("取消訂單失敗：" + e.getMessage());
        }
    }
}
