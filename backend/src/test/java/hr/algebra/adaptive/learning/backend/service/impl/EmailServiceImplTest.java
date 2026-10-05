package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmailServiceImpl")
class EmailServiceImplTest {

    @Mock private JavaMailSender mailSender;
    @Mock private MimeMessage mimeMessage;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("ana@test.hr")
                .firstName("Ana")
                .lastName("Anić")
                .role(UserRole.STUDENT)
                .build();
        user.setId(UUID.randomUUID());

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Nested
    @DisplayName("Slanje isključeno")
    class Disabled {

        @Test
        @DisplayName("ne šalje poruku za potvrdu adrese")
        void skipsVerificationEmail() {
            EmailServiceImpl service = service(false);

            service.sendVerificationEmail(user, "token123");

            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("ne šalje obavijest o odobrenju računa")
        void skipsApprovalEmail() {
            EmailServiceImpl service = service(false);

            service.sendAccountApprovedEmail(user);

            verify(mailSender, never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("ne baca iznimku kad je slanje isključeno")
        void doesNotThrow() {
            EmailServiceImpl service = service(false);

            assertThatCode(() -> service.sendVerificationEmail(user, "token"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Slanje uključeno")
    class Enabled {

        @Test
        @DisplayName("šalje poruku za potvrdu adrese")
        void sendsVerificationEmail() {
            EmailServiceImpl service = service(true);

            service.sendVerificationEmail(user, "token123");

            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("šalje obavijest o odobrenju računa")
        void sendsApprovalEmail() {
            EmailServiceImpl service = service(true);

            service.sendAccountApprovedEmail(user);

            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("neuspjeh slanja ne ruši poziv")
        void survivesSendFailure() {
            EmailServiceImpl service = service(true);
            doThrow(new RuntimeException("SMTP nedostupan"))
                    .when(mailSender).send(any(MimeMessage.class));

            assertThatCode(() -> service.sendVerificationEmail(user, "token"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("neuspjeh pri odobrenju ne ruši poziv")
        void survivesApprovalFailure() {
            EmailServiceImpl service = service(true);
            doThrow(new RuntimeException("SMTP nedostupan"))
                    .when(mailSender).send(any(MimeMessage.class));

            assertThatCode(() -> service.sendAccountApprovedEmail(user))
                    .doesNotThrowAnyException();
        }
    }

    private EmailServiceImpl service(boolean enabled) {
        return new EmailServiceImpl(
                mailSender,
                "noreply@adaptivelearn.hr",
                "http://localhost:3000",
                enabled);
    }
}
