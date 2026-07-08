package hr.algebra.adaptive.learning.backend.dto.response;

import hr.algebra.adaptive.learning.backend.domain.entity.AuditLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AuditLogResponse {

    private UUID id;
    private UUID userId;
    private String userEmail;
    private String userRole;
    private String action;
    private String httpMethod;
    private String endpoint;
    private boolean success;
    private String errorMessage;
    private Long durationMs;
    private String ipAddress;
    private LocalDateTime createdAt;

    public static AuditLogResponse fromEntity(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .userEmail(a.getUserEmail())
                .userRole(a.getUserRole())
                .action(a.getAction())
                .httpMethod(a.getHttpMethod())
                .endpoint(a.getEndpoint())
                .success(a.isSuccess())
                .errorMessage(a.getErrorMessage())
                .durationMs(a.getDurationMs())
                .ipAddress(a.getIpAddress())
                .createdAt(a.getCreatedAt())
                .build();
    }
}