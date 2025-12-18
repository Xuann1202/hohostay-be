package tw.com.ispan.eeit.ho_back.support_search;

import java.util.Date;

public class SupportSearchRequest {
    private String userId;
    private String username;
    private String caseCode;
    private String status;
    private String categoryId;
    private String reasonId;
    private String closedBy;
    private Date startDate;
    private Date endDate;
    private Date closeStart; // 案件結案開始時間
    private Date closeEnd;

    // -------- Getter / Setter --------

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCaseCode() {
        return caseCode;
    }

    public void setCaseCode(String caseCode) {
        this.caseCode = caseCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getReasonId() {
        return reasonId;
    }

    public void setReasonId(String reasonId) {
        this.reasonId = reasonId;
    }

    public String getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(String closedBy) {
        this.closedBy = closedBy;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getCloseStart() {
        return closeStart;
    }

    public void setCloseStart(Date closeStart) {
        this.closeStart = closeStart;
    }

    public Date getCloseEnd() {
        return closeEnd;
    }

    public void setCloseEnd(Date closeEnd) {
        this.closeEnd = closeEnd;
    }

}
