package hr.algebra.adaptive.learning.backend.dto.request;

import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import lombok.Data;

// UpdateRoleRequest.java
@Data
public class UpdateRoleRequest {
    private UserRole role;
}


