package com.jticket.jticket_backend;

import com.jticket.jticket_backend.entities.User;
import com.jticket.jticket_backend.entities.UserRole;
import com.jticket.jticket_backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class JticketBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(JticketBackendApplication.class, args);
	}

	// Setup default admin
	@Bean
	ApplicationRunner bootstrapAdmin(UserRepository userRepository,
									 PasswordEncoder passwordEncoder,
									 @Value("${app.admin.username}") String adminUsername,
									 @Value("${app.admin.email}") String adminEmail,
									 @Value("${app.admin.password}") String adminPassword) {
		return args -> {
			if (!userRepository.existsByRole(UserRole.ADMIN)) {
				User admin = new User(adminUsername, adminEmail, passwordEncoder.encode(adminPassword));
				admin.setRole(UserRole.ADMIN);
				userRepository.save(admin);
				System.out.println(">>> Admin user created: " + adminUsername);
			}
		};
	}
}

