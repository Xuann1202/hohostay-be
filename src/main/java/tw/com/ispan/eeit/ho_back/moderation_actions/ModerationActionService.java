package tw.com.ispan.eeit.ho_back.moderation_actions;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;

public interface ModerationActionService {
    Page<ModerationAction> list(Pageable pageable);

    List<ModerationAction> listAll(Sort sort);

    ModerationAction get(Long id);

    ModerationAction create(ModerationAction ma);

    ModerationAction update(Long id, ModerationAction ma);

    void delete(Long id);
}
