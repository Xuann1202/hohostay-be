package tw.com.ispan.eeit.ho_back.analytics_daily;

import org.modelmapper.ModelMapper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 每日營運分析 API 控制器
 */
@RestController
@RequestMapping("/api/analytics/daily")
public class AnalyticsDailyController {

    private final AnalyticsDailyService service;
    private final ModelMapper modelMapper;
    private final AnalyticsDailyRawQueryRepository rawQueryRepository;

    public AnalyticsDailyController(AnalyticsDailyService service, ModelMapper modelMapper,
            AnalyticsDailyRawQueryRepository rawQueryRepository) {
        this.service = service;
        this.modelMapper = modelMapper;
        this.rawQueryRepository = rawQueryRepository;
    }

    /**
     * 查詢所有統計數據
     * GET /api/analytics/daily
     */
    @GetMapping
    public ResponseEntity<List<AnalyticsDailyDTO>> getAll() {
        try {
            List<AnalyticsDailyDTO> data = service.getAll();
            System.out.println("獲取所有 analytics_daily 數據，共 " + (data != null ? data.size() : 0) + " 筆");
            if (data != null && !data.isEmpty()) {
                System.out.println("第一筆數據範例: " + data.get(0));
            }
            return ResponseEntity.ok(data != null ? data : new java.util.ArrayList<>());
        } catch (Exception e) {
            System.err.println("獲取 analytics_daily 數據時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new java.util.ArrayList<>());
        }
    }

    /**
     * 獲取指定日期的歷史概況數據
     * GET /api/analytics/daily/historical?date=2025-11-25&allowFallback=false
     */
    @GetMapping("/historical")
    public ResponseEntity<AnalyticsDailyDTO> getHistorical(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, defaultValue = "false") Boolean allowFallback) {
        try {
            System.out.println("查詢歷史數據 - date: " + date + ", allowFallback: " + allowFallback);
            AnalyticsDailyDTO data = service.getByDate(date);
            System.out.println("查詢結果: " + (data != null ? "找到數據" : "未找到數據"));
            
            if (data == null) {
                if (allowFallback) {
                    // 如果允許回退，返回最近的一筆數據
                    System.out.println("允許回退，查詢最近的一筆數據");
                    List<AnalyticsDailyDTO> allData = service.getAll();
                    System.out.println("所有數據共 " + allData.size() + " 筆");
                    if (!allData.isEmpty()) {
                        // 按日期排序，返回最新的
                        allData.sort((a, b) -> {
                            if (a.getDate() == null || b.getDate() == null) {
                                return 0;
                            }
                            return b.getDate().compareTo(a.getDate());
                        });
                        System.out.println("返回最近的一筆數據，日期: " + allData.get(0).getDate());
                        return ResponseEntity.ok(allData.get(0));
                    }
                    System.out.println("沒有可用的回退數據");
                }
                // 即使沒有數據，也返回 200 狀態碼，但 body 為 null
                // 這樣前端可以正確處理，而不是收到 404
                System.out.println("返回 null（未找到指定日期的數據）");
                return ResponseEntity.ok().body(null);
            }
            System.out.println("返回找到的數據，日期: " + data.getDate());
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            System.err.println("獲取歷史數據時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * 獲取所有統計數據列表（別名路由，避免與 /{id} 衝突）
     * GET /api/analytics/daily/list
     */
    @GetMapping("/list")
    public ResponseEntity<List<AnalyticsDailyDTO>> getList() {
        return getAll();
    }

    /**
     * 根據 ID 查詢統計數據
     * GET /api/analytics/daily/{id}
     * 使用正則表達式限制 id 必須是數字（至少一位數字）
     * 注意：此路由必須在所有具體路由（如 /list, /historical 等）之後定義
     */
    @GetMapping(value = "/{id}", params = {})
    public ResponseEntity<AnalyticsDailyDTO> getById(@PathVariable String id) {
        try {
            // 先驗證 id 是否為數字
            if (id == null || !id.matches("\\d+")) {
                System.out.println("ID 不是數字格式: " + id);
                return ResponseEntity.notFound().build();
            }
            
            Long idLong = Long.parseLong(id);
            AnalyticsDailyDTO data = service.getById(idLong);
            if (data == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(data);
        } catch (NumberFormatException e) {
            // 如果 id 不是數字，返回 404
            System.out.println("無法將 ID 轉換為數字: " + id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("獲取數據時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 根據日期查詢統計數據
     * GET /api/analytics/daily/date?date=2025-11-11
     */
    @GetMapping("/date")
    public ResponseEntity<AnalyticsDailyDTO> getByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AnalyticsDailyDTO data = service.getByDate(date);
        if (data == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(data);
    }

    /**
     * 新增統計數據
     * POST /api/analytics/daily
     */
    @PostMapping
    public ResponseEntity<AnalyticsDailyDTO> create(@RequestBody AnalyticsDailyDTO dto) {
        AnalyticsDailyDTO saved = service.save(dto);
        return ResponseEntity.ok(saved);
    }

    /**
     * 更新統計數據（支援部分更新）
     * PUT /api/analytics/daily/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<AnalyticsDailyDTO> update(
            @PathVariable Long id,
            @RequestBody AnalyticsDailyDTO dto) {

        // 檢查記錄是否存在
        AnalyticsDailyDTO existing = service.getById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        // 只更新 DTO 中有值的欄位（null 值會被忽略）
        modelMapper.getConfiguration().setSkipNullEnabled(true);
        modelMapper.map(dto, existing);

        // 儲存更新
        AnalyticsDailyDTO updated = service.save(existing);
        return ResponseEntity.ok(updated);
    }

    /**
     * 刪除統計數據
     * DELETE /api/analytics/daily/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 獲取今日新增用戶數
     * GET /api/analytics/daily/new-users/today
     */
    @GetMapping("/new-users/today")
    public ResponseEntity<Map<String, Object>> getTodayNewUsers() {
        LocalDate today = LocalDate.now();
        Integer count = rawQueryRepository.countNewUsersByDate(today);
        if (count == null) {
            count = 0;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        response.put("date", today.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * 獲取今日新增評論數
     * GET /api/analytics/daily/new-reviews/today
     */
    @GetMapping("/new-reviews/today")
    public ResponseEntity<Map<String, Object>> getTodayNewReviews() {
        LocalDate today = LocalDate.now();
        Integer count = rawQueryRepository.countNewReviewsByDate(today);
        if (count == null) {
            count = 0;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        response.put("date", today.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * 獲取今日新增飯店數
     * GET /api/analytics/daily/new-hotels/today
     */
    @GetMapping("/new-hotels/today")
    public ResponseEntity<Map<String, Object>> getTodayNewHotels() {
        LocalDate today = LocalDate.now();
        Integer count = rawQueryRepository.countNewHotelsByDate(today);
        if (count == null) {
            count = 0;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        response.put("date", today.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * 獲取今日客服案件數
     * GET /api/analytics/daily/support-cases/today
     */
    @GetMapping("/support-cases/today")
    public ResponseEntity<Map<String, Object>> getTodaySupportCases() {
        LocalDate today = LocalDate.now();
        Integer count = rawQueryRepository.countSupportCasesByDate(today);
        if (count == null) {
            count = 0;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        response.put("date", today.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * 獲取日期範圍內的訂單與成交額數據
     * 從 analytics_daily 表查詢 total_orders 和 gmv_amount
     * GET /api/analytics/daily/order-revenue?startDate=2025-11-01&endDate=2025-11-30
     */
    @GetMapping("/order-revenue")
    public ResponseEntity<List<Map<String, Object>>> getOrderRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            System.out.println("查詢訂單與成交額數據 - startDate: " + startDate + ", endDate: " + endDate);
            
            // 從 analytics_daily 表查詢指定日期範圍的數據
            // 使用 Service 的 getAll 然後過濾日期範圍
            List<AnalyticsDailyDTO> allData = service.getAll();
            List<AnalyticsDailyDTO> filteredData = allData.stream()
                    .filter(dto -> dto.getDate() != null && 
                            !dto.getDate().isBefore(startDate) && 
                            !dto.getDate().isAfter(endDate))
                    .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                    .collect(java.util.stream.Collectors.toList());
            
            System.out.println("從 analytics_daily 表查詢到 " + filteredData.size() + " 筆記錄");
            
            List<Map<String, Object>> response = new ArrayList<>();
            
            for (AnalyticsDailyDTO dto : filteredData) {
                Map<String, Object> item = new HashMap<>();
                item.put("date", dto.getDate().toString()); // date
                item.put("orders", dto.getTotalOrders() != null ? dto.getTotalOrders() : 0); // total_orders
                item.put("gmv", dto.getGmvAmount() != null ? dto.getGmvAmount().doubleValue() : 0.0); // gmv_amount
                response.add(item);
                System.out.println("訂單數據: date=" + item.get("date") + ", orders=" + item.get("orders") + ", gmv=" + item.get("gmv"));
            }
            
            System.out.println("返回 " + response.size() + " 筆訂單與成交額數據");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("獲取訂單與成交額數據時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ArrayList<>());
        }
    }

    /**
     * 獲取指定月份的聚合數據（用於平台規模成長趨勢 - 月趨勢）
     * 計算該月份所有資料的總和 / 當月份天數，取整數
     * GET /api/analytics/daily/monthly-aggregate?year=2025&month=11
     */
    @GetMapping("/monthly-aggregate")
    public ResponseEntity<AnalyticsDailyDTO> getMonthlyAggregate(
            @RequestParam Integer year,
            @RequestParam Integer month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        int daysInMonth = yearMonth.lengthOfMonth();
        
        // 獲取該月份所有日期的數據
        List<AnalyticsDailyDTO> monthlyData = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            AnalyticsDailyDTO dailyData = service.getByDate(date);
            if (dailyData != null) {
                monthlyData.add(dailyData);
            }
        }
        
        if (monthlyData.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // 計算平均值（總和 / 天數）
        long totalUsers = monthlyData.stream().mapToLong(d -> d.getTotalUsers() != null ? d.getTotalUsers() : 0).sum();
        long totalHotels = monthlyData.stream().mapToLong(d -> d.getTotalHotels() != null ? d.getTotalHotels() : 0).sum();
        long totalOrders = monthlyData.stream().mapToLong(d -> d.getTotalOrders() != null ? d.getTotalOrders() : 0).sum();
        BigDecimal totalGmv = monthlyData.stream()
                .map(d -> d.getGmvAmount() != null ? d.getGmvAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        AnalyticsDailyDTO aggregate = new AnalyticsDailyDTO();
        aggregate.setTotalUsers((int) (totalUsers / daysInMonth));
        aggregate.setTotalHotels((int) (totalHotels / daysInMonth));
        aggregate.setTotalOrders((int) (totalOrders / daysInMonth));
        aggregate.setGmvAmount(totalGmv.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP));
        aggregate.setDate(yearMonth.atDay(1)); // 使用月份第一天作為日期標識
        
        return ResponseEntity.ok(aggregate);
    }

    /**
     * 獲取指定季度的聚合數據（用於平台規模成長趨勢 - 季趨勢）
     * 計算該季度三個月份的數據相加 / 3，取整數
     * GET /api/analytics/daily/quarterly-aggregate?year=2025&quarter=4
     */
    @GetMapping("/quarterly-aggregate")
    public ResponseEntity<AnalyticsDailyDTO> getQuarterlyAggregate(
            @RequestParam Integer year,
            @RequestParam Integer quarter) {
        // 計算該季度的月份範圍
        int startMonth = (quarter - 1) * 3 + 1;
        int endMonth = quarter * 3;
        
        List<AnalyticsDailyDTO> quarterlyData = new ArrayList<>();
        
        // 獲取該季度每個月的聚合數據
        for (int month = startMonth; month <= endMonth; month++) {
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();
            int daysInMonth = yearMonth.lengthOfMonth();
            
            // 獲取該月份所有日期的數據
            List<AnalyticsDailyDTO> monthlyData = new ArrayList<>();
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                AnalyticsDailyDTO dailyData = service.getByDate(date);
                if (dailyData != null) {
                    monthlyData.add(dailyData);
                }
            }
            
            if (!monthlyData.isEmpty()) {
                // 計算該月的平均值
                long totalUsers = monthlyData.stream().mapToLong(d -> d.getTotalUsers() != null ? d.getTotalUsers() : 0).sum();
                long totalHotels = monthlyData.stream().mapToLong(d -> d.getTotalHotels() != null ? d.getTotalHotels() : 0).sum();
                long totalOrders = monthlyData.stream().mapToLong(d -> d.getTotalOrders() != null ? d.getTotalOrders() : 0).sum();
                BigDecimal totalGmv = monthlyData.stream()
                        .map(d -> d.getGmvAmount() != null ? d.getGmvAmount() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                
                AnalyticsDailyDTO monthlyAggregate = new AnalyticsDailyDTO();
                monthlyAggregate.setTotalUsers((int) (totalUsers / daysInMonth));
                monthlyAggregate.setTotalHotels((int) (totalHotels / daysInMonth));
                monthlyAggregate.setTotalOrders((int) (totalOrders / daysInMonth));
                monthlyAggregate.setGmvAmount(totalGmv.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP));
                quarterlyData.add(monthlyAggregate);
            }
        }
        
        if (quarterlyData.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // 計算季度平均值（三個月的平均值相加 / 3）
        long totalUsers = quarterlyData.stream().mapToLong(d -> d.getTotalUsers() != null ? d.getTotalUsers() : 0).sum();
        long totalHotels = quarterlyData.stream().mapToLong(d -> d.getTotalHotels() != null ? d.getTotalHotels() : 0).sum();
        long totalOrders = quarterlyData.stream().mapToLong(d -> d.getTotalOrders() != null ? d.getTotalOrders() : 0).sum();
        BigDecimal totalGmv = quarterlyData.stream()
                .map(d -> d.getGmvAmount() != null ? d.getGmvAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        int monthCount = quarterlyData.size();
        AnalyticsDailyDTO aggregate = new AnalyticsDailyDTO();
        aggregate.setTotalUsers((int) (totalUsers / monthCount));
        aggregate.setTotalHotels((int) (totalHotels / monthCount));
        aggregate.setTotalOrders((int) (totalOrders / monthCount));
        aggregate.setGmvAmount(totalGmv.divide(BigDecimal.valueOf(monthCount), 2, RoundingMode.HALF_UP));
        aggregate.setDate(YearMonth.of(year, startMonth).atDay(1)); // 使用季度第一個月第一天作為日期標識
        
        return ResponseEntity.ok(aggregate);
    }
}
