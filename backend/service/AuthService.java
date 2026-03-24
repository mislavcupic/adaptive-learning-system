package hr.algebra.backend.service;

import hr.algebra.backend.domain.entity.User;
import hr.algebra.backend.dto.request.LoginRequest;
import hr.algebra.backend.dto.request.RefreshTokenRequest;
import hr.algebra.backend.dto.request.RegisterRequest;
import hr.algebra.backend.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String accessToken, String refreshToken);

    void logoutAllDevices(User user);
}
