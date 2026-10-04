package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.Course;
import hr.algebra.adaptive.learning.backend.domain.entity.LearningOutcome;
import hr.algebra.adaptive.learning.backend.domain.entity.Task;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.LanguageType;
import hr.algebra.adaptive.learning.backend.domain.enums.TaskType;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.request.TaskRequest;
import hr.algebra.adaptive.learning.backend.dto.response.TaskResponse;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.LearningOutcomeRepository;
import hr.algebra.adaptive.learning.backend.repository.TaskRepository;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskServiceImpl")
class TaskServiceImplTest {

    @Mock private TaskRepository taskRepository;
    @Mock private LearningOutcomeRepository outcomeRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private TaskServiceImpl taskService;

    private UUID taskId;
    private UUID outcomeId;
    private UUID teacherId;
    private LearningOutcome outcome;
    private User teacher;
    private Task existingTask;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        outcomeId = UUID.randomUUID();
        teacherId = UUID.randomUUID();

        Course course = new Course();
        course.setId(UUID.randomUUID());
        course.setName("Uvod u programiranje");
        course.setLanguageType(LanguageType.C);

        outcome = new LearningOutcome();
        outcome.setId(outcomeId);
        outcome.setName("Petlje");
        outcome.setCourse(course);

        teacher = User.builder()
                .email("ines@test.hr")
                .firstName("Ines")
                .lastName("Inić")
                .role(UserRole.TEACHER)
                .build();
        teacher.setId(teacherId);

        existingTask = Task.builder()
                .title("Zbroj niza")
                .description("Izračunaj zbroj")
                .instructions("Napiši program")
                .taskType(TaskType.CODE)
                .starterCode("int main() {}")
                .solutionCode("rjesenje")
                .testCases("[]")
                .maxScore(20)
                .timeLimitSeconds(30)
                .memoryLimitMb(128)
                .outcome(outcome)
                .createdBy(teacher)
                .orderIndex(1)
                .isActive(true)
                .build();
        existingTask.setId(taskId);

        when(outcomeRepository.findById(outcomeId)).thenReturn(Optional.of(outcome));
        when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> {
            Task t = i.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
    }

    @Nested
    @DisplayName("Kreiranje zadatka")
    class Creating {

        @Test
        @DisplayName("sprema zadatak s predanim podacima")
        void savesWithGivenData() {
            TaskResponse result = taskService.create(request(TaskType.CODE), teacherId);

            assertThat(result.getTitle()).isEqualTo("Novi zadatak");
            assertThat(result.getMaxScore()).isEqualTo(25);
        }

        @Test
        @DisplayName("novi zadatak je aktivan")
        void newTaskIsActive() {
            taskService.create(request(TaskType.CODE), teacherId);

            verify(taskRepository).save(argThat(Task::isActive));
        }

        @Test
        @DisplayName("bez navedenog tipa pretpostavlja programski zadatak")
        void defaultsToCodeType() {
            TaskRequest req = request(null);

            taskService.create(req, teacherId);

            verify(taskRepository).save(argThat(t -> t.getTaskType() == TaskType.CODE));
        }

        @Test
        @DisplayName("čuva opcije i točan odgovor za višestruki izbor")
        void keepsChoiceFields() {
            TaskRequest req = request(TaskType.MULTIPLE_CHOICE);
            req.setOptions("[\"A\",\"B\"]");
            req.setCorrectAnswer("A");

            taskService.create(req, teacherId);

            verify(taskRepository).save(argThat(t ->
                    "[\"A\",\"B\"]".equals(t.getOptions()) && "A".equals(t.getCorrectAnswer())));
        }

        @Test
        @DisplayName("povezuje zadatak s ishodom učenja i autorom")
        void linksOutcomeAndCreator() {
            taskService.create(request(TaskType.CODE), teacherId);

            verify(taskRepository).save(argThat(t ->
                    t.getOutcome().equals(outcome) && t.getCreatedBy().equals(teacher)));
        }

        @Test
        @DisplayName("baca iznimku za nepostojeći ishod učenja")
        void throwsForUnknownOutcome() {
            UUID unknown = UUID.randomUUID();
            when(outcomeRepository.findById(unknown)).thenReturn(Optional.empty());

            TaskRequest req = request(TaskType.CODE);
            req.setOutcomeId(unknown);

            assertThatThrownBy(() -> taskService.create(req, teacherId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("baca iznimku za nepostojećeg autora")
        void throwsForUnknownCreator() {
            UUID unknown = UUID.randomUUID();
            when(userRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.create(request(TaskType.CODE), unknown))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Izmjena zadatka")
    class Updating {

        @Test
        @DisplayName("mijenja naslov i bodove")
        void updatesTitleAndScore() {
            TaskRequest req = request(TaskType.CODE);
            req.setTitle("Promijenjeni naslov");
            req.setMaxScore(50);

            TaskResponse result = taskService.update(taskId, req);

            assertThat(result.getTitle()).isEqualTo("Promijenjeni naslov");
            assertThat(result.getMaxScore()).isEqualTo(50);
        }

        @Test
        @DisplayName("zadržava postojeći tip kad novi nije naveden")
        void keepsTypeWhenNotGiven() {
            TaskRequest req = request(null);

            taskService.update(taskId, req);

            assertThat(existingTask.getTaskType()).isEqualTo(TaskType.CODE);
        }

        @Test
        @DisplayName("mijenja tip zadatka kad je naveden")
        void changesTypeWhenGiven() {
            TaskRequest req = request(TaskType.TEXT);

            taskService.update(taskId, req);

            assertThat(existingTask.getTaskType()).isEqualTo(TaskType.TEXT);
        }

        @Test
        @DisplayName("baca iznimku za nepostojeći zadatak")
        void throwsForUnknownTask() {
            UUID unknown = UUID.randomUUID();
            when(taskRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.update(unknown, request(TaskType.CODE)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Umnožavanje zadatka")
    class Duplicating {

        @Test
        @DisplayName("dodaje oznaku kopije u naslov")
        void marksAsCopy() {
            TaskResponse result = taskService.duplicate(taskId, teacherId);

            assertThat(result.getTitle()).isEqualTo("Zbroj niza (kopija)");
        }

        @Test
        @DisplayName("preuzima sadržaj izvornog zadatka")
        void copiesContent() {
            taskService.duplicate(taskId, teacherId);

            verify(taskRepository).save(argThat(t ->
                    "int main() {}".equals(t.getStarterCode())
                            && "rjesenje".equals(t.getSolutionCode())
                            && t.getMaxScore() == 20
                            && t.getTaskType() == TaskType.CODE));
        }

        @Test
        @DisplayName("zadržava isti ishod učenja")
        void keepsOutcome() {
            taskService.duplicate(taskId, teacherId);

            verify(taskRepository).save(argThat(t -> t.getOutcome().equals(outcome)));
        }

        @Test
        @DisplayName("kopija je novi zapis, ne mijenja izvornik")
        void doesNotTouchOriginal() {
            taskService.duplicate(taskId, teacherId);

            assertThat(existingTask.getTitle()).isEqualTo("Zbroj niza");
        }

        @Test
        @DisplayName("baca iznimku za nepostojeći zadatak")
        void throwsForUnknownTask() {
            UUID unknown = UUID.randomUUID();
            when(taskRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.duplicate(unknown, teacherId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Brisanje i dohvat")
    class DeletingAndFetching {

        @Test
        @DisplayName("briše zadatak")
        void deletesTask() {
            taskService.delete(taskId);

            verify(taskRepository).delete(existingTask);
        }

        @Test
        @DisplayName("baca iznimku pri brisanju nepostojećeg zadatka")
        void throwsWhenDeletingUnknown() {
            UUID unknown = UUID.randomUUID();
            when(taskRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.delete(unknown))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("vraća zadatak po identifikatoru")
        void returnsById() {
            TaskResponse result = taskService.getById(taskId);

            assertThat(result.getTitle()).isEqualTo("Zbroj niza");
        }

        @Test
        @DisplayName("vraća zadatke ishoda učenja")
        void returnsByOutcome() {
            when(taskRepository.findByOutcomeIdOrderByOrderIndexAsc(outcomeId))                    .thenReturn(List.of(existingTask));

            assertThat(taskService.getByOutcome(outcomeId)).hasSize(1);
        }

        @Test
        @DisplayName("vraća prazan popis kad ishod nema zadataka")
        void emptyWhenNoTasks() {
            when(taskRepository.findByOutcomeIdOrderByOrderIndexAsc(outcomeId))                    .thenReturn(List.of());

            assertThat(taskService.getByOutcome(outcomeId)).isEmpty();
        }
    }

    private TaskRequest request(TaskType type) {
        TaskRequest r = new TaskRequest();
        r.setTitle("Novi zadatak");
        r.setDescription("Opis");
        r.setInstructions("Upute");
        r.setTaskType(type);
        r.setOutcomeId(outcomeId);
        r.setMaxScore(25);
        r.setTimeLimitSeconds(30);
        r.setMemoryLimitMb(128);
        r.setOrderIndex(1);
        return r;
    }
}
