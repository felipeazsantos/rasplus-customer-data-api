package dev.felipeazsantos.rasplus.api.customer.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import dev.felipeazsantos.rasplus.api.customer.dto.LoginDto;
import dev.felipeazsantos.rasplus.api.customer.dto.UserDetailsDto;
import dev.felipeazsantos.rasplus.api.customer.dto.UserRepresentationDto;

public interface AuthenticationService {

    String auth(LoginDto dto);

    void sendRecoveryCode(String email);

    boolean recoveryCodeIsValid(String recoveryCode, String email);

    void updatePasswordByRecoveryCode(UserDetailsDto userDetailsDto);

    void createAuthUser(UserRepresentationDto userRepresentationDto);

    void updateAuthUser(UserRepresentationDto userRepresentationDto, String email);
}
