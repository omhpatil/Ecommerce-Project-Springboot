package com.ecommerce.repository;

import com.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ Check if a user already exists by email (useful for registration)
    boolean existsByEmail(String email);

    // ✅ Fetch user details by email (required for login / authentication)
    Optional<User> findByEmail(String email);
}
