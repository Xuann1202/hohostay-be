package tw.com.ispan.eeit.ho_back.roomtypebedtype;

import java.util.List;

/**
 * 房型床型配置服務接口
 * 用於管理房型與床型的組合配置
 */
public interface RoomTypeBedTypeService {

    /**
     * 創建房型床型配置
     * 
     * @param dto 房型床型配置資料
     * @return 創建後的配置
     */
    RoomTypeBedTypeDTO createRoomTypeBedType(RoomTypeBedTypeDTO dto);

    /**
     * 更新房型床型配置
     * 
     * @param id  配置 ID
     * @param dto 更新的配置資料
     * @return 更新後的配置
     */
    RoomTypeBedTypeDTO updateRoomTypeBedType(Integer id, RoomTypeBedTypeDTO dto);

    /**
     * 刪除房型床型配置
     * 
     * @param id 配置 ID
     */
    void deleteRoomTypeBedType(Integer id);

    /**
     * 根據 ID 查詢單一配置
     * 
     * @param id 配置 ID
     * @return 配置資料
     */
    RoomTypeBedTypeDTO getRoomTypeBedTypeById(Integer id);

    /**
     * 查詢所有配置
     * 
     * @return 所有配置列表
     */
    List<RoomTypeBedTypeDTO> getAllRoomTypeBedTypes();

    /**
     * 根據房型 ID 查詢配置
     * 
     * @param roomTypeId 房型 ID
     * @return 該房型的所有配置列表
     */
    List<RoomTypeBedTypeDTO> getRoomTypeBedTypesByRoomTypeId(Integer roomTypeId);

    /**
     * 根據床型 ID 查詢配置
     * 
     * @param bedTypeId 床型 ID
     * @return 該床型的所有配置列表
     */
    List<RoomTypeBedTypeDTO> getRoomTypeBedTypesByBedTypeId(Integer bedTypeId);
}
