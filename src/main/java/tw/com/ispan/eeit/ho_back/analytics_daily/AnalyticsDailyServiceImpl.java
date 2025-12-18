package tw.com.ispan.eeit.ho_back.analytics_daily;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnalyticsDailyServiceImpl implements AnalyticsDailyService {

    private final AnalyticsDailyRepository repository;
    private final AnalyticsDailyCollectorService collectorService;

    public AnalyticsDailyServiceImpl(AnalyticsDailyRepository repository,
                                     AnalyticsDailyCollectorService collectorService) {
        this.repository = repository;
        this.collectorService = collectorService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyticsDailyDTO> getAll() {
        try {
            List<AnalyticsDaily> entities = repository.findAll();
            System.out.println("從資料庫查詢到 " + (entities != null ? entities.size() : 0) + " 筆 analytics_daily 記錄");
            if (entities != null && !entities.isEmpty()) {
                System.out.println("第一筆記錄範例: id=" + entities.get(0).getId() + ", date=" + entities.get(0).getDate());
            }
            List<AnalyticsDailyDTO> result = entities.stream()
                    .map(AnalyticsDailyDTO::from)
                    .collect(Collectors.toList());
            System.out.println("轉換為 DTO 後共 " + result.size() + " 筆");
            return result;
        } catch (Exception e) {
            System.err.println("查詢 analytics_daily 數據時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsDailyDTO getById(Long id) {
        return repository.findById(id)
                .map(AnalyticsDailyDTO::from)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsDailyDTO getByDate(LocalDate date) {
        try {
            System.out.println("Service.getByDate - 查詢日期: " + date);
            Optional<AnalyticsDaily> entity = repository.findByDate(date);
            System.out.println("Repository.findByDate 結果: " + (entity.isPresent() ? "找到" : "未找到"));
            if (entity.isPresent()) {
                AnalyticsDailyDTO dto = AnalyticsDailyDTO.from(entity.get());
                System.out.println("轉換為 DTO: id=" + dto.getId() + ", date=" + dto.getDate() + 
                        ", totalUsers=" + dto.getTotalUsers() + ", totalHotels=" + dto.getTotalHotels());
                return dto;
            }
            System.out.println("未找到日期 " + date + " 的數據");
            return null;
        } catch (Exception e) {
            System.err.println("查詢指定日期數據時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public AnalyticsDailyDTO save(AnalyticsDailyDTO dto) {
        AnalyticsDaily entity = dto.toEntity();
        AnalyticsDaily saved = repository.save(entity);
        return AnalyticsDailyDTO.from(saved);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public AnalyticsDailyDTO collectAndSaveDailyData(LocalDate date) {
        // 檢查該日期是否已有資料
        Optional<AnalyticsDaily> existingData = repository.findByDate(date);

        if (existingData.isPresent()) {
            // 如果已有資料，直接返回現有資料，不覆蓋
            System.out.println("日期 " + date + " 的數據已存在，跳過收集（不覆蓋）");
            return AnalyticsDailyDTO.from(existingData.get());
        }

        // 如果沒有資料，才進行收集和保存
        System.out.println("新增日期 " + date + " 的數據");
        AnalyticsDaily newData = collectorService.collectDataForDate(date);
        AnalyticsDaily saved = repository.save(newData);
        return AnalyticsDailyDTO.from(saved);
    }
}
