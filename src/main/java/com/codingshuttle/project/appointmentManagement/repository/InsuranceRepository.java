package com.codingshuttle.project.appointmentManagement.repository;

import com.codingshuttle.project.appointmentManagement.entity.Insurance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsuranceRepository extends JpaRepository<Insurance, Long> {
}