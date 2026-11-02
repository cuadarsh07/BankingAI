package in.ashish.combined.service;

import in.ashish.combined.dto.AuthRequest;
import in.ashish.combined.dto.AuthResponse;
import in.ashish.combined.dto.RegisterRequest;

/**
 * Service interface for user registration and authentication.
 */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(AuthRequest request);
}
