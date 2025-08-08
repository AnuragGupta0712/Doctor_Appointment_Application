package com.codingshuttle.project.appointmentManagement.repository;

import com.codingshuttle.project.appointmentManagement.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
}