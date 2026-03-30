package hr.algebra.adaptive.learning.backend.service;

import hr.algebra.adaptive.learning.backend.domain.enums.ResearchGroup;

import java.util.UUID;

public interface ResearchGroupService {
    ResearchGroup assignGroup(UUID schoolClassId);
}
