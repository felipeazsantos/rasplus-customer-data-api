package dev.felipeazsantos.rasplus.api.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRepresentationDto {
    private Boolean enabled;
    private String username;
    private Boolean emailVerified;
    private String email;
    private String firstName;
    private String lastName;
    private List<CredentialRepresentationDto> credentials;
    private List<String> groups;
}
