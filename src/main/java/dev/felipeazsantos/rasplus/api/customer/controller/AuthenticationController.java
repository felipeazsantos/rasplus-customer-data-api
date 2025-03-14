package dev.felipeazsantos.rasplus.api.customer.controller;

import dev.felipeazsantos.rasplus.api.customer.dto.*;
import dev.felipeazsantos.rasplus.api.customer.model.redis.UserRecoveryCode;
import dev.felipeazsantos.rasplus.api.customer.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;


    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> auth(@RequestBody @Valid LoginDto dto) {
        return ResponseEntity.status(HttpStatus.OK).body(authenticationService.auth(dto));
    }

    @PostMapping(value = "/recovery-code/send",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(value = "hasAnyAuthority('CLIENT_READ_WRITE', 'ADMIN_READ', 'ADMIN_WRITE')")
    public ResponseEntity<Void> sendRecoveryCode(@RequestBody @Valid UserRecoveryCode dto) {
        authenticationService.sendRecoveryCode(dto.getEmail());
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/recovery-code/")
    @PreAuthorize(value = "hasAnyAuthority('CLIENT_READ_WRITE', 'ADMIN_READ', 'ADMIN_WRITE')")
    public ResponseEntity<Void> recoveryCodeIsValid(@RequestParam("recoveryCode") String recoveryCode,
                                                 @RequestParam("email") String email) {
        authenticationService.recoveryCodeIsValid(recoveryCode, email);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/recovery-code/password", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(value = "hasAnyAuthority('CLIENT_READ_WRITE', 'ADMIN_READ', 'ADMIN_WRITE')")
    public ResponseEntity<Void> updatePasswordByRecoveryCode(@RequestBody @Valid UserDetailsDto dto) {
        authenticationService.updatePasswordByRecoveryCode(dto);
        return ResponseEntity.noContent().build();
    }
}
