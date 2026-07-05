package hr.algebra.adaptive.learning.backend.service;

import hr.algebra.adaptive.learning.backend.dto.response.ResearchResultResponse;
import java.util.List;

public interface ResearchResultService {
    List<ResearchResultResponse> getResults();
}
