package tw.com.ispan.eeit.ho_back.photo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tw.com.ispan.eeit.ho_back.hotel.Hotel;
import tw.com.ispan.eeit.ho_back.hotel.HotelRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 照片服務實現
 */
@Service
public class PhotoServiceImpl implements PhotoService {

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private HotelRepository hotelRepository;

    /**
     * Entity 轉 DTO
     */
    private PhotoDTO toDto(Photo photo) {
        if (photo == null)
            return null;

        PhotoDTO dto = new PhotoDTO();
        dto.setId(photo.getId());
        dto.setHotelId(photo.getHotel().getId());
        dto.setUrl(photo.getUrl());
        dto.setIsCover(photo.getIsCover());
        dto.setDisplayOrder(photo.getDisplayOrder());
        return dto;
    }

    /**
     * DTO 轉 Entity
     */
    private Photo toEntity(PhotoDTO dto, Hotel hotel) {
        Photo photo = new Photo();
        photo.setHotel(hotel);
        photo.setUrl(dto.getUrl());
        photo.setIsCover(dto.getIsCover() != null ? dto.getIsCover() : false);
        photo.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
        return photo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhotoDTO> getPhotosByHotelId(Integer hotelId) {
        List<Photo> photos = photoRepository.findByHotelId(hotelId);
        return photos.stream()
                .sorted((p1, p2) -> {
                    // 先按 display_order 排序，相同則按 id 排序
                    int orderCompare = Integer.compare(
                            p1.getDisplayOrder() != null ? p1.getDisplayOrder() : 0,
                            p2.getDisplayOrder() != null ? p2.getDisplayOrder() : 0);
                    if (orderCompare != 0)
                        return orderCompare;
                    return Integer.compare(p1.getId(), p2.getId());
                })
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PhotoDTO getPhotoById(Integer photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found: " + photoId));
        return toDto(photo);
    }

    @Override
    @Transactional
    public PhotoDTO createPhoto(Integer hotelId, PhotoDTO dto) {
        // 0. 驗證 URL 是否為空（額外檢查以確保資料完整性）
        if (dto.getUrl() == null || dto.getUrl().trim().isEmpty()) {
            throw new RuntimeException("照片 URL 不能為空");
        }

        // 1. 驗證飯店是否存在
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found: " + hotelId));

        // 2. 計算顯示順序（如果未提供）
        if (dto.getDisplayOrder() == null) {
            List<Photo> existingPhotos = photoRepository.findByHotelId(hotelId);
            int maxOrder = existingPhotos.stream()
                    .mapToInt(p -> p.getDisplayOrder() != null ? p.getDisplayOrder() : 0)
                    .max()
                    .orElse(-1);
            dto.setDisplayOrder(maxOrder + 1);
        }

        // 3. 如果是第一張照片，則設為封面
        List<Photo> existingPhotos = photoRepository.findByHotelId(hotelId);
        if (existingPhotos.isEmpty() && (dto.getIsCover() == null || !dto.getIsCover())) {
            dto.setIsCover(true);
        }

        // 4. 如果設為封面，需將其他照片的封面標記移除
        if (dto.getIsCover() != null && dto.getIsCover()) {
            List<Photo> coverPhotos = existingPhotos.stream()
                    .filter(Photo::getIsCover)
                    .collect(Collectors.toList());
            for (Photo coverPhoto : coverPhotos) {
                coverPhoto.setIsCover(false);
                photoRepository.save(coverPhoto);
            }
        }

        // 5. 創建照片
        Photo photo = toEntity(dto, hotel);
        Photo savedPhoto = photoRepository.save(photo);
        return toDto(savedPhoto);
    }

    @Override
    @Transactional
    public List<PhotoDTO> createPhotos(Integer hotelId, List<PhotoDTO> dtos) {
        // 修改：添加驗證和錯誤處理
        if (dtos == null || dtos.isEmpty()) {
            throw new RuntimeException("照片列表不能為空");
        }
        try {
            return dtos.stream()
                    .map(dto -> {
                        // 驗證每個 DTO
                        if (dto == null) {
                            throw new RuntimeException("照片資料不能為 null");
                        }
                        if (dto.getUrl() == null || dto.getUrl().trim().isEmpty()) {
                            throw new RuntimeException("照片 URL 不能為空");
                        }
                        return createPhoto(hotelId, dto);
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // 記錄錯誤並重新拋出
            System.err.println("創建照片失敗: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("創建照片失敗: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public PhotoDTO updatePhoto(Integer photoId, PhotoDTO dto) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found: " + photoId));

        if (dto.getUrl() != null)
            photo.setUrl(dto.getUrl());
        if (dto.getIsCover() != null) {
            // 如果設為封面，需將該飯店的其他照片的封面標記移除
            if (dto.getIsCover()) {
                List<Photo> coverPhotos = photoRepository.findByHotelId(photo.getHotel().getId()).stream()
                        .filter(p -> !p.getId().equals(photoId) && p.getIsCover())
                        .collect(Collectors.toList());
                for (Photo coverPhoto : coverPhotos) {
                    coverPhoto.setIsCover(false);
                    photoRepository.save(coverPhoto);
                }
            }
            photo.setIsCover(dto.getIsCover());
        }
        if (dto.getDisplayOrder() != null)
            photo.setDisplayOrder(dto.getDisplayOrder());

        Photo savedPhoto = photoRepository.save(photo);
        return toDto(savedPhoto);
    }

    @Override
    @Transactional
    public void deletePhoto(Integer photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found: " + photoId));
        photoRepository.delete(photo);
    }

    @Override
    @Transactional
    public PhotoDTO setCoverPhoto(Integer hotelId, Integer photoId) {
        // 1. 驗證飯店是否存在
        hotelRepository.findById(hotelId)
                .orElseThrow(() -> new RuntimeException("Hotel not found: " + hotelId));

        // 2. 將所有照片的封面標記移除
        List<Photo> photos = photoRepository.findByHotelId(hotelId);
        for (Photo photo : photos) {
            if (photo.getIsCover()) {
                photo.setIsCover(false);
                photoRepository.save(photo);
            }
        }

        // 3. 將選中的照片設為封面
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found: " + photoId));
        photo.setIsCover(true);
        Photo savedPhoto = photoRepository.save(photo);
        return toDto(savedPhoto);
    }

    @Override
    @Transactional
    public PhotoDTO updatePhotoOrder(Integer photoId, Integer newOrder) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new RuntimeException("Photo not found: " + photoId));
        photo.setDisplayOrder(newOrder);
        Photo savedPhoto = photoRepository.save(photo);
        return toDto(savedPhoto);
    }

    @Override
    @Transactional
    public List<String> findPhotosByHotelId(Integer hotelId) {
        List<String> photoUrl = new ArrayList<>();
        if (hotelId != null) {
            Optional<Hotel> op = hotelRepository.findById(hotelId);
            if (op.isPresent()) {
                List<Photo> photos = photoRepository.findByHotelId(hotelId);
                for (Photo photo : photos) {
                    photoUrl.add(photo.getUrl());
                }
                return photoUrl;
            } else
                throw new RuntimeException("飯店不存在");
        }

        return null;
    }
}
