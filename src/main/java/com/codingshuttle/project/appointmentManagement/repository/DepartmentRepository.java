package com.codingshuttle.project.appointmentManagement.repository;

import com.codingshuttle.project.appointmentManagement.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
}