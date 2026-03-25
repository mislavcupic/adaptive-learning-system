package hr.algebra.adaptive.learning.backend.service;

import hr.algebra.adaptive.learning.backend.dto.response.AdminDashboardResponse;
import hr.algebra.adaptive.learning.backend.dto.response.StudentDashboardResponse;
import hr.algebra.adaptive.learning.backend.dto.response.TeacherDashboardResponse;

import java.util.UUID;

public interface DashboardService {

    StudentDashboardResponse getStudentDashboard(UUID studentId);

    TeacherDashboardResponse getTeacherDashboard(UUID teacherId);

    AdminDashboardResponse getAdminDashboard();
}