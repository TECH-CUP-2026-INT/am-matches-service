package co.edu.escuelaing.techcup.match.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.escuelaing.techcup.match.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/partidos/123");
    }

    @Test
    void handleNotFound_returns404() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                new MatchNotFoundException(UUID.randomUUID()), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().path()).isEqualTo("/api/partidos/123");
    }

    @Test
    void handleMatchAccessDenied_returns403() {
        ResponseEntity<ErrorResponse> response = handler.handleMatchAccessDenied(
                new MatchAccessDeniedException(UUID.randomUUID()), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleConflict_returns409ForInvalidMatchState() {
        ResponseEntity<ErrorResponse> response = handler.handleConflict(
                new InvalidMatchStateException("no en curso"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("no en curso");
    }

    @Test
    void handleConflict_returns409ForMatchSheetAlreadyExists() {
        ResponseEntity<ErrorResponse> response = handler.handleConflict(
                new MatchSheetAlreadyExistsException(UUID.randomUUID()), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleConflict_returns409ForPenaltyShootoutRequired() {
        ResponseEntity<ErrorResponse> response = handler.handleConflict(
                new PenaltyShootoutRequiredException("se requieren penales"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleInvalidTeam_returns400() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalidTeam(
                new InvalidTeamException(UUID.randomUUID(), UUID.randomUUID()), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleValidation_joinsFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("request", "minute", "no puede ser negativo");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("minute: no puede ser negativo");
    }

    @Test
    void handleBusinessValidation_returns400() {
        ResponseEntity<ErrorResponse> response = handler.handleBusinessValidation(
                new ValidationException("dato inválido"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("dato inválido");
    }

    @Test
    void handleUnauthenticated_returns401() {
        ResponseEntity<ErrorResponse> response = handler.handleUnauthenticated(
                new InsufficientAuthenticationException("no autenticado"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleForbidden_returns403WithGenericMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleForbidden(
                new AccessDeniedException("denegado"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().message()).isEqualTo("No tiene permisos suficientes para esta acción");
    }

    @Test
    void handleUnexpected_returns500() {
        when(request.getMethod()).thenReturn("GET");
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new RuntimeException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("Ocurrió un error inesperado");
    }
}
