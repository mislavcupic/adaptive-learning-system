package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.*;
import hr.algebra.adaptive.learning.backend.domain.enums.ExamAttemptStatus;
import hr.algebra.adaptive.learning.backend.domain.enums.TaskType;
import hr.algebra.adaptive.learning.backend.dto.request.ExamAnswerRequest;
import hr.algebra.adaptive.learning.backend.dto.response.ExamAttemptResponse;
import hr.algebra.adaptive.learning.backend.dto.request.ExamRequest;
import hr.algebra.adaptive.learning.backend.dto.response.ExamResponse;
import hr.algebra.adaptive.learning.backend.dto.request.SubmissionRequest;
import hr.algebra.adaptive.learning.backend.exception.BadRequestException;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.*;
import hr.algebra.adaptive.learning.backend.service.ExamService;
import hr.algebra.adaptive.learning.backend.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamServiceImpl implements ExamService {

    public static final String ISPIT_NOT_STARTED = "Niste započeli ovaj ispit.";
    public static final String EXAM_NOT_STARTED = ISPIT_NOT_STARTED;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository attemptRepository;
    private final ExamAnswerRepository answerRepository;
    private final TaskRepository taskRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionService submissionService;


    // NASTAVNIK


    @Override
    @Transactional
    public ExamResponse create(ExamRequest request, UUID createdById) {
        log.info("Kreiranje ispita: {}", request.getTitle());

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Kolegij nije pronađen: " + request.getCourseId()));

        User creator = userRepository.findById(createdById)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Korisnik nije pronađen: " + createdById));

        Exam exam = Exam.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .instructions(request.getInstructions())
                .course(course)
                .createdBy(creator)
                .timeLimitMinutes(request.getTimeLimitMinutes())
                .passingScore(request.getPassingScore() != null ? request.getPassingScore() : 50)
                .availableFrom(request.getAvailableFrom())
                .availableUntil(request.getAvailableUntil())
                .showTestResults(Boolean.TRUE.equals(request.getShowTestResults()))
                .isPublished(Boolean.TRUE.equals(request.getIsPublished()))
                .isActive(true)
                .tasks(resolveTasks(request.getTaskIds()))
                .build();

        Exam saved = examRepository.save(exam);
        return ExamResponse.fromEntityWithTasks(saved);
    }

    @Override
    @Transactional
    public ExamResponse update(UUID examId, ExamRequest request) {
        Exam exam = findExamOrThrow(examId);

        // Izmjena zadataka nakon sto je netko poceo rjesavati promijenila bi
        // ispit pod nogama tih studenata, pa to ne dopustamo.
        boolean hasAttempts = !attemptRepository.findByExamIdOrderByStartedAtDesc(examId).isEmpty();
        if (hasAttempts && request.getTaskIds() != null) {
            throw new BadRequestException(
                    "Zadaci se ne mogu mijenjati jer je ispit već započet.");
        }

        exam.setTitle(request.getTitle());
        exam.setDescription(request.getDescription());
        exam.setInstructions(request.getInstructions());
        exam.setTimeLimitMinutes(request.getTimeLimitMinutes());
        exam.setPassingScore(request.getPassingScore());
        exam.setAvailableFrom(request.getAvailableFrom());
        exam.setAvailableUntil(request.getAvailableUntil());
        exam.setShowTestResults(Boolean.TRUE.equals(request.getShowTestResults()));

        if (request.getTaskIds() != null) {
            exam.getTasks().clear();
            exam.getTasks().addAll(resolveTasks(request.getTaskIds()));
        }

        return ExamResponse.fromEntityWithTasks(examRepository.save(exam));
    }

    @Override
    public ExamResponse getById(UUID examId) {
        Exam exam = examRepository.findByIdWithTasks(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Ispit nije pronađen: " + examId));
        return ExamResponse.fromEntityWithTasks(exam);
    }

    @Override
    public List<ExamResponse> getByCourse(UUID courseId) {
        return examRepository.findByCourseIdAndIsActiveTrueOrderByCreatedAtDesc(courseId).stream()
                .map(ExamResponse::fromEntity)
                .toList();
    }

    @Override
    public List<ExamResponse> getByTeacher(UUID teacherId) {
        return examRepository.findByCreatedByIdAndIsActiveTrueOrderByCreatedAtDesc(teacherId).stream()
                .map(ExamResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public void publish(UUID examId) {
        Exam exam = findExamOrThrow(examId);

        if (exam.getTasks().isEmpty()) {
            throw new BadRequestException("Ispit bez zadataka ne može se objaviti.");
        }

        exam.setPublished(true);
        examRepository.save(exam);
        log.info("Ispit objavljen: {}", exam.getTitle());
    }

    @Override
    @Transactional
    public void unpublish(UUID examId) {
        Exam exam = findExamOrThrow(examId);
        exam.setPublished(false);
        examRepository.save(exam);
    }

    @Override
    @Transactional
    public void delete(UUID examId) {
        Exam exam = findExamOrThrow(examId);

        if (!attemptRepository.findByExamIdOrderByStartedAtDesc(examId).isEmpty()) {
            // Ispit s pokusajima se ne brise nego deaktivira, kako bi
            // rezultati studenata ostali sacuvani.
            exam.setActive(false);
            exam.setPublished(false);
            examRepository.save(exam);
            log.info("Ispit deaktiviran jer ima pokušaje: {}", exam.getTitle());
            return;
        }

        examRepository.delete(exam);
    }

    @Override
    public List<ExamAttemptResponse> getAttemptsForExam(UUID examId) {
        return attemptRepository.findAllForExamOverview(examId).stream()
                .map(ExamAttemptResponse::fromEntity)
                .toList();
    }

    @Override
    public ExamAttemptResponse getAttempt(UUID attemptId) {
        ExamAttempt attempt = attemptRepository.findByIdWithAnswers(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pokušaj nije pronađen: " + attemptId));
        return ExamAttemptResponse.fromEntityWithAnswers(attempt);
    }

    @Override
    @Transactional
    public ExamAttemptResponse gradeAnswer(UUID attemptId, UUID taskId,
                                           Integer score, String feedback, UUID teacherId) {
        ExamAnswer answer = answerRepository.findByAttemptIdAndTaskId(attemptId, taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Odgovor nije pronađen."));

        Integer maxScore = answer.getTask().getMaxScore();
        if (score != null && maxScore != null && (score < 0 || score > maxScore)) {
            throw new BadRequestException(
                    "Bodovi moraju biti između 0 i " + maxScore + ".");
        }

        answer.setTeacherScore(score);
        answer.setFinalScore(score);
        answer.setTeacherFeedback(feedback);
        answerRepository.save(answer);

        // Ukupan rezultat pokusaja mijenja se sa svakom ocjenom
        recalculateTotal(answer.getAttempt());

        return getAttempt(attemptId);
    }

    @Override
    @Transactional
    public ExamAttemptResponse finalizeReview(UUID attemptId, String feedback, UUID teacherId) {
        ExamAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Pokušaj nije pronađen."));

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Nastavnik nije pronađen."));

        attempt.setTeacherFeedback(feedback);
        attempt.setReviewedAt(LocalDateTime.now());
        attempt.setReviewedBy(teacher);
        attempt.setStatus(ExamAttemptStatus.REVIEWED);

        recalculateTotal(attempt);
        attemptRepository.save(attempt);

        return getAttempt(attemptId);
    }

    // ==================================================================
    // STUDENT
    // ==================================================================

    @Override
    public List<ExamResponse> getAvailableForStudent(UUID courseId, UUID studentId) {
        return examRepository.findAvailableForStudents(courseId).stream()
                .map(exam -> {
                    ExamResponse response = ExamResponse.fromEntity(exam);
                    attemptRepository.findByStudentIdAndExamId(studentId, exam.getId())
                            .ifPresent(a -> {
                                response.setMyAttemptStatus(a.getStatus().name());
                                response.setMyAttemptId(a.getId());
                            });
                    return response;
                })
                .toList();
    }

    @Override
    @Transactional
    public ExamAttemptResponse startAttempt(UUID examId, UUID studentId) {
        log.info("Student {} započinje ispit {}", studentId, examId);

        var existing = attemptRepository.findByStudentAndExamWithAnswers(studentId, examId);
        if (existing.isPresent()) {
            return processExistingAttempt(existing.get());
        }

        Exam exam = examRepository.findByIdWithTasks(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Ispit nije pronađen."));

        LocalDateTime now = LocalDateTime.now();
        validateExamAvailability(exam, now);

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student nije pronađen."));

        LocalDateTime deadline = calculateDeadline(exam, now);

        ExamAttempt attempt = ExamAttempt.builder()
                .exam(exam)
                .student(student)
                .status(ExamAttemptStatus.IN_PROGRESS)
                .startedAt(now)
                .deadlineAt(deadline)
                .maxScore(exam.getMaxScore())
                .answers(new ArrayList<>())
                .build();

        ExamAttempt saved = attemptRepository.save(attempt);
        createAndSaveAnswers(saved, exam);

        return ExamAttemptResponse.fromEntityWithAnswers(saved);
    }

    private ExamAttemptResponse processExistingAttempt(ExamAttempt attempt) {
        if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Ovaj ispit ste već predali.");
        }
        if (attempt.isExpired()) {
            closeExpired(attempt);
            throw new BadRequestException("Vrijeme za rješavanje ispita je isteklo.");
        }
        return ExamAttemptResponse.fromEntityWithAnswers(attempt);
    }

    private void validateExamAvailability(Exam exam, LocalDateTime now) {
        if (!exam.isPublished() || !exam.isActive()) {
            throw new BadRequestException("Ispit trenutno nije dostupan.");
        }
        if (exam.getAvailableFrom() != null && now.isBefore(exam.getAvailableFrom())) {
            throw new BadRequestException("Ispit još nije otvoren za rješavanje.");
        }
        if (exam.getAvailableUntil() != null && now.isAfter(exam.getAvailableUntil())) {
            throw new BadRequestException("Rok za rješavanje ispita je prošao.");
        }
    }

    private LocalDateTime calculateDeadline(Exam exam, LocalDateTime now) {
        LocalDateTime deadline = exam.getTimeLimitMinutes() != null
                ? now.plusMinutes(exam.getTimeLimitMinutes())
                : null;

        if (deadline != null && exam.getAvailableUntil() != null
                && deadline.isAfter(exam.getAvailableUntil())) {
            deadline = exam.getAvailableUntil();
        }
        return deadline;
    }

    private void createAndSaveAnswers(ExamAttempt saved, Exam exam) {
        for (Task task : exam.getTasks()) {
            ExamAnswer answer = ExamAnswer.builder()
                    .attempt(saved)
                    .task(task)
                    .answerContent(task.getTaskType() == TaskType.CODE
                            ? task.getStarterCode() : null)
                    .markedDone(false)
                    .build();
            saved.getAnswers().add(answerRepository.save(answer));
        }
    }
    @Override
    public ExamAttemptResponse getMyAttempt(UUID examId, UUID studentId) {
        ExamAttempt attempt = attemptRepository
                .findByStudentAndExamWithAnswers(studentId, examId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        EXAM_NOT_STARTED));
        return ExamAttemptResponse.fromEntityWithAnswers(attempt);
    }

    @Override
    @Transactional
    public ExamAttemptResponse saveAnswer(UUID examId, ExamAnswerRequest request, UUID studentId) {
        ExamAttempt attempt = attemptRepository
                .findByStudentAndExamWithAnswers(studentId, examId)
                .orElseThrow(() -> new ResourceNotFoundException(EXAM_NOT_STARTED));

        if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Ispit je predan i više se ne može mijenjati.");
        }

        if (attempt.isExpired()) {
            closeExpired(attempt);
            throw new BadRequestException("Vrijeme za rješavanje ispita je isteklo.");
        }

        ExamAnswer answer = answerRepository
                .findByAttemptIdAndTaskId(attempt.getId(), request.getTaskId())
                .orElseThrow(() -> new BadRequestException(
                        "Zadatak nije dio ovog ispita."));

        answer.setAnswerContent(request.getAnswerContent());
        answer.setAnsweredAt(LocalDateTime.now());
        if (request.getMarkedDone() != null) {
            answer.setMarkedDone(request.getMarkedDone());
        }

        // Izvrsavanje koda tijekom ispita, ako ispit to dopusta
        boolean shouldRun = Boolean.TRUE.equals(request.getRunTests())
                && answer.getTask().getTaskType() == TaskType.CODE
                && attempt.getExam().isShowTestResults()
                && request.getAnswerContent() != null
                && !request.getAnswerContent().isBlank();

        if (shouldRun) {
            try {
                var submissionRequest = new SubmissionRequest();
                submissionRequest.setTaskId(answer.getTask().getId());
                submissionRequest.setCode(request.getAnswerContent());

                var result = submissionService.submit(submissionRequest, studentId);
                submissionRepository.findById(result.getId())
                        .ifPresent(answer::setSubmission);
            } catch (Exception e) {
                log.error("Izvršavanje koda tijekom ispita nije uspjelo: {}", e.getMessage());
            }
        }

        answerRepository.save(answer);

        return getMyAttempt(examId, studentId);
    }

    @Override
    @Transactional
    public ExamAttemptResponse submitAttempt(UUID examId, UUID studentId) {
        log.info("Student {} predaje ispit {}", studentId, examId);

        ExamAttempt attempt = attemptRepository
                .findByStudentAndExamWithAnswers(studentId, examId)
                .orElseThrow(() -> new ResourceNotFoundException(EXAM_NOT_STARTED));

        if (attempt.getStatus() != ExamAttemptStatus.IN_PROGRESS) {
            throw new BadRequestException("Ispit je već predan.");
        }

        attempt.setStatus(ExamAttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(LocalDateTime.now());

        gradeAttempt(attempt);

        attemptRepository.save(attempt);
        return getMyAttempt(examId, studentId);
    }

    @Override
    public List<ExamAttemptResponse> getMyAttempts(UUID studentId) {
        return attemptRepository.findByStudentIdOrderByStartedAtDesc(studentId).stream()
                .map(ExamAttemptResponse::fromEntity)
                .toList();
    }

    // ==================================================================
    // POMOCNE METODE
    // ==================================================================

    private List<Task> resolveTasks(List<UUID> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Task> tasks = new ArrayList<>();
        for (UUID taskId : taskIds) {
            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Zadatak nije pronađen: " + taskId));
            tasks.add(task);
        }
        return tasks;
    }

    /**
     * Automatsko ocjenjivanje pri predaji.
     *
     * Programski zadaci ocjenjuju se prema udjelu prosao testova, zadaci s
     * jednim tocnim odgovorom usporedbom s ocekivanim, a tekstualni i
     * ceklista ostaju nastavniku na rucni pregled.
     */
    private void gradeAttempt(ExamAttempt attempt) {
        for (ExamAnswer answer : attempt.getAnswers()) {
            Task task = answer.getTask();
            int maxScore = task.getMaxScore() != null ? task.getMaxScore() : 0;

            evaluateTaskAnswer(task, answer, maxScore);
            answerRepository.save(answer);
        }

        recalculateTotal(attempt);
    }

    private void evaluateTaskAnswer(Task task, ExamAnswer answer, int maxScore) {
        if (task.getTaskType() == TaskType.CODE) {
            processCodeTask(answer, maxScore);
        } else if (task.getTaskType() == TaskType.MULTIPLE_CHOICE) {
            processMultipleChoiceTask(task, answer, maxScore);
        }
    }

    private void processCodeTask(ExamAnswer answer, int maxScore) {
        var submission = answer.getSubmission();
        if (submission == null) {
            return;
        }

        if (submission.getTestsTotal() != null && submission.getTestsTotal() > 0) {
            double ratio = (double) submission.getTestsPassed() / submission.getTestsTotal();
            answer.setAiScore((int) Math.round(ratio * maxScore));
            return;
        }

        if (submission.getAiScore() != null) {
            answer.setAiScore(submission.getAiScore());
        }
    }

    private void processMultipleChoiceTask(Task task, ExamAnswer answer, int maxScore) {
        String correct = task.getCorrectAnswer();
        String given = answer.getAnswerContent();

        if (correct != null && given != null && correct.trim().equalsIgnoreCase(given.trim())) {
            answer.setAiScore(maxScore);
        } else {
            answer.setAiScore(0);
        }
    }

    private void recalculateTotal(ExamAttempt attempt) {
        int total = attempt.getAnswers().stream()
                .map(ExamAnswer::getEffectiveScore)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        attempt.setTotalScore(total);

        if (attempt.getMaxScore() == null) {
            attempt.setMaxScore(attempt.getExam().getMaxScore());
        }

        if (attempt.getStatus() == ExamAttemptStatus.SUBMITTED) {
            attempt.setStatus(ExamAttemptStatus.GRADED);
        }

        attemptRepository.save(attempt);
    }

    private void closeExpired(ExamAttempt attempt) {
        attempt.setStatus(ExamAttemptStatus.EXPIRED);
        attempt.setSubmittedAt(LocalDateTime.now());
        gradeAttempt(attempt);
        attemptRepository.save(attempt);
        log.info("Pokušaj zatvoren zbog isteka roka: {}", attempt.getId());
    }

    private Exam findExamOrThrow(UUID examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Ispit nije pronađen: " + examId));
    }
}