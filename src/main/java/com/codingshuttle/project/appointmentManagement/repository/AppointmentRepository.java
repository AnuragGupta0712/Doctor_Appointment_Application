package com.codingshuttle.project.appointmentManagement.repository;

import com.codingshuttle.project.appointmentManagement.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
}