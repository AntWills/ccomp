package com.ccomp.br.domain.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password4j.Argon2Password4jPasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomAuthEntryPoint authEntryPoint;

    @Value("${spring.profiles.active}")
    private String profileActive;

    @Value("${jwt.private.key}")
    private RSAPrivateKey privateKey;

    @Value("${jwt.public.key}")
    private RSAPublicKey publicKey;

    private static final String[] PUBLIC_AUTH_ROUTES = {
            "/api/auth/**"
    };

    private static final String[] PUBLIC_EVENT_ROUTES = {
            "/api/events/{eventId:\\d+}",
            "/api/events"
    };

    private static final String[] PUBLIC_NEWS_ROUTES = {
            "/api/news/{slug}",
            "/api/news"
    };

    private static final String[] SWAGGER_ROUTES = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/favicon.ico",
            "/v3/api-docs/**",
            "/v3/api-docs"
    };

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerFilterChain(HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
        DaoAuthenticationProvider swaggerProvider = new DaoAuthenticationProvider(userDetailsService);
        swaggerProvider.setPasswordEncoder(this.passwordEncoder());
        AuthenticationManager swaggerAuthManager = new ProviderManager(swaggerProvider);

        http.securityMatcher(SWAGGER_ROUTES)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .authenticationManager(swaggerAuthManager)
                .httpBasic(Customizer.withDefaults());


        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                            authorize.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                            authorize.requestMatchers("/error").permitAll();
                            // Auth
                            authorize.requestMatchers(PUBLIC_AUTH_ROUTES).permitAll();
                            // Event
                            authorize.requestMatchers(HttpMethod.GET, PUBLIC_EVENT_ROUTES).permitAll();
                            authorize.requestMatchers(HttpMethod.POST, "/api/events/search").permitAll();
                            // News
                            authorize.requestMatchers(HttpMethod.GET, PUBLIC_NEWS_ROUTES).permitAll();
                            authorize.requestMatchers(HttpMethod.POST, "/api/news/search").permitAll();
                            // Documentação
//                            authorize.requestMatchers(SWAGGER_ROUTES).permitAll();

                            authorize.anyRequest().authenticated();
                        }

                )
                .oauth2ResourceServer(
                        oauth2 -> {
                            oauth2.jwt(jwt -> jwt
                                    .decoder(jwtDecoder())
                                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                            );
                            oauth2.authenticationEntryPoint(authEntryPoint);
                        }

                ).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // Diz ao Spring para ler a claim "roles" em vez de "scope"
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        // Como o valor já é "ROLE_USER", deixamos o prefixo vazio (para não virar "ROLE_ROLE_USER")
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:8080",
                "http://localhost:5173",
                "http://localhost:4200"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2Password4jPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        JWK jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        var jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }

    @PostConstruct
    public void checkKeys() {
        if(profileActive.equals("dev"))
            log.info("Public key: {}",
                Base64.getEncoder().encodeToString(publicKey.getEncoded()).substring(0, 20));
    }
}
