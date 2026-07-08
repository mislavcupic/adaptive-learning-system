package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.dto.response.AuditLogResponse;
import hr.algebra.adaptive.learning.backend.repository.AuditLogRepository;
import hr.algebra.adaptive.learning.backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public List<AuditLogResponse> getLogs(boolean isAdmin) {
        var logs = isAdmin
                ? auditLogRepository.findAllOrderByCreatedAtDesc()
                : auditLogRepository.findNonAdminOrderByCreatedAtDesc();

        return logs.stream()
                .map(AuditLogResponse::fromEntity)
                .toList();
    }
}