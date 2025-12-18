package tw.com.ispan.eeit.ho_back.reason;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.com.ispan.eeit.ho_back.support.SupportRepository;

@Service
public class ReasonService {

    private final SupportRepository supportRepository;

    @Autowired
    private ReasonRepository reasonRepository;

    ReasonService(SupportRepository supportRepository) {
        this.supportRepository = supportRepository;
    }

    public List<ReasonBean> findAll() {
        return reasonRepository.findAllByOrderByCodeAsc();
    }

    public ReasonBean findById(@NonNull Integer id) {
        Optional<ReasonBean> optional = reasonRepository.findById(id);
        return optional.orElse(null);
    }

    @Transactional
    public ReasonBean create(@NonNull ReasonBean reason) {
        return reasonRepository.save(reason);
    }

    @Transactional
    public ReasonBean update(@NonNull Integer id, ReasonBean newReason) {
        Optional<ReasonBean> optional = reasonRepository.findById(id);

        if (optional.isPresent()) {
            ReasonBean existing = optional.get();
            existing.setCode(newReason.getCode());
            existing.setDescription(newReason.getDescription());
            existing.setNote(newReason.getNote());
            return reasonRepository.save(existing);
        }
        return null;
    }

    @Transactional
    public boolean deleteById(@NonNull Integer id) {
        if (reasonRepository.existsById(id)) {
            reasonRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean isReasonUsed(Integer reasonId) {
        return supportRepository.countByReason_Id(reasonId) > 0;
    }

}
