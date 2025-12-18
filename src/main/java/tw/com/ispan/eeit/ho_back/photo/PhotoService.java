package tw.com.ispan.eeit.ho_back.photo;

import java.util.List;

/**
 * 照片服務接口
 */
public interface PhotoService {
    /**
     * 獲取飯店的所有照片
     * 
     * @param hotelId 飯店 ID
     * @return 照片列表
     */
    List<PhotoDTO> getPhotosByHotelId(Integer hotelId);

    /**
     * 根據 ID 獲取照片
     * 
     * @param photoId 照片 ID
     * @return 照片資料
     */
    PhotoDTO getPhotoById(Integer photoId);

    /**
     * 創建照片
     * 
     * @param hotelId 飯店 ID
     * @param dto     照片資料
     * @return 創建的照片
     */
    PhotoDTO createPhoto(Integer hotelId, PhotoDTO dto);

    /**
     * 批次創建照片
     * 
     * @param hotelId 飯店 ID
     * @param dtos    照片資料列表
     * @return 創建的照片列表
     */
    List<PhotoDTO> createPhotos(Integer hotelId, List<PhotoDTO> dtos);

    /**
     * 更新照片
     * 
     * @param photoId 照片 ID
     * @param dto     照片資料
     * @return 更新的照片
     */
    PhotoDTO updatePhoto(Integer photoId, PhotoDTO dto);

    /**
     * 刪除照片
     * 
     * @param photoId 照片 ID
     */
    void deletePhoto(Integer photoId);

    /**
     * 設定封面照片
     * 
     * @param hotelId 飯店 ID
     * @param photoId 照片 ID
     * @return 更新的照片
     */
    PhotoDTO setCoverPhoto(Integer hotelId, Integer photoId);

    /**
     * 調整照片順序
     * 
     * @param photoId  照片 ID
     * @param newOrder 新的順序
     * @return 更新的照片
     */
    PhotoDTO updatePhotoOrder(Integer photoId, Integer newOrder);

    List<String> findPhotosByHotelId(Integer hotelId);
}
