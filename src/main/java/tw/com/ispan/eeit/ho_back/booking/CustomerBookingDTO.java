package tw.com.ispan.eeit.ho_back.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record CustomerBookingDTO(
                Integer bookingId,
                Integer totalPrice,
                Integer status,
                LocalDate startDate,
                LocalDate endDate,
                LocalDateTime bookingDate,
                String hotelName,           // 飯店名稱
                String photoUrl,            // 飯店封面圖片 URL
                LocalTime checkInTime,      // ✅ 新增：入住時間
                List<RoomInfoDTO> rooms,    // ✅ 新增：房型資訊列表
                Boolean canReview,          // ✅ 新增：是否可以評論（已完成且未評論）
                Boolean hasReview,          // ✅ 新增：是否已有評論
                String request) {           // ✅ 新增：特殊需求/備註
}
