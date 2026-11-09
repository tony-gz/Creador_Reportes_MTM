package com.simreports.seguridad;

import com.simreports.usuarios.UsuarioService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;

import java.time.Clock;

/**
 * Inicio de sesión y permisos — Fase 4 punto 2 (spec 12, D-024 y D-026).
 *
 * <ul>
 *   <li>Sin sesión solo se ve la pantalla de entrada, sus estilos y scripts
 *       (el código es público, el repo también) y {@code /salud}.</li>
 *   <li>{@code POST /api/sesion/entrar} (campos {@code usuario} y
 *       {@code password}) y {@code POST /api/sesion/salir}, ambos con CSRF.</li>
 *   <li>CSRF para páginas con JavaScript: cookie {@code XSRF-TOKEN} que el
 *       cliente devuelve en la cabecera {@code X-XSRF-TOKEN}.</li>
 *   <li>{@code /api/admin/**} solo para el superadmin.</li>
 *   <li>Política de contenido estricta: solo scripts y estilos propios.</li>
 * </ul>
 */
@Configuration
public class ConfiguracionSeguridad {

    static final String POLITICA_CONTENIDO = "default-src 'self'; script-src 'self'; style-src 'self'; "
            + "img-src 'self' data:; connect-src 'self'; form-action 'self'; frame-ancestors 'none'; "
            + "base-uri 'none'; object-src 'none'";

    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, RespuestasSeguridad respuestas,
                                                 UsuarioService usuarios) throws Exception {
        http
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/entrar.html", "/css/**", "/js/**", "/favicon.ico", "/salud",
                                "/api/sesion/csrf", "/error").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("SUPERADMIN")
                        .anyRequest().authenticated())
                .formLogin(f -> f
                        .loginPage("/entrar.html")
                        .loginProcessingUrl("/api/sesion/entrar")
                        .usernameParameter("usuario")
                        .passwordParameter("password")
                        .successHandler(respuestas::entradaCorrecta)
                        .failureHandler(respuestas::entradaFallida)
                        .permitAll())
                .logout(l -> l
                        .logoutUrl("/api/sesion/salir")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .csrf(c -> c.spa())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(respuestas::sinSesion)
                        .accessDeniedHandler(respuestas::accesoDenegado))
                .requestCache(r -> r.disable())
                .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives(POLITICA_CONTENIDO)))
                .addFilterBefore(new UsuarioVigenteFiltro(usuarios, respuestas), AuthorizationFilter.class);
        return http.build();
    }
}
