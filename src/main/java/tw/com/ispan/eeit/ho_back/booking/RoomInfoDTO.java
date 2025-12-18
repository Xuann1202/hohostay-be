package tw.com.ispan.eeit.ho_back.booking;

/**
 * 房型資訊 DTO
 * 用於訂單詳情中的房型列表
 */
public record RoomInfoDTO(
        String roomName,      // 房型名稱
        String bedType,       // 床型名稱
        Integer quantity,     // 數量
        Integer unitPrice,    // 單價
        Integer subtotal) {   // 小計
}


