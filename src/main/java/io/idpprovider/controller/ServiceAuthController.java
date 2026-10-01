package io.idpprovider.controller;

import io.idpprovider.dto.request.ServiceAuthRequest;
import io.idpprovider.dto.response.ServiceAuthResponse;
import io.idpprovider.service.ServiceAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/auth/service")
@RequiredArgsConstructor
public class ServiceAuthController {

    private final ServiceAuthenticationService serviceAuthenticationService;

    @PostMapping("/authenticate")
    public ResponseEntity<ServiceAuthResponse> authenticate(@RequestBody final ServiceAuthRequest request) {
        return ResponseEntity.ok(serviceAuthenticationService.authenticate(request.serviceName(), request.clientSecret()));
    }
}