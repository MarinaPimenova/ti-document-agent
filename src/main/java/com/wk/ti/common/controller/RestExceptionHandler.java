package com.wk.ti.common.controller;

import com.wk.ti.exception.NotAuthorizedException;
import com.wk.ti.exception.model.ClientErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({NotAuthorizedException.class})
    public ResponseEntity<ClientErrorResponse> handleNotAuthorizedException(Exception ex) {
        log.error(ex.getMessage());
        ClientErrorResponse errorResponse =
                buildErrorMessage("not_authorized", ex.getMessage(), HttpStatus.UNAUTHORIZED.value());

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ClientErrorResponse> handleUncaughtException(
            Throwable ex, HttpServletRequest request,
            @AuthenticationPrincipal OidcUser user) {
        log.error(getStacktrace(ex));

        String message = processRequest(request, ex.getMessage(), user);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorMessage("error", message, 500));
    }

    private ClientErrorResponse buildErrorMessage(String error, String message, int status) {
        return ClientErrorResponse.builder()
                .error(error)
                .message(message)
                .status(status)
                .build();
    }

    private String processRequest(HttpServletRequest request,
                                  String message,
                                  OidcUser user) {
        String requestURL = request.getRequestURL().toString();
        String userEmail = "UNKNOWN";
        if (user != null) {
            userEmail = user.getEmail();
        }

        return String.format("URL: %s, email: %s, Message: %s", requestURL, userEmail, message);
    }

    protected String getStacktrace(Throwable ex) {
        String stacktrace = ExceptionUtils.getStackTrace(ex);

        if (!(ex instanceof NullPointerException) && ex.getCause() != null) {
            stacktrace =
                    stacktrace.isBlank()
                            ? ExceptionUtils.getStackTrace(ex.getCause())
                            : stacktrace + "\n\n\n" + ExceptionUtils.getStackTrace(ex.getCause());
        }
        return stacktrace;
    }

}
