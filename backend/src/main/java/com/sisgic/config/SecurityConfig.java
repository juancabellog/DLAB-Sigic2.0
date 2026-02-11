package com.sisgic.config;

import com.sisgic.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Order(2) // Prioridad menor que H2ConsoleConfig
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth ->
                auth.requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/health").permitAll() // Health check accesible
                    .requestMatchers("/api/catalogs/**").permitAll() // Catálogos accesibles
                    .requestMatchers("/api/projects/**").permitAll() // Proyectos accesibles
                    .requestMatchers("/api/publications/**").permitAll() // Publicaciones accesibles
                    .requestMatchers("/api/journals/**").permitAll() // Revistas accesibles
                    .requestMatchers("/api/researchers/**").permitAll() // Investigadores accesibles
                    .requestMatchers("/api/scientific-events/**").permitAll() // Organizaciones de eventos científicos accesibles
                    .requestMatchers("/api/thesis/**").permitAll() // Tesis accesibles
                    .requestMatchers("/api/technology-transfer/**").permitAll() // Transferencia tecnológica accesible
                    .requestMatchers("/api/postdoctoral-fellows/**").permitAll() // Becarios postdoctorales accesibles
                    .requestMatchers("/api/outreach-activities/**").permitAll() // Actividades de difusión accesibles
                    .requestMatchers("/api/scientific-collaborations/**").permitAll() // Colaboraciones científicas accesibles
                    .requestMatchers("/api/thesis-students/**").permitAll() // Estudiantes de tesis accesibles
                    .requestMatchers("/api/files/**").permitAll() // Upload de archivos accesible
                    .requestMatchers("/api/admin/cache/**").permitAll() // Endpoints de administración de caché accesibles (para app antigua)
                    .requestMatchers("/pdfs/**").permitAll() // PDFs accesibles
                    .requestMatchers("/h2-console/**").permitAll()
                    .requestMatchers("/error").permitAll()
                    .anyRequest().authenticated()
            )
            .headers(headers -> headers
                .frameOptions().sameOrigin() // Permite iframe para H2 Console
            );
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
