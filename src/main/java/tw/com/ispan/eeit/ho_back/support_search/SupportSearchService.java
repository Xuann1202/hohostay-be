package tw.com.ispan.eeit.ho_back.support_search;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import tw.com.ispan.eeit.ho_back.support.SupportBean;

@Service
public class SupportSearchService {

    @Autowired
    private SupportSearchRepository repo;

    @Autowired
    private SupportReportExcelBuilder excelBuilder;

    public List<SupportBean> search(SupportSearchRequest req) {
        return repo.search(req);
    }

    public byte[] exportExcel(SupportSearchRequest req) {
        List<SupportBean> data = repo.search(req);
        return excelBuilder.buildExcel(data);
    }

}