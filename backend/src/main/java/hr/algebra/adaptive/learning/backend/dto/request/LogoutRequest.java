package hr.algebra.adaptive.learning.backend.dto.request;

import lombok.Data;

@Data
public class LogoutRequest {

    private String refreshToken;
}
