package tw.com.ispan.eeit.ho_back.support_search;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tw.com.ispan.eeit.ho_back.support.SupportBean;

@RestController
@RequestMapping("/api/report/support")
public class SupportSearchController {

    @Autowired
    private SupportSearchService service;

    @PostMapping("/search")
    public List<SupportBean> search(@RequestBody SupportSearchRequest req) {
        return service.search(req);
    }

    @PostMapping("/export")
    public ResponseEntity<?> exportExcel(@RequestBody SupportSearchRequest req) {
        byte[] file = service.exportExcel(req);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Support_Report.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .body(file);
    }

}
