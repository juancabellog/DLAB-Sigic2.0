package com.sisgic.controller;

import com.sisgic.dto.LoginRequest;
import com.sisgic.dto.LoginResponse;
import com.sisgic.entity.Role;
import com.sisgic.entity.User;
import com.sisgic.repository.RoleRepository;
import com.sisgic.repository.UserRepository;
import com.sisgic.security.JwtUtils;
import com.sisgic.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    com.sisgic.repository.RRHHRepository rrhhRepository;

    @Value("${app.version:dev}")
    private String appVersion;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new LoginResponse(jwt,
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    @PostMapping("/register")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body("Error: Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body("Error: Email is already in use!");
        }

        // Validate idRRHH if provided
        Long idRRHH = signUpRequest.getIdRRHH();
        if (idRRHH == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Error: Researcher (RRHH) is required!");
        }
        
        com.sisgic.entity.RRHH rrhh = rrhhRepository.findById(idRRHH)
                .orElseThrow(() -> new RuntimeException("Error: RRHH not found with id: " + idRRHH));
        
        // Update RRHH email with the user's email
        rrhh.setEmail(signUpRequest.getEmail());
        rrhhRepository.save(rrhh);

        // Create new user's account
        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                null, // firstName - no longer required
                null, // lastName - no longer required
                encoder.encode(signUpRequest.getPassword()),
                idRRHH);

        Set<String> strRoles = Set.of("ROLE_USER");
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);
                        break;
                    case "mod":
                        Role modRole = roleRepository.findByName("ROLE_MODERATOR")
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(modRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByName("ROLE_USER")
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken() {
        // Si llegamos aquí, el token es válido (Spring Security ya lo validó)
        Map<String, Object> body = new HashMap<>();
        body.put("status", "ok");
        body.put("version", appVersion);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/me")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<?> getCurrentUser() {
        // Obtener el usuario actual desde el contexto de seguridad
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Cargar el usuario con sus roles usando JOIN FETCH para evitar LazyInitializationException
        User user = userRepository.findByUsernameWithRoles(userPrincipal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Obtener todos los roles del usuario (puede tener múltiples roles)
        // Los roles ya están cargados gracias al JOIN FETCH
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList());

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("roles", roles); // Array con todos los roles del usuario

        return ResponseEntity.ok(userInfo);
    }

    public static class RegisterRequest {
        @jakarta.validation.constraints.NotBlank
        private String username;
        
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Email
        private String email;
        
        private String firstName; // Opcional, mantenido para compatibilidad
        
        private String lastName; // Opcional, mantenido para compatibilidad
        
        @jakarta.validation.constraints.NotBlank
        private String password;
        
        @jakarta.validation.constraints.NotNull(message = "Researcher (RRHH) is required")
        private Long idRRHH;

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public Long getIdRRHH() { return idRRHH; }
        public void setIdRRHH(Long idRRHH) { this.idRRHH = idRRHH; }
    }
}