package hr.algebra.adaptive.learning.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email je obavezan")
    @Email(message = "Neispravan format emaila")
    private String email;

    @NotBlank(message = "Lozinka je obavezna")
    private String password;
}
