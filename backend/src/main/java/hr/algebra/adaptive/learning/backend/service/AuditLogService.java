package hr.algebra.adaptive.learning.backend.service;

import hr.algebra.adaptive.learning.backend.dto.response.AuditLogResponse;

import java.util.List;

public interface AuditLogService {

    // isAdmin = true -> vidi sve; false (TEACHER) -> sve osim ADMIN akcija
    List<AuditLogResponse> getLogs(boolean isAdmin);
}

