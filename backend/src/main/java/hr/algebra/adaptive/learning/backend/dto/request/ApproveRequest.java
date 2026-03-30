package hr.algebra.adaptive.learning.backend.dto.request;

import lombok.Data;
import java.util.UUID;

@Data
public class ApproveRequest {
    private UUID schoolClassId;
    private boolean includeInResearch;

}