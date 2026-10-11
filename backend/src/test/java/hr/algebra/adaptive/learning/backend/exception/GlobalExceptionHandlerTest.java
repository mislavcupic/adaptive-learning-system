package hr.algebra.adaptive.learning.backend.exception;

import hr.algebra.adaptive.learning.backend.dto.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("nepostojeci zapis vraca 404")
    void resourceNotFound() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleResourceNotFound(
                new ResourceNotFoundException("Course", "id", UUID.randomUUID().toString()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isNotBlank();
    }

    @Test
    @DisplayName("neovlasteni pristup vraca 401")
    void unauthorized() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleUnauthorized(
                new UnauthorizedException("Nemate pravo pristupa"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getMessage()).isEqualTo("Nemate pravo pristupa");
    }

    @Test
    @DisplayName("kriva lozinka vraca 401 bez otkrivanja detalja")
    void badCredentials() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleBadCredentials(
                new BadCredentialsException("User ines@test.hr not found in database"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getMessage()).isEqualTo("Pogrešan email ili lozinka");
        assertThat(response.getBody().getMessage()).doesNotContain("ines@test.hr");
    }

    @Test
    @DisplayName("neispravan argument vraca 400")
    void illegalArgument() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgument(
                new IllegalArgumentException("No enum constant UserRole.SUPERVISOR"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("SUPERVISOR");
    }

    @Test
    @DisplayName("ponovno rjesavanje provjere vraca 409")
    void assessmentAlreadyCompleted() {
        ResponseEntity<ApiResponse<Object>> response = handler.handleAssessmentAlreadyCompleted(
                new AssessmentAlreadyCompletedException("Provjera je vec rijesena"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getMessage()).isEqualTo("Provjera je vec rijesena");
    }

    @Test
    @DisplayName("neocekivana greska vraca 500 bez tehnickih detalja")
    void genericException() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleGenericException(
                new NullPointerException("Cannot invoke \"User.getEmail()\" because \"user\" is null"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("Došlo je do greške na serveru");
        assertThat(response.getBody().getMessage()).doesNotContain("NullPointer");
    }

    @Test
    @DisplayName("pogreske validacije vracaju 400 s popisom polja")
    void validationErrors() throws Exception {
        BindingResult binding = new BeanPropertyBindingResult(new Object(), "taskRequest");
        binding.addError(new FieldError("taskRequest", "title", "Naslov je obavezan"));
        binding.addError(new FieldError("taskRequest", "maxScore", "Bodovi moraju biti pozitivni"));

        ResponseEntity<ApiResponse<Map<String, String>>> response =
                handler.handleValidationErrors(exceptionFor(binding));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("Greška u validaciji");
        assertThat(response.getBody().getData())
                .containsEntry("title", "Naslov je obavezan")
                .containsEntry("maxScore", "Bodovi moraju biti pozitivni");
    }

    @Test
    @DisplayName("bez pogresaka validacije popis polja je prazan")
    void emptyValidationErrors() throws Exception {
        BindingResult binding = new BeanPropertyBindingResult(new Object(), "taskRequest");

        ResponseEntity<ApiResponse<Map<String, String>>> response =
                handler.handleValidationErrors(exceptionFor(binding));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getData()).isEmpty();
    }

    private MethodArgumentNotValidException exceptionFor(BindingResult binding) throws Exception {
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummy", String.class), 0);
        return new MethodArgumentNotValidException(parameter, binding);
    }

    @SuppressWarnings("unused")
    private void dummy(String argument) {
        // Sluzi samo kao MethodParameter za konstruktor iznimke.
    }
}