package tw.com.ispan.eeit.ho_back.booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import tw.com.ispan.eeit.ho_back.bookingInventory.BookingInventory;
import tw.com.ispan.eeit.ho_back.bookingInventory.BookingInventoryDto;
import tw.com.ispan.eeit.ho_back.bookingInventory.BookingInventoryRepository;
import tw.com.ispan.eeit.ho_back.bookingInventory.BookingResponseDTO;
import tw.com.ispan.eeit.ho_back.coupon.Coupon;
import tw.com.ispan.eeit.ho_back.coupon.CouponService;
import tw.com.ispan.eeit.ho_back.inventory.Inventory;
import tw.com.ispan.eeit.ho_back.inventory.InventoryRepository;
import tw.com.ispan.eeit.ho_back.inventory.InventoryService;
import tw.com.ispan.eeit.ho_back.properties.BookingStatusProperties;
import tw.com.ispan.eeit.ho_back.user.User;
import tw.com.ispan.eeit.ho_back.user.UserRepository;
import tw.com.ispan.eeit.ho_back.photo.PhotoRepository;
import tw.com.ispan.eeit.ho_back.photo.Photo;
import tw.com.ispan.eeit.ho_back.hotel.Hotel;
import tw.com.ispan.eeit.ho_back.room.Room;

@Service
@Transactional
public class BookingService {
    @Autowired
    BookingRepository bookingRepository;
    @Autowired
    InventoryRepository inventoryRepository;
    @Autowired
    BookingInventoryRepository bookingInventoryRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PhotoRepository photoRepository;

    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private CouponService couponService;
    @Autowired
    BookingStatusProperties bookingStatusProperties;
    @Autowired
    InventoryService inventoryService;

    public Booking createBooking(BookingDto bookingDto) {
        // 檢查Booking參數
        if (bookingDto == null) {
            throw new IllegalArgumentException("bookingDto不能為空");
        }
        if (bookingDto.getUserId() == null) {
            throw new IllegalArgumentException("User ID 不能為空");
        }
        if (bookingDto.getStartDate() == null || bookingDto.getEndDate() == null) {
            throw new IllegalArgumentException("住宿日期不能為空");
        }
        if (!bookingDto.getEndDate().isAfter(bookingDto.getStartDate())) {
            throw new IllegalArgumentException("開始住宿日期不能在結束日期之後");
        }
        System.out.println("bookingDto" + bookingDto);
        User user = userRepository.findById(bookingDto.getUserId())
                .orElseThrow(() -> new RuntimeException("user" + bookingDto.getUserId() + "不存在"));
        Booking booking = modelMapper.map(bookingDto, Booking.class);
        Integer totalPrice = 0;
        booking.setUser(user);

        booking.setTotalPrice(totalPrice);

        // To-do付款方式---------------------------------
        booking.setPaymentType("信用卡");
        booking.setPaymentDate(LocalDateTime.now());
        booking.setStatus(bookingStatusProperties.getUnpaid());
        Integer night = (int) ChronoUnit.DAYS.between(bookingDto.getStartDate(), bookingDto.getEndDate());
        booking.setNight(night);

        bookingRepository.save(booking);

        List<BookingInventory> bookingInventories = booking.getBookingInventories();

        List<BookingInventoryDto> bookingInventoriesDto = bookingDto.getBookingInventoriesDto();

        for (BookingInventoryDto bookingInventoryDto : bookingInventoriesDto) {
            BookingInventory bookingInventory = processOneBookingInventory(bookingInventoryDto, booking);
            bookingInventories.add(bookingInventory);
            System.out.println("bookingInventories" + bookingInventories);
        }
        booking.setBookingInventories(bookingInventories);
        // 檢查訂房日期與庫存日期一致性
        Set<LocalDate> inventoryDates = extractInventoryDates(bookingInventories);
        System.out.println("inventoryDates" + inventoryDates);
        List<LocalDate> sortedDates = new ArrayList<>(inventoryDates);
        Collections.sort(sortedDates);
        System.out.println("sortedDates" + sortedDates);
        Integer day = sortedDates.size();
        System.out.println("day" + day);

        if (!sortedDates.get(0).equals(bookingDto.getStartDate()) ||
                !sortedDates.get(day - 1).equals(bookingDto.getEndDate().minusDays(1))) {
            System.out.println(
                    "bookingDto.getStartDate" + bookingDto.getStartDate() + "-" + bookingDto.getEndDate().minusDays(1));
            System.out.println("庫存日期" + sortedDates.get(0) + "-" + sortedDates.get(day - 1));
            throw new RuntimeException("庫存與入住天數不一致");
        }

        // 檢查庫存是否連續
        for (int i = 1; i < sortedDates.size(); i++) {
            if (!sortedDates.get(i).equals(sortedDates.get(i - 1).plusDays(1))) {
                throw new RuntimeException("庫存日期不連續");
            }
        }
        // 計算總價
        totalPrice = calculateTotalPrice(bookingInventories);
        Integer discountPrice = applyCoupon(booking, bookingDto.getCouponSn(), totalPrice);
        booking.setTotalPrice(discountPrice);

        bookingRepository.save(booking);
        System.out.println("========== 訂單創建完成 ==========");
        System.out.println("Final Booking: " + booking);
        return booking;
    }

    // 更新訂單狀態（付款後）
    public Booking updateBookingStatusAfterPay(Integer id, Integer status) {
        Optional<Booking> op = bookingRepository.findById(id);
        if (op.isPresent()) {
            Booking booking = op.get();
            Integer oldStatus = booking.getStatus();
            booking.setStatus(status);
            booking.setUpdatedTime(LocalDateTime.now());
            // 保存到資料庫
            bookingRepository.save(booking);

            // 處理優惠券邏輯：當狀態變更為已付款（status=2）時，增加優惠券使用次數
            boolean isTargetStatus = (status != null && (status == 2 || status == 4));
            if (isTargetStatus
                    && booking.getCouponId() != null
                    && (oldStatus == null || !oldStatus.equals(status))) {
                // oldStatus 原本不是 2/4 -> 才加次數，避免重複
                if (oldStatus == null || (oldStatus != 2 && oldStatus != 4)) {
                    couponService.incrementUseCount(booking.getCouponId());
                }
            }

            return booking;
        } else {
            throw new RuntimeException("Booking Id 不存在");
        }
    }

    // 處理bookingInventory
    private BookingInventory processOneBookingInventory(BookingInventoryDto dto, Booking booking) {
        // 驗證參數
        validateBookingInventoryDto(dto);

        // 獲取庫存
        Inventory inventory = inventoryRepository.findById(dto.getInventoryId())
                .orElseThrow(
                        () -> new RuntimeException("inventory" + dto.getInventoryId() + "不存在"));

        // 檢查庫存數量
        if (inventory.getStock() < dto.getNumber()) {
            throw new RuntimeException("庫存不足，選擇的房間已售完");
        }

        // 減少庫存
        inventoryRepository.decrementStockByQuantity(inventory.getId(), dto.getNumber());

        // 創建並保存 BookingInventory
        BookingInventory bookingInventory = modelMapper.map(dto, BookingInventory.class);

        Integer price = inventory.getPrice();
        Integer partPrice = price * dto.getNumber();

        bookingInventory.setBookingId(booking.getId());
        bookingInventory.setInventoryId(inventory.getId());
        bookingInventory.setPartPrice(partPrice);
        bookingInventory.setBooking(booking);
        bookingInventory.setInventory(inventory);

        bookingInventoryRepository.save(bookingInventory);

        return bookingInventory;
    }

    // 計算總價
    private Integer calculateTotalPrice(List<BookingInventory> bookingInventories) {
        Integer totalPrice = 0;
        for (BookingInventory bookingInventory : bookingInventories) {
            Integer price = bookingInventory.getPartPrice();
            totalPrice += price;
        }
        return totalPrice;
    }

    // 優惠卷折扣後的價錢
    private Integer applyCoupon(Booking booking, String couponSn, Integer totalPrice) {
        if (couponSn == null || couponSn.trim().isEmpty()) {
            return totalPrice;
        }
        // 檢查優惠卷是否有效
        Coupon coupon = couponService.couponValidate(couponSn);

        // 檢查最低消費限制
        if (totalPrice < coupon.getMinimum()) {
            throw new RuntimeException("訂單金額須滿" + coupon.getMinimum() + "元才可使用此優惠券");
        }
        Integer discountedPrice = totalPrice - coupon.getDiscount();
        booking.setCouponId(coupon.getId());

        return Math.max(0, discountedPrice);
    }

    private void validateBookingInventoryDto(BookingInventoryDto dto) {
        if (dto.getInventoryId() == null) {
            throw new IllegalArgumentException("庫存ID不能為空");
        }
        if (dto.getNumber() == null || dto.getNumber() <= 0) {
            throw new IllegalArgumentException("房間數量必須大於0");
        }
    }

    private Set<LocalDate> extractInventoryDates(List<BookingInventory> bookingInventories) {
        Set<LocalDate> dates = new TreeSet<>();
        for (BookingInventory bi : bookingInventories) {
            dates.add(bi.getInventory().getDate());
        }
        return dates;
    }

    // elina飯店業者的歷史訂單查詢
    public List<BookingResponseDTO> findBookingDetailsByHotelOwner(Integer hotelOwnerId) {
        return bookingInventoryRepository.findBookingDetailsByHotelOwner(hotelOwnerId);
    }

    /**
     * 更新訂單狀態
     * 當訂單狀態變更為 status=2(已付款) 或 status=4(完成) 時，自動更新對應 coupon 的 use_count
     * 
     * @param bookingId 訂單ID
     * @param newStatus 新狀態（"2"=已付款, "4"=完成）
     */
    // **這邊注意status是integer
    public void updateBookingStatus(Integer bookingId, Integer newStatus) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("訂單不存在，ID: " + bookingId));

        Integer oldStatus = booking.getStatus();
        booking.setStatus(newStatus);
        booking.setUpdatedTime(LocalDateTime.now());
        bookingRepository.save(booking);

        // 只處理 2/4 這兩種狀態
        boolean isTargetStatus = (newStatus != null && (newStatus == 2 || newStatus == 4));

        if (isTargetStatus
                && booking.getCouponId() != null
                && (oldStatus == null || !oldStatus.equals(newStatus))) {

            // oldStatus 原本不是 2/4 -> 才加次數，避免重複
            if (oldStatus == null || (oldStatus != 2 && oldStatus != 4)) {
                couponService.incrementUseCount(booking.getCouponId());
            }
        }
    }

    /**
     * 根據訂單 ID 和用戶 ID 查找訂單（用於支付驗證）
     */
    public Optional<Booking> findBookingByIdAndUserId(Integer bookingId, Integer userId) {
        return bookingRepository.findByIdAndUser_Id(bookingId, userId);
    }

    /**
     * 獲取 BookingStatusProperties（用於 Controller 檢查訂單狀態）
     */
    public BookingStatusProperties getBookingStatusProperties() {
        return bookingStatusProperties;
    }

    // 1. 客戶查詢自己的訂單列表
    public List<CustomerBookingDTO> findBookingsByCustomerId(Integer customerId) {

        List<Booking> bookings = bookingRepository.findByUser_IdOrderByBookingDateDesc(customerId);

        List<CustomerBookingDTO> result = new ArrayList<>();

        for (Booking booking : bookings) {
            // 從 Booking -> BookingInventory -> Inventory -> Room -> Hotel 獲取飯店資訊
            String hotelName = null;
            String photoUrl = null;

            // 從第一個 BookingInventory 獲取 Hotel（同一個訂單的所有房間都屬於同一個飯店）
            if (booking.getBookingInventories() != null && !booking.getBookingInventories().isEmpty()) {
                BookingInventory firstBookingInventory = booking.getBookingInventories().get(0);
                if (firstBookingInventory.getInventory() != null
                        && firstBookingInventory.getInventory().getRoom() != null
                        && firstBookingInventory.getInventory().getRoom().getHotel() != null) {

                    Hotel hotel = firstBookingInventory.getInventory().getRoom().getHotel();
                    hotelName = hotel.getName();

                    // 獲取封面圖片
                    Photo coverPhoto = photoRepository.findFirstByHotelIdAndIsCoverTrue(hotel.getId());
                    if (coverPhoto != null) {
                        photoUrl = coverPhoto.getUrl();
                    } else {
                        // 如果沒有封面圖片，獲取第一張圖片
                        List<Photo> photos = photoRepository.findByHotelId(hotel.getId());
                        if (photos != null && !photos.isEmpty()) {
                            photoUrl = photos.get(0).getUrl();
                        }
                    }
                }
            }

            // ✅ 檢查評論狀態
            // 條件：已完成（status = 4）且超過退房時間（endDate < 今天）且確實入住（status = 4 代表已入住）
            // 每筆訂單限一次評論，但可以修改（如果已有評論，顯示「查看評論」或「修改評論」）
            LocalDate today = LocalDate.now();
            Boolean canReview = false;
            Boolean hasReview = (booking.getReview() != null);

            // 只有已完成（status = 4）且超過退房時間的訂單才能評論
            if (booking.getStatus() != null && booking.getStatus() == 4) {
                // 檢查是否超過退房時間（endDate < 今天）
                if (booking.getEndDate() != null && booking.getEndDate().isBefore(today)) {
                    // 如果沒有評論，可以撰寫評論
                    if (booking.getReview() == null) {
                        canReview = true;
                    }
                    // 如果已有評論，hasReview 已經設為 true，可以修改評論
                }
            }

            result.add(new CustomerBookingDTO(
                    booking.getId(),
                    booking.getTotalPrice(),
                    booking.getStatus(),
                    booking.getStartDate(),
                    booking.getEndDate(),
                    booking.getBookingDate(),
                    hotelName,
                    photoUrl,
                    booking.getCheckInTime(), // ✅ 入住時間
                    null, // 列表不需要詳細房型資訊，設為 null
                    canReview, // ✅ 是否可以評論
                    hasReview, // ✅ 是否已有評論
                    booking.getRequest())); // ✅ 特殊需求/備註
        }

        return result;
    }

    /**
     * 取消訂單
     * 
     * @param bookingId 訂單ID
     * @param userId    用戶ID（用於權限檢查）
     * @return 取消後的訂單
     */
    public Booking cancelBooking(Integer bookingId, Integer userId) {
        // 查找訂單並檢查權限
        Optional<Booking> bookingOpt = bookingRepository.findByIdAndUser_Id(bookingId, userId);
        if (bookingOpt.isEmpty()) {
            throw new RuntimeException("找不到訂單或無權限取消");
        }

        Booking booking = bookingOpt.get();

        // 檢查訂單狀態，只有未付款或已付款的訂單才能取消
        Integer unpaidStatus = bookingStatusProperties.getUnpaid();
        Integer paidStatus = bookingStatusProperties.getPaid();
        Integer cancelStatus = bookingStatusProperties.getCancel();

        if (booking.getStatus() == null) {
            throw new RuntimeException("訂單狀態異常");
        }

        // 如果已經取消，不能再取消
        if (booking.getStatus().equals(cancelStatus)) {
            throw new RuntimeException("此訂單已經取消");
        }

        // 如果已完成，不能取消
        Integer completeStatus = bookingStatusProperties.getComplete();
        if (booking.getStatus().equals(completeStatus)) {
            throw new RuntimeException("已完成的訂單無法取消");
        }

        // 設置訂單狀態為已取消
        booking.setStatus(cancelStatus);
        booking.setUpdatedTime(LocalDateTime.now());

        return bookingRepository.save(booking);
    }

    // 2. 客戶查單筆訂單（含權限檢查）
    public CustomerBookingDTO findBookingById(Integer bookingId, Integer userId) {

        Optional<Booking> optional = bookingRepository.findByIdAndUser_Id(bookingId, userId);

        if (!optional.isPresent()) {
            return null;
        }

        Booking booking = optional.get();

        // 從 Booking -> BookingInventory -> Inventory -> Room -> Hotel 獲取飯店資訊
        String hotelName = null;
        String photoUrl = null;

        // 從第一個 BookingInventory 獲取 Hotel
        if (booking.getBookingInventories() != null && !booking.getBookingInventories().isEmpty()) {
            BookingInventory firstBookingInventory = booking.getBookingInventories().get(0);
            if (firstBookingInventory.getInventory() != null
                    && firstBookingInventory.getInventory().getRoom() != null
                    && firstBookingInventory.getInventory().getRoom().getHotel() != null) {

                Hotel hotel = firstBookingInventory.getInventory().getRoom().getHotel();
                hotelName = hotel.getName();

                // 獲取封面圖片
                Photo coverPhoto = photoRepository.findFirstByHotelIdAndIsCoverTrue(hotel.getId());
                if (coverPhoto != null) {
                    photoUrl = coverPhoto.getUrl();
                } else {
                    // 如果沒有封面圖片，獲取第一張圖片
                    List<Photo> photos = photoRepository.findByHotelId(hotel.getId());
                    if (photos != null && !photos.isEmpty()) {
                        photoUrl = photos.get(0).getUrl();
                    }
                }
            }
        }

        // ✅ 查詢房型資訊列表
        List<RoomInfoDTO> roomInfos = new ArrayList<>();
        if (booking.getBookingInventories() != null && !booking.getBookingInventories().isEmpty()) {
            // 按房型分組統計（同一個房型可能有多個 BookingInventory，需要合併）
            for (BookingInventory bi : booking.getBookingInventories()) {
                if (bi.getInventory() != null && bi.getInventory().getRoom() != null) {
                    Room room = bi.getInventory().getRoom();

                    // 取得房型名稱
                    String roomName = room.getName();

                    // 取得床型資訊
                    String bedTypeName = "未設定";
                    if (room.getRoomTypeBedType() != null && room.getRoomTypeBedType().getBedType() != null) {
                        bedTypeName = room.getRoomTypeBedType().getBedType().getName();
                        // 如果有床數，加上床數資訊
                        if (room.getRoomTypeBedType().getBedNumber() != null
                                && room.getRoomTypeBedType().getBedNumber() > 1) {
                            bedTypeName += " x" + room.getRoomTypeBedType().getBedNumber();
                        }
                    }

                    // 數量（從 BookingInventory 取得）
                    Integer quantity = bi.getNumber();

                    // 單價（從 Inventory 取得）
                    Integer unitPrice = bi.getInventory().getPrice();

                    // 小計（從 BookingInventory 取得）
                    Integer subtotal = bi.getPartPrice();

                    roomInfos.add(new RoomInfoDTO(
                            roomName,
                            bedTypeName,
                            quantity,
                            unitPrice,
                            subtotal));
                }
            }
        }

        // ✅ 檢查評論狀態
        // 條件：已完成（status = 4）且超過退房時間（endDate < 今天）且確實入住（status = 4 代表已入住）
        // 每筆訂單限一次評論，但可以修改（如果已有評論，顯示「查看評論」或「修改評論」）
        LocalDate today = LocalDate.now();
        Boolean canReview = false;
        Boolean hasReview = (booking.getReview() != null);

        // 只有已完成（status = 4）且超過退房時間的訂單才能評論
        if (booking.getStatus() != null && booking.getStatus() == 4) {
            // 檢查是否超過退房時間（endDate < 今天）
            if (booking.getEndDate() != null && booking.getEndDate().isBefore(today)) {
                // 如果沒有評論，可以撰寫評論
                if (booking.getReview() == null) {
                    canReview = true;
                }
                // 如果已有評論，hasReview 已經設為 true，可以修改評論
            }
        }

        return new CustomerBookingDTO(
                booking.getId(),
                booking.getTotalPrice(),
                booking.getStatus(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getBookingDate(),
                hotelName,
                photoUrl,
                booking.getCheckInTime(), // ✅ 入住時間
                roomInfos, // ✅ 房型資訊列表
                canReview, // ✅ 是否可以評論
                hasReview, // ✅ 是否已有評論
                booking.getRequest()); // ✅ 特殊需求/備註
    }

}
