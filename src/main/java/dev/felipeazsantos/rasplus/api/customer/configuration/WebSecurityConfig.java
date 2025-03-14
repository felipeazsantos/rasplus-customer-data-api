package dev.felipeazsantos.rasplus.api.customer.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@EnableWebSecurity
@Configuration
public class WebSecurityConfig {

    private static final String[] AUTH_SWAGGER_LIST = {
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/v2/api-docs/**",
            "/swagger-resources/**"
    };

    private static final String[] ADMIN_ROLE = {"ADMIN_READ", "ADMIN_WRITE"};
    private static final String[] CLIENT_ROLE = {"CLIENT_READ_WRITE"};
    private static final String[] USER_ROLE = {"USER_READ", "USER_WRITE"};
    private static final String[] CLIENT_ADMIN_ROLES = Stream.of(CLIENT_ROLE, ADMIN_ROLE)
            .flatMap(Arrays::stream)
            .toArray(String[]::new);
    private static final String[] USER_ADMIN_ROLES = Stream.of(USER_ROLE, ADMIN_ROLE)
            .flatMap(Arrays::stream)
            .toArray(String[]::new);


    @Value("${keycloak.auth-server-uri}")
    private String keycloakUri;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(AUTH_SWAGGER_LIST).permitAll();
                    authorize.requestMatchers(HttpMethod.POST, "/v1/auth").permitAll();
                    authorize.requestMatchers("/v1/auth/**").permitAll();
                    authorize.anyRequest().hasAnyAuthority(ADMIN_ROLE);
                })
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(keycloakUri + "/realms/REALM_RASPLUS_API/protocol/openid-connect/certs").build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        Converter<Jwt, Collection<GrantedAuthority>> jwtCollectionConverter = jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");
            return roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        };

        JwtAuthenticationConverter jwt = new JwtAuthenticationConverter();
        jwt.setJwtGrantedAuthoritiesConverter(jwtCollectionConverter);
        return jwt;
    }

}
