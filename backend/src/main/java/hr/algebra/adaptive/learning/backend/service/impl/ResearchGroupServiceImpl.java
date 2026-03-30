package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.enums.ResearchGroup;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
import hr.algebra.adaptive.learning.backend.service.ResearchGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResearchGroupServiceImpl implements ResearchGroupService {

    private final UserRepository userRepository;

    @Override
    public ResearchGroup assignGroup(UUID schoolClassId) {
        long experimental = userRepository.countBySchoolClassIdAndResearchGroup(
                schoolClassId, ResearchGroup.EXPERIMENTAL);
        long control = userRepository.countBySchoolClassIdAndResearchGroup(
                schoolClassId, ResearchGroup.CONTROL);

        if (experimental <= control) return ResearchGroup.EXPERIMENTAL;
        return ResearchGroup.CONTROL;
    }
}
