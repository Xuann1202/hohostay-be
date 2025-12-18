package tw.com.ispan.eeit.ho_back.bookingInventory;

import java.time.LocalDate;

public record BookingResponseDTO(
                LocalDate startDate,
                String userName, // 入住者姓名
                String hotelName, // 飯店名稱
                String roomName, // 房型名稱
                Long qty, // ✅ 總數量（Long）
                Long inventoryPrice, // ✅ 總金額（Long）
                String status) { // 訂單狀態
}