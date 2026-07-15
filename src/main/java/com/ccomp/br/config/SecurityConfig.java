package com.ccomp.br.config;

import com.ccomp.br.domain.users.security.CustomAuthEntryPoint;
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
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password4j.Argon2Password4jPasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomAuthEntryPoint authEntryPoint;

    @Value("${jwt.private.key}")
    private RSAPrivateKey privateKey;

    @Value("${jwt.public.key}")
    private RSAPublicKey publicKey;

    @Value("${swagger.security.enabled:true}")
    private boolean swaggerSecurityEnabled;

    @Value("${swagger.username}")
    private String swaggerUsername;

    @Value("${swagger.password}")
    private String swaggerPassword;

    private static final String[] PUBLIC_AUTH_ROUTES = {
            "/api/auth/**"
    };

    private static final String[] PUBLIC_EVENT_ROUTES = {
            "/api/events/{eventId:\\d+}"
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

//    @Bean
//    @Order(1)
//    public SecurityFilterChain swaggerFilterChain(HttpSecurity http) throws Exception {
//        http.securityMatcher(SWAGGER_ROUTES)
//                .csrf(AbstractHttpConfigurer::disable)
//                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
//
//        if (swaggerSecurityEnabled) {
//            // Cria um AuthenticationManager isolado, só para o Swagger.
//            // NÃO usa o AuthenticationManagerBuilder compartilhado do HttpSecurity,
//            // evitando conflito com o AuthenticationManager global usado no login.
//            DaoAuthenticationProvider swaggerProvider = new DaoAuthenticationProvider(swaggerUserDetailsService());
//            swaggerProvider.setPasswordEncoder(passwordEncoder());
//
//            AuthenticationManager swaggerAuthManager = new ProviderManager(swaggerProvider);
//
//            http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
//                    .authenticationManager(swaggerAuthManager) // <- isolado, não é o bean global
//                    .httpBasic(basic -> {});
//        } else {
//            http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
//        }
//
//        return http.build();
//    }

//    @Bean
//    public UserDetailsService swaggerUserDetailsService() {
//        var user = User.withUsername(swaggerUsername)
//                .password(passwordEncoder().encode(swaggerPassword))
//                .roles("SWAGGER")
//                .build();
//        return new InMemoryUserDetailsManager(user);
//    }

    @Bean
//    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                            // Auth
                            authorize.requestMatchers(PUBLIC_AUTH_ROUTES).permitAll();
                            // Event
                            authorize.requestMatchers(HttpMethod.GET, PUBLIC_EVENT_ROUTES).permitAll();
                            // News
                            authorize.requestMatchers(HttpMethod.GET, PUBLIC_NEWS_ROUTES).permitAll();
                            // Documentação
                            authorize.requestMatchers(SWAGGER_ROUTES).permitAll();

                            authorize.anyRequest().authenticated();
                        }

                )
                .oauth2ResourceServer(
                        oauth2 -> {
                            oauth2.jwt(jwt -> jwt.decoder(jwtDecoder()));
                            oauth2.authenticationEntryPoint(authEntryPoint);
                        }

                ).build();
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
        log.info("Public key: {}",
                Base64.getEncoder().encodeToString(publicKey.getEncoded()).substring(0, 20));
    }
}
