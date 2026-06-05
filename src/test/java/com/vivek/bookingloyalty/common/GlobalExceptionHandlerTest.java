package com.vivek.bookingloyalty.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void optimisticLockFailure_mapsTo409Conflict() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/loyalty/me/redeem");

        ResponseEntity<ApiError> response = handler.handleOptimisticLock(
                new OptimisticLockingFailureException("stale"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().path()).isEqualTo("/api/loyalty/me/redeem");
    }
}
