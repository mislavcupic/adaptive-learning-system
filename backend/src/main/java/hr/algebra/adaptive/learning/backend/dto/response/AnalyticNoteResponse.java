package hr.algebra.adaptive.learning.backend.dto.response;

import hr.algebra.adaptive.learning.backend.domain.entity.AnalyticNote;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AnalyticNoteResponse {
    private UUID id;
    private String insight;
    private String skillName;
    private String noteType;
    private UUID submissionId;
    private LocalDateTime createdAt;

    public static AnalyticNoteResponse fromEntity(AnalyticNote note) {
        return AnalyticNoteResponse.builder()
                .id(note.getId())
                .insight(note.getInsight())
                .skillName(note.getSkillName())
                .noteType(note.getNoteType())
                .submissionId(note.getSubmission() != null ? note.getSubmission().getId() : null)
                .createdAt(note.getCreatedAt())
                .build();
    }
}