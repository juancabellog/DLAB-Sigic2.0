package com.sisgic.service;

import com.sisgic.entity.User;
import com.sisgic.repository.UserRepository;
import com.sisgic.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Obtiene el idRRHH del usuario actualmente autenticado
     * @return Optional con el idRRHH del usuario, o Optional.empty() si no está autenticado o no tiene idRRHH
     */
    public Optional<Long> getCurrentUserIdRRHH() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        
        Object principal = authentication.getPrincipal();
        Optional<User> user = Optional.empty();
        
        if (principal instanceof UserPrincipal) {
            // Si el principal es UserPrincipal, usar el ID directamente
            UserPrincipal userPrincipal = (UserPrincipal) principal;
            Long userId = userPrincipal.getId();
            user = userRepository.findById(userId);
        } else if (principal instanceof String) {
            // Si el principal es un String (username), buscar por username
            String username = (String) principal;
            user = userRepository.findByUsername(username);
        } else {
            return Optional.empty();
        }
        
        if (user.isPresent()) {
            User u = user.get();
            Long idRRHH = u.getIdRRHH();
            // Retornar el idRRHH incluso si es null (la función acepta null)
            return Optional.ofNullable(idRRHH);
        }
        
        return Optional.empty();
    }
    
    /**
     * Obtiene el usuario actualmente autenticado
     * @return Optional con el usuario, o Optional.empty() si no está autenticado
     */
    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserPrincipal) {
            // Si el principal es UserPrincipal, usar el ID directamente
            UserPrincipal userPrincipal = (UserPrincipal) principal;
            Long userId = userPrincipal.getId();
            return userRepository.findById(userId);
        } else if (principal instanceof String) {
            // Si el principal es un String (username), buscar por username
            String username = (String) principal;
            return userRepository.findByUsername(username);
        }
        
        return Optional.empty();
    }
    
    /**
     * Obtiene el username del usuario actualmente autenticado
     * @return Optional con el username del usuario, o Optional.empty() si no está autenticado
     */
    public Optional<String> getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) principal;
            return Optional.of(userPrincipal.getUsername());
        } else if (principal instanceof String) {
            String username = (String) principal;
            return Optional.of(username);
        }
        
        return Optional.empty();
    }
}

