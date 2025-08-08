package com.codingshuttle.project.appointmentManagement.repository;

import com.codingshuttle.project.appointmentManagement.entity.User;
import com.codingshuttle.project.appointmentManagement.entity.type.AuthProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByProviderIdAndProviderType(String providerId, AuthProviderType providerType);
}