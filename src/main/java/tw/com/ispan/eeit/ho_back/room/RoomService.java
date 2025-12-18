package tw.com.ispan.eeit.ho_back.room;

import java.util.List;

import tw.com.ispan.eeit.ho_back.inventory.dto.HotelQueryDto;

/**
 * 房間服務接口
 */
public interface RoomService {

    /**
     * 創建房間
     * 
     * @param hotelId 飯店 ID
     * @param dto     房間資料
     * @return 創建後的房間
     */
    RoomDTO createRoom(Integer hotelId, RoomDTO dto);

    /**
     * 更新房間
     * 
     * @param roomId 房間 ID
     * @param dto    更新的房間資料
     * @return 更新後的房間
     */
    RoomDTO updateRoom(Integer roomId, RoomDTO dto);

    /**
     * 刪除房間
     * 
     * @param roomId 房間 ID
     */
    void deleteRoom(Integer roomId);

    /**
     * 根據 ID 查詢房間
     * 
     * @param roomId 房間 ID
     * @return 房間資料
     */
    RoomDTO getRoomById(Integer roomId);

    /**
     * 根據飯店 ID 查詢所有房間（公開API，會過濾停業飯店）
     * 
     * @param hotelId 飯店 ID
     * @return 房間列表
     */
    List<RoomDTO> getRoomsByHotelId(Integer hotelId);

    /**
     * 根據飯店 ID 查詢所有房間（後台管理用，不過濾停業狀態）
     * 
     * @param hotelId 飯店 ID
     * @return 房間列表
     */
    List<RoomDTO> getRoomsByHotelIdForOwner(Integer hotelId);

    public List<RoomDetailDto> findRoomInventoryByRoomIds(List<Integer> roomIds, HotelQueryDto query);
}
