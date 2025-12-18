package tw.com.ispan.eeit.ho_back.sphoto;

import java.util.List;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class SphotoService {

    private final SphotoRepository sphotoRepository;

    public SphotoService(SphotoRepository sphotoRepository) {
        this.sphotoRepository = sphotoRepository;
    }

    /* 查多筆 */
    public List<SphotoBean> findAll() {
        return sphotoRepository.findAll();
    }

    /* 查一筆 */
    public SphotoBean findById(Integer id) {
        if (id == null) {
            return null;
        }
        return sphotoRepository.findById(id).orElse(null);
    }

    /* 新增 */
    public SphotoBean create(@NonNull SphotoBean sphotoBean) {
        return sphotoRepository.save(sphotoBean);
    }

    /* 更新 */
    public SphotoBean update(@NonNull Integer id, SphotoBean updateBean) {
        Optional<SphotoBean> optional = sphotoRepository.findById(id);
        if (optional.isPresent()) {
            SphotoBean existing = optional.get();
            existing.setName(updateBean.getName());
            existing.setUrl(updateBean.getUrl());
            return sphotoRepository.save(existing);
        }
        return null;
    }

    /* 刪除 */
    public void deleteById(@NonNull Integer id) {
        sphotoRepository.deleteById(id);
    }

}
