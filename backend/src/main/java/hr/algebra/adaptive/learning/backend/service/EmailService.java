package hr.algebra.adaptive.learning.backend.service;

import hr.algebra.adaptive.learning.backend.domain.entity.User;

public interface EmailService {

    void sendVerificationEmail(User user, String token);

    void sendAccountApprovedEmail(User user);
}
