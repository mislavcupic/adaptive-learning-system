package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.Submission;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.dto.response.StudentSubmissionsOverviewResponse;
import hr.algebra.adaptive.learning.backend.dto.response.StudentSubmissionsOverviewResponse.AttemptEntry;
import hr.algebra.adaptive.learning.backend.dto.response.StudentSubmissionsOverviewResponse.StudentBlock;
import hr.algebra.adaptive.learning.backend.repository.SubmissionRepository;
import hr.algebra.adaptive.learning.backend.service.SubmissionOverviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionOverviewServiceImpl implements SubmissionOverviewService {

    private final SubmissionRepository submissionRepository;

    @Override
    public StudentSubmissionsOverviewResponse getOverview(UUID courseId, UUID studentId, Integer lastDays) {

        LocalDateTime from = (lastDays != null && lastDays > 0)
                ? LocalDateTime.now().minusDays(lastDays)
                : null;

        // Jedan upit dohvaca sve predaje zajedno sa studentom, zadatkom i kolegijem.
        List<Submission> submissions =
                submissionRepository.findAllForTeacherOverview(courseId, studentId, from);

        log.info("Nastavnicki pregled: dohvaceno {} predaja", submissions.size());

        // Grupiranje po studentu; LinkedHashMap cuva redoslijed iz upita (prezime, ime).
        Map<UUID, List<Submission>> byStudent = submissions.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getStudent().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<StudentBlock> blocks = new ArrayList<>();

        for (Map.Entry<UUID, List<Submission>> entry : byStudent.entrySet()) {
            List<Submission> studentSubmissions = entry.getValue();
            User student = studentSubmissions.get(0).getStudent();

            List<AttemptEntry> attempts = studentSubmissions.stream()
                    .map(this::toAttempt)
                    .toList();

            long tasksAttempted = studentSubmissions.stream()
                    .map(s -> s.getTask().getId())
                    .distinct()
                    .count();

            long withAiFeedback = studentSubmissions.stream()
                    .filter(s -> s.getAiFeedback() != null && !s.getAiFeedback().isBlank())
                    .count();

            Double averageScore = studentSubmissions.stream()
                    .map(this::resolveScore)
                    .filter(java.util.Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .average()
                    .stream().boxed().findFirst().orElse(null);

            LocalDateTime lastActivity = studentSubmissions.stream()
                    .map(Submission::getCreatedAt)
                    .filter(java.util.Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            blocks.add(StudentBlock.builder()
                    .studentId(student.getId())
                    .firstName(student.getFirstName())
                    .lastName(student.getLastName())
                    .email(student.getEmail())
                    .schoolClassName(student.getSchoolClass() != null
                            ? student.getSchoolClass().getName() : null)
                    .researchGroup(student.getResearchGroup())
                    .totalSubmissions(studentSubmissions.size())
                    .tasksAttempted((int) tasksAttempted)
                    .withAiFeedback((int) withAiFeedback)
                    .averageScore(averageScore != null
                            ? Math.round(averageScore * 100.0) / 100.0 : null)
                    .lastActivity(lastActivity)
                    .attempts(attempts)
                    .build());
        }

        return StudentSubmissionsOverviewResponse.builder()
                .totalStudents(blocks.size())
                .totalSubmissions(submissions.size())
                .students(blocks)
                .build();
    }

    private AttemptEntry toAttempt(Submission s) {
        return AttemptEntry.builder()
                .submissionId(s.getId())
                .taskId(s.getTask().getId())
                .taskTitle(s.getTask().getTitle())
                .courseName(s.getTask().getOutcome().getCourse().getName())
                .status(s.getStatus())
                .aiScore(s.getAiScore())
                .teacherScore(s.getTeacherScore())
                .finalScore(s.getFinalScore())
                .maxScore(s.getTask().getMaxScore())
                .testsPassed(s.getTestsPassed())
                .testsTotal(s.getTestsTotal())
                .aiFeedback(s.getAiFeedback())
                .teacherFeedback(s.getTeacherFeedback())
                .executionTimeMs(s.getExecutionTimeMs())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private Integer resolveScore(Submission s) {
        if (s.getFinalScore() != null) return s.getFinalScore();
        if (s.getTeacherScore() != null) return s.getTeacherScore();
        return s.getAiScore();
    }
}