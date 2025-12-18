package tw.com.ispan.eeit.ho_back.qcategory;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class QCategoryService {

    private QCategoryRepository qCategoryRepository;

    public QCategoryService(QCategoryRepository qCategoryRepository) {
        this.qCategoryRepository = qCategoryRepository;
    }

    /* 查全部 */
    public List<QCategoryBean> findAll() {
        return qCategoryRepository.findAllByOrderBySortOrderAsc();
    }

    /* 查一筆 */
    public QCategoryBean findById(Integer id) {
        return qCategoryRepository.findById(id).orElse(null);
    }

    /* 照類別查 */

    public List<QCategoryBean> findByCategoryName(String name) {
        return qCategoryRepository.findByNameContainingIgnoreCase(name);
    }
    /* 新增類別 */

    public QCategoryBean create(QCategoryBean qCategoryBean) {
        return qCategoryRepository.save(qCategoryBean);
    }

    /* 修改類別 */

    public QCategoryBean updateById(Integer id, QCategoryBean qCategoryBean) {
        Optional<QCategoryBean> optional = qCategoryRepository.findById(id);
        if (optional.isPresent()) {
            QCategoryBean existing = optional.get();
            existing.setName(qCategoryBean.getName());
            existing.setDescription(qCategoryBean.getDescription());
            existing.setUpdatedTime(new Date()); // 更新時間記錄當下時間
            return qCategoryRepository.save(existing);
        }
        return null;
    }

    /* 刪除類別 */

    public void deleteById(Integer id) {
        qCategoryRepository.deleteById(id);
    }

    /* 刪除驗證，若有文章使用類別不可以刪除 */
    public boolean isCategoryInUse(Integer id) {
        return qCategoryRepository.isCategoryInUse(id);
    }

    /* 排序方法 */
    public void updateSortOrder(List<QCategorySortRequest> sortList) {
        for (

        QCategorySortRequest req : sortList) {
            QCategoryBean bean = qCategoryRepository.findById(req.getId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            bean.setSortOrder(req.getSortOrder());
            qCategoryRepository.save(bean);
        }
    }
}
