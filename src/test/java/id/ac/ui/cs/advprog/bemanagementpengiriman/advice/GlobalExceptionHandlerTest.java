package id.ac.ui.cs.advprog.bemanagementpengiriman.advice;

import id.ac.ui.cs.advprog.bemanagementpengiriman.model.Pengiriman;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidation_shouldReturnFieldErrors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "field", "must not be blank"));
        Method method = ModelAttributeMethodProcessor.class.getDeclaredMethods()[0];
        MethodParameter parameter = new MethodParameter(method, 0);

        var response = handler.handleValidation(new MethodArgumentNotValidException(parameter, bindingResult));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertEquals("must not be blank", body.get("field"));
    }

    @Test
    void handleSimpleExceptions_shouldMapToExpectedStatuses() {
        assertEquals(HttpStatus.BAD_REQUEST,
                handler.handleBadRequest(new IllegalArgumentException("bad")).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                handler.handleAccessDenied(new AccessDeniedException("denied")).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                handler.handleSecurity(new SecurityException("forbidden")).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,
                handler.handleConflict(new DataIntegrityViolationException("conflict",
                        new IllegalStateException("duplicate"))).getStatusCode());
        assertEquals(HttpStatus.CONFLICT,
                handler.handleOptimisticLock(
                        new ObjectOptimisticLockingFailureException(Pengiriman.class, 1L)).getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                handler.handleOther(new RuntimeException("boom")).getStatusCode());
    }
}
