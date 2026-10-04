package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.enums.ResearchGroup;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
import hr.algebra.adaptive.learning.backend.service.ResearchGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;

/**
 * Dodjela istrazivacke skupine blokovskom randomizacijom.
 *
 * Kad su skupine izjednacene, odluka je slucajna. Cim jedna odmakne,
 * sljedeci student ide u manju. Time se cuva ravnoteza brojeva, a
 * raspored ostaje nepredvidljiv - za razliku od naizmjenicne dodjele,
 * gdje se iz redoslijeda odobravanja moze unaprijed znati tko gdje ide.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResearchGroupServiceImpl implements ResearchGroupService {

    private final UserRepository userRepository;

    private final Random random = new Random();

    @Override
    public ResearchGroup assignGroup(UUID schoolClassId) {
        long experimental = userRepository.countBySchoolClassIdAndResearchGroup(
                schoolClassId, ResearchGroup.EXPERIMENTAL);
        long control = userRepository.countBySchoolClassIdAndResearchGroup(
                schoolClassId, ResearchGroup.CONTROL);

        ResearchGroup assigned;

        if (experimental > control) {
            assigned = ResearchGroup.CONTROL;
        } else if (control > experimental) {
            assigned = ResearchGroup.EXPERIMENTAL;
        } else {
            assigned = random.nextBoolean()
                    ? ResearchGroup.EXPERIMENTAL
                    : ResearchGroup.CONTROL;
        }

        log.info("Razred {}: E={}, K={} -> dodijeljeno {}",
                schoolClassId, experimental, control, assigned);

        return assigned;
    }
}