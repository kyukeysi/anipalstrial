package com.anipals.backend.auth.service;

import com.anipals.backend.auth.dto.AuthResponse;
import com.anipals.backend.auth.dto.LoginRequest;
import com.anipals.backend.auth.dto.RegisterRequest;
import com.anipals.backend.game.entity.PlayerGameState;
import com.anipals.backend.game.service.GameService;
import com.anipals.backend.user.entity.User;
import com.anipals.backend.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GameService gameService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       GameService gameService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.gameService = gameService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new RuntimeException("Email already exists");
        }

        String playerKey = "player-" + UUID.randomUUID();
        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );
        user.setPlayerKey(playerKey);
        user.setTutorialCompleted(false);

        User saved = userRepository.save(user);
        PlayerGameState player = gameService.createFreshPlayer(playerKey);

        return new AuthResponse("User registered successfully", saved.getEmail(), playerKey, player.getUid(), false);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() ->
                        new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new RuntimeException("Invalid credentials");
        }

        if (user.getPlayerKey() == null || user.getPlayerKey().isBlank()) {
            user.setPlayerKey("player-" + UUID.randomUUID());
        }

        PlayerGameState player = gameService.ensurePlayerExists(user.getPlayerKey());
        user.setTutorialCompleted(player.isTutorialCompleted());

        return new AuthResponse("Login successful", user.getEmail(), user.getPlayerKey(), player.getUid(), player.isTutorialCompleted());
    }
}
