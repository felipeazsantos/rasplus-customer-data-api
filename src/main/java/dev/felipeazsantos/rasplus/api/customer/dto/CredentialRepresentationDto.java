package dev.felipeazsantos.rasplus.api.customer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CredentialRepresentationDto {
    private String type;
    private String value;
    private boolean temporary;
}
