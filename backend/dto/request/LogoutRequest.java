package hr.algebra.backend.dto.request;

import lombok.Data;

@Data
public class LogoutRequest {

    private String refreshToken;
}
