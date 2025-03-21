package dev.felipeazsantos.rasplus.api.customer.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.felipeazsantos.rasplus.api.customer.component.HttpComponent;
import dev.felipeazsantos.rasplus.api.customer.dto.*;
import dev.felipeazsantos.rasplus.api.customer.exception.BadRequestException;
import dev.felipeazsantos.rasplus.api.customer.exception.NotFoudException;
import dev.felipeazsantos.rasplus.api.customer.model.redis.UserRecoveryCode;
import dev.felipeazsantos.rasplus.api.customer.repository.redis.UserRecoveryCodeRepository;
import dev.felipeazsantos.rasplus.api.customer.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String PASSWORD = "password";

    @Value("${keycloak.auth-server-uri}")
    private String keycloakUri;

    @Value("${webservices.rasplus.redis.recoverycode.timeout}")
    private String recoveryCodeTimeout;

    @Value("${keycloak.credentials.client-id}")
    private String clientId;

    @Value("${keycloak.credentials.client-secret}")
    private String clientSecret;

    @Value("${keycloak.credentials.authorization-grant-type}")
    private String grantType;

    @Autowired
    private UserRecoveryCodeRepository userRecoveryCodeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HttpComponent httpComponent;


    @Override
    public String auth(LoginDto dto) {
        try {
            MultiValueMap<String, String> keycloakOAuth = KeycloakOAuthDto.builder()
                    .clientId(dto.getClientId())
                    .clientSecret(dto.getClientSecret())
                    .grantType(dto.getGrantType())
                    .username(dto.getUsername())
                    .password(dto.getPassword())
                    .refreshToken(dto.getRefreshToken())
                    .build();

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(keycloakOAuth, httpComponent.httpHeaders());
            ResponseEntity<String> response = httpComponent.restTemplate().postForEntity(
                    keycloakUri + "/realms/REALM_RASPLUS_API/protocol/openid-connect/token", request, String.class
            );
            return response.getBody();
        } catch (Exception e) {
            throw new BadRequestException("Erro ao formatar token - "+e.getMessage());
        }
    }

    @Override
    public void sendRecoveryCode(String email){

        UserRecoveryCode userRecoveryCode;
        String code = String.format("%04d", new Random().nextInt(10000));
        var userRecoveryCodeOpt = userRecoveryCodeRepository.findByEmail(email);

        if (userRecoveryCodeOpt.isEmpty()) {
            try {
                getUserAuthId(email, getHttpHeaders());
            } catch (BadRequestException | JsonProcessingException ex) {
                throw new NotFoudException("User auth credentials not found");
            }

            userRecoveryCode = new UserRecoveryCode();
            userRecoveryCode.setEmail(email);

        } else {
            userRecoveryCode = userRecoveryCodeOpt.get();
        }
        userRecoveryCode.setCode(code);
        userRecoveryCode.setCreationDate(LocalDateTime.now());

        userRecoveryCodeRepository.save(userRecoveryCode);
//        mailIntegration.send(email, "Código de recuperação de conta: "+code, "Código de recuperação de conta");
    }

    @Override
    public boolean recoveryCodeIsValid(String recoveryCode, String email) {

        var userRecoveryCodeOpt = userRecoveryCodeRepository.findByEmail(email);

        if (userRecoveryCodeOpt.isEmpty()) {
            throw new NotFoudException("Usuário não encontrado");
        }

        UserRecoveryCode userRecoveryCode = userRecoveryCodeOpt.get();

        LocalDateTime timeout = userRecoveryCode.getCreationDate().plusMinutes(Long.parseLong(recoveryCodeTimeout));
        LocalDateTime now = LocalDateTime.now();

        if (!(recoveryCode.equals(userRecoveryCode.getCode()) && now.isBefore(timeout))) {
            throw new BadRequestException("Token is invalid or expired");
        }

        return true;
    }

    @Override
    public void updatePasswordByRecoveryCode(UserDetailsDto userDetailsDto) {
        if (recoveryCodeIsValid(userDetailsDto.getRecoveryCode(), userDetailsDto.getEmail())) {
            var userRepresentationDto = getUserRepresentationDtoUpdated(userDetailsDto.getPassword());
            updateAuthUser(userRepresentationDto, userRepresentationDto.getEmail());
        }
    }

    @Override
    public void createAuthUser(UserRepresentationDto userRepresentationDto) {
        try {
            HttpHeaders httpHeaders = getHttpHeaders();
            HttpEntity<UserRepresentationDto> request = new HttpEntity<>(userRepresentationDto, httpHeaders);
            httpComponent.restTemplate().postForEntity(
                    keycloakUri + "/admin/realms/REALM_RASPLUS_API/users",
                    request,
                    String.class
            );
        } catch (Exception ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    @Override
    public void updateAuthUser(UserRepresentationDto userRepresentationDto, String currentEmail) {
        try {
            HttpHeaders httpHeaders = getHttpHeaders();
            String userId = getUserAuthId(currentEmail, httpHeaders);
            HttpEntity<UserRepresentationDto> request = new HttpEntity<>(userRepresentationDto, httpHeaders);
            httpComponent.restTemplate().put(
                    keycloakUri + "/admin/realms/REALM_RASPLUS_API/users/{id}",
                    request,
                    userId
            );
        } catch (Exception ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    private String getUserAuthId(String currentEmail, HttpHeaders httpHeaders) throws JsonProcessingException {
        HttpEntity<UserRepresentationDto> request = new HttpEntity<>(httpHeaders);
        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("email", currentEmail);
        uriVariables.put("exact", "true");
        String responseGetUser = httpComponent.restTemplate().exchange(
                keycloakUri + "/admin/realms/REALM_RASPLUS_API/users?email={email}&exact={exact}",
                HttpMethod.GET,
                request,
                String.class,
                uriVariables
        ).getBody();

        List<Map<String, Object>> users = objectMapper.readValue(responseGetUser, new TypeReference<List<Map<String, Object>>>() {
        });
        if (users.isEmpty()) {
            throw new BadRequestException("Error to update user");
        }
        return users.get(0).get("id").toString();
    }

    private String getAdminCliAccessToken() throws JsonProcessingException {
        LoginDto loginDto = new LoginDto();
        loginDto.setClientId(clientId);
        loginDto.setClientSecret(clientSecret);
        loginDto.setGrantType(grantType);
        Map<String, String> clientCredentialsResponse = objectMapper.readValue(auth(loginDto), Map.class);
        return clientCredentialsResponse.get("access_token");
    }

    private HttpHeaders getHttpHeaders() throws JsonProcessingException {
        String accessToken = getAdminCliAccessToken();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(accessToken);
        return httpHeaders;
    }

    private UserRepresentationDto getUserRepresentationDtoUpdated(String newPassword) {
        CredentialRepresentationDto credentialRepresentationDto = CredentialRepresentationDto.builder()
                .temporary(false)
                .value(newPassword)
                .type("password")
                .build();

        return UserRepresentationDto.builder()
                .enabled(true)
                .credentials(List.of(credentialRepresentationDto))
                .build();
    }

}
