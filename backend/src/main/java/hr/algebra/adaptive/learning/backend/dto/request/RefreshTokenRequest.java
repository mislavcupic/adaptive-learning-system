package hr.algebra.adaptive.learning.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token je obavezan")
    private String refreshToken;
}
