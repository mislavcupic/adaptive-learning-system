package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.AuditLog;
import hr.algebra.adaptive.learning.backend.dto.response.AuditLogResponse;
import hr.algebra.adaptive.learning.backend.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogServiceImpl")
class AuditLogServiceImplTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private AuditLogServiceImpl service;

    @Test
    @DisplayName("administrator dobiva sve zapise")
    void adminGetsEverything() {
        when(auditLogRepository.findAllOrderByCreatedAtDesc())
                .thenReturn(List.of(entry("createTask"), entry("deleteUser")));

        List<AuditLogResponse> result = service.getLogs(true);

        assertThat(result).hasSize(2);
        verify(auditLogRepository).findAllOrderByCreatedAtDesc();
        verify(auditLogRepository, never()).findNonAdminOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("nastavnik dobiva zapise bez administratorskih")
    void teacherGetsFiltered() {
        when(auditLogRepository.findNonAdminOrderByCreatedAtDesc())
                .thenReturn(List.of(entry("createTask")));

        List<AuditLogResponse> result = service.getLogs(false);

        assertThat(result).hasSize(1);
        verify(auditLogRepository).findNonAdminOrderByCreatedAtDesc();
        verify(auditLogRepository, never()).findAllOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("prazna evidencija vraca praznu listu")
    void emptyLog() {
        when(auditLogRepository.findAllOrderByCreatedAtDesc()).thenReturn(List.of());

        assertThat(service.getLogs(true)).isEmpty();
    }

    private AuditLog entry(String action) {
        return AuditLog.builder()
                .userEmail("ines@test.hr")
                .userRole("TEACHER")
                .action(action)
                .httpMethod("POST")
                .endpoint("/api/tasks")
                .success(true)
                .durationMs(12L)
                .ipAddress("10.0.0.5")
                .build();
    }
}
