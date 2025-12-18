package tw.com.ispan.eeit.ho_back.analytics_daily;

import java.time.LocalDate;
import java.util.List;

public interface AnalyticsDailyService {
    List<AnalyticsDailyDTO> getAll();

    AnalyticsDailyDTO getById(Long id);

    AnalyticsDailyDTO getByDate(LocalDate date);

    AnalyticsDailyDTO save(AnalyticsDailyDTO dto);

    void delete(Long id);

    // 排程相關
    AnalyticsDailyDTO collectAndSaveDailyData(LocalDate date);
}
