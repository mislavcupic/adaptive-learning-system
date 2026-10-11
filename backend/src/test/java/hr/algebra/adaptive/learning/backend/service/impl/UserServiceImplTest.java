package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.SchoolClass;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.GroupType;
import hr.algebra.adaptive.learning.backend.domain.enums.ResearchGroup;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
import hr.algebra.adaptive.learning.backend.dto.response.StudentResponse;
import hr.algebra.adaptive.learning.backend.dto.response.UserResponse;
import hr.algebra.adaptive.learning.backend.exception.BadRequestException;
import hr.algebra.adaptive.learning.backend.exception.ResourceNotFoundException;
import hr.algebra.adaptive.learning.backend.repository.SchoolClassRepository;
import hr.algebra.adaptive.learning.backend.repository.UserRepository;
import hr.algebra.adaptive.learning.backend.service.EmailService;
import hr.algebra.adaptive.learning.backend.service.ResearchGroupService;
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
@DisplayName("UserServiceImpl")
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private ResearchGroupService researchGroupService;
    @Mock private EmailService emailService;

    @InjectMocks private UserServiceImpl userService;

    private UUID guestId;
    private UUID classId;
    private UUID unknownId;
    private User guest;
    private User student;
    private SchoolClass schoolClass;

    @BeforeEach
    void setUp() {
        guestId = UUID.randomUUID();
        classId = UUID.randomUUID();
        unknownId = UUID.randomUUID();

        schoolClass = new SchoolClass();
        schoolClass.setId(classId);
        schoolClass.setName("2.A");

        guest = User.builder()
                .email("novi@test.hr")
                .firstName("Ivo")
                .lastName("Ivić")
                .role(UserRole.GUEST)
                .isActive(false)
                .emailVerified(true)
                .build();
        guest.setId(guestId);

        student = User.builder()
                .email("ana@test.hr")
                .firstName("Ana")
                .lastName("Anić")
                .role(UserRole.STUDENT)
                .isActive(true)
                .emailVerified(true)
                .schoolClass(schoolClass)
                .groupType(GroupType.EXPERIMENTAL)
                .researchGroup(ResearchGroup.EXPERIMENTAL)
                .build();
        student.setId(UUID.randomUUID());

        when(userRepository.findById(guestId)).thenReturn(Optional.of(guest));
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Nested
    @DisplayName("Dohvat korisnika")
    class Reading {

        @Test
        @DisplayName("vraća pojedinog korisnika")
        void returnsSingleUser() {
            assertThat(userService.getById(guestId).getEmail()).isEqualTo("novi@test.hr");
        }

        @Test
        @DisplayName("baca iznimku za nepostojećeg korisnika")
        void missingUser() {
            assertThatThrownBy(() -> userService.getById(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("vraća sve korisnike")
        void returnsAll() {
            when(userRepository.findAll()).thenReturn(List.of(guest, student));

            assertThat(userService.getAll()).hasSize(2);
        }

        @Test
        @DisplayName("vraća korisnike po ulozi")
        void returnsByRole() {
            when(userRepository.findByRole(UserRole.GUEST)).thenReturn(List.of(guest));

            assertThat(userService.getByRole(UserRole.GUEST)).hasSize(1);
            verify(userRepository).findByRole(UserRole.GUEST);
        }

        @Test
        @DisplayName("vraća samo aktivne studente")
        void returnsActiveStudents() {
            when(userRepository.findByRoleAndIsActiveTrue(UserRole.STUDENT))
                    .thenReturn(List.of(student));

            List<StudentResponse> result = userService.getAllStudents();

            assertThat(result).hasSize(1);
            verify(userRepository).findByRoleAndIsActiveTrue(UserRole.STUDENT);
        }

        @Test
        @DisplayName("vraća nastavnike")
        void returnsTeachers() {
            User teacher = User.builder()
                    .email("ines@test.hr")
                    .firstName("Ines")
                    .lastName("Inić")
                    .role(UserRole.TEACHER)
                    .isActive(true)
                    .emailVerified(true)
                    .build();
            teacher.setId(UUID.randomUUID());

            when(userRepository.findByRoleAndIsActiveTrue(UserRole.TEACHER))
                    .thenReturn(List.of(teacher));

            assertThat(userService.getAllTeachers()).hasSize(1);
        }

        @Test
        @DisplayName("prazan popis kad nema nastavnika")
        void noTeachers() {
            when(userRepository.findByRoleAndIsActiveTrue(UserRole.TEACHER))
                    .thenReturn(List.of());

            assertThat(userService.getAllTeachers()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Filtriranje po skupini")
    class FilteringByGroup {

        @Test
        @DisplayName("vraća studente tražene skupine")
        void returnsMatchingGroup() {
            when(userRepository.findByRoleAndIsActiveTrue(UserRole.STUDENT))
                    .thenReturn(List.of(student));

            assertThat(userService.getStudentsByGroup(GroupType.EXPERIMENTAL)).hasSize(1);
        }

        @Test
        @DisplayName("izostavlja studente bez dodijeljene skupine")
        void skipsStudentsWithoutGroup() {
            User withoutGroup = User.builder()
                    .email("bez@test.hr")
                    .firstName("Bez")
                    .lastName("Skupine")
                    .role(UserRole.STUDENT)
                    .isActive(true)
                    .emailVerified(true)
                    .schoolClass(schoolClass)
                    .build();
            withoutGroup.setId(UUID.randomUUID());

            when(userRepository.findByRoleAndIsActiveTrue(UserRole.STUDENT))
                    .thenReturn(List.of(student, withoutGroup));

            assertThat(userService.getStudentsByGroup(GroupType.EXPERIMENTAL)).hasSize(1);
        }

        @Test
        @DisplayName("prazan popis kad nitko nije u toj skupini")
        void noMatches() {
            when(userRepository.findByRoleAndIsActiveTrue(UserRole.STUDENT))
                    .thenReturn(List.of());

            assertThat(userService.getStudentsByGroup(GroupType.EXPERIMENTAL)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Odobravanje korisnika")
    class Approval {

        @Test
        @DisplayName("postavlja ulogu STUDENT i aktivira račun")
        void promotesToStudent() {
            UserResponse result = userService.approveUser(guestId, classId, false);

            assertThat(result.getRole()).isEqualTo(UserRole.STUDENT);
            assertThat(result.isActive()).isTrue();
        }

        @Test
        @DisplayName("dodjeljuje razred")
        void assignsSchoolClass() {
            userService.approveUser(guestId, classId, false);

            assertThat(guest.getSchoolClass()).isEqualTo(schoolClass);
        }

        @Test
        @DisplayName("dodjeljuje istraživačku skupinu kad je student uključen u istraživanje")
        void assignsResearchGroup() {
            when(researchGroupService.assignGroup(classId))
                    .thenReturn(ResearchGroup.EXPERIMENTAL);

            userService.approveUser(guestId, classId, true);

            assertThat(guest.getResearchGroup()).isEqualTo(ResearchGroup.EXPERIMENTAL);
            verify(researchGroupService).assignGroup(classId);
        }

        @Test
        @DisplayName("ostavlja skupinu nedodijeljenom kad student nije u istraživanju")
        void leavesGroupUnassigned() {
            userService.approveUser(guestId, classId, false);

            assertThat(guest.getResearchGroup()).isEqualTo(ResearchGroup.NOT_ASSIGNED);
            verify(researchGroupService, never()).assignGroup(any());
        }

        @Test
        @DisplayName("ne dodjeljuje skupinu bez razreda")
        void noGroupWithoutClass() {
            userService.approveUser(guestId, null, true);

            assertThat(guest.getResearchGroup()).isEqualTo(ResearchGroup.NOT_ASSIGNED);
            verify(researchGroupService, never()).assignGroup(any());
        }

        @Test
        @DisplayName("odobrenje bez razreda ne dira razred")
        void keepsNoSchoolClass() {
            userService.approveUser(guestId, null, false);

            assertThat(guest.getSchoolClass()).isNull();
            verify(schoolClassRepository, never()).findById(any());
        }

        @Test
        @DisplayName("šalje obavijest o aktivaciji računa")
        void sendsApprovalEmail() {
            userService.approveUser(guestId, classId, false);

            verify(emailService).sendAccountApprovedEmail(any(User.class));
        }

        @Test
        @DisplayName("odbija odobrenje korisnika koji nije GUEST")
        void rejectsNonGuest() {
            guest.setRole(UserRole.STUDENT);

            assertThatThrownBy(() -> userService.approveUser(guestId, classId, false))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("nije GUEST");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("odbija odobrenje bez potvrđene e-mail adrese")
        void rejectsUnverifiedEmail() {
            guest.setEmailVerified(false);

            assertThatThrownBy(() -> userService.approveUser(guestId, classId, false))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("nije potvrdio");

            verify(emailService, never()).sendAccountApprovedEmail(any());
        }

        @Test
        @DisplayName("baca iznimku za nepostojećeg korisnika")
        void throwsForUnknownUser() {
            assertThatThrownBy(() -> userService.approveUser(unknownId, classId, false))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("baca iznimku za nepostojeći razred")
        void throwsForUnknownClass() {
            UUID unknownClass = UUID.randomUUID();
            when(schoolClassRepository.findById(unknownClass)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.approveUser(guestId, unknownClass, false))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Odbijanje prijave")
    class Rejection {

        @Test
        @DisplayName("briše korisnika koji čeka odobrenje")
        void deletesPendingUser() {
            userService.rejectPendingUser(guestId);

            verify(userRepository).delete(guest);
        }

        @Test
        @DisplayName("odbija brisanje korisnika koji nije GUEST")
        void rejectsNonGuest() {
            guest.setRole(UserRole.TEACHER);

            assertThatThrownBy(() -> userService.rejectPendingUser(guestId))
                    .isInstanceOf(BadRequestException.class);

            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("baca iznimku za nepostojećeg korisnika")
        void throwsForUnknownUser() {
            assertThatThrownBy(() -> userService.rejectPendingUser(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Popis prijava na čekanju")
    class PendingList {

        @Test
        @DisplayName("prikazuje samo korisnike s potvrđenom adresom")
        void onlyVerifiedUsers() {
            when(userRepository.findByRoleAndIsActiveFalseAndEmailVerifiedTrue(UserRole.GUEST))
                    .thenReturn(List.of(guest));

            List<UserResponse> result = userService.getPendingRegistrations();

            assertThat(result).hasSize(1);
            verify(userRepository).findByRoleAndIsActiveFalseAndEmailVerifiedTrue(UserRole.GUEST);
        }

        @Test
        @DisplayName("broji samo korisnike s potvrđenom adresom")
        void countsOnlyVerified() {
            when(userRepository.countByRoleAndIsActiveFalseAndEmailVerifiedTrue(UserRole.GUEST))
                    .thenReturn(3L);

            assertThat(userService.getPendingRegistrationsCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("vraća prazan popis kad nema prijava")
        void emptyWhenNoPending() {
            when(userRepository.findByRoleAndIsActiveFalseAndEmailVerifiedTrue(UserRole.GUEST))
                    .thenReturn(List.of());

            assertThat(userService.getPendingRegistrations()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Izmjena korisnika")
    class Updating {

        @Test
        @DisplayName("mijenja ime i prezime")
        void updatesName() {
            UserResponse result = userService.updateProfile(guestId, "Ivana", "Ivanić");

            assertThat(result.getFirstName()).isEqualTo("Ivana");
            assertThat(result.getLastName()).isEqualTo("Ivanić");
        }

        @Test
        @DisplayName("prazno ime ostavlja staro")
        void keepsNameWhenNull() {
            UserResponse result = userService.updateProfile(guestId, null, null);

            assertThat(result.getFirstName()).isEqualTo("Ivo");
            assertThat(result.getLastName()).isEqualTo("Ivić");
        }

        @Test
        @DisplayName("mijenja samo ime kad prezime nije zadano")
        void updatesOnlyFirstName() {
            UserResponse result = userService.updateProfile(guestId, "Ivana", null);

            assertThat(result.getFirstName()).isEqualTo("Ivana");
            assertThat(result.getLastName()).isEqualTo("Ivić");
        }

        @Test
        @DisplayName("mijenja ulogu")
        void updatesRole() {
            UserResponse result = userService.updateRole(guestId, UserRole.TEACHER);

            assertThat(result.getRole()).isEqualTo(UserRole.TEACHER);
        }

        @Test
        @DisplayName("mijenja status računa")
        void updatesStatus() {
            assertThat(userService.updateStatus(guestId, true).isActive()).isTrue();
            assertThat(userService.updateStatus(guestId, false).isActive()).isFalse();
        }

        @Test
        @DisplayName("mijenja skupinu studenta")
        void updatesGroup() {
            userService.updateGroup(guestId, GroupType.EXPERIMENTAL);

            assertThat(guest.getGroupType()).isEqualTo(GroupType.EXPERIMENTAL);
        }

        @Test
        @DisplayName("deaktivira račun")
        void deactivates() {
            guest.setActive(true);

            userService.deactivate(guestId);

            assertThat(guest.isActive()).isFalse();
        }

        @Test
        @DisplayName("aktivira račun")
        void activates() {
            userService.activate(guestId);

            assertThat(guest.isActive()).isTrue();
        }

        @Test
        @DisplayName("briše korisnika")
        void deletes() {
            userService.delete(guestId);

            verify(userRepository).delete(guest);
        }
    }

    @Nested
    @DisplayName("Nepostojeći korisnik")
    class MissingUser {

        @Test
        @DisplayName("izmjena profila baca iznimku")
        void updateProfileThrows() {
            assertThatThrownBy(() -> userService.updateProfile(unknownId, "A", "B"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("izmjena uloge baca iznimku")
        void updateRoleThrows() {
            assertThatThrownBy(() -> userService.updateRole(unknownId, UserRole.TEACHER))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("izmjena statusa baca iznimku")
        void updateStatusThrows() {
            assertThatThrownBy(() -> userService.updateStatus(unknownId, true))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("izmjena skupine baca iznimku")
        void updateGroupThrows() {
            assertThatThrownBy(() -> userService.updateGroup(unknownId, GroupType.EXPERIMENTAL))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("aktivacija baca iznimku")
        void activateThrows() {
            assertThatThrownBy(() -> userService.activate(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("deaktivacija baca iznimku")
        void deactivateThrows() {
            assertThatThrownBy(() -> userService.deactivate(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("brisanje baca iznimku")
        void deleteThrows() {
            assertThatThrownBy(() -> userService.delete(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).delete(any());
        }

        @Nested
        @DisplayName("Anonimizacija")
        class Anonymizing {

            @Test
            @DisplayName("uklanja ime i prezime")
            void removesName() {
                UserResponse result = userService.anonymize(guestId);

                assertThat(result.getFirstName()).isEqualTo("Anonimizirano");
                assertThat(result.getLastName()).isEqualTo("Anonimizirano");
            }

            @Test
            @DisplayName("zamjenjuje e-mail adresu oznakom")
            void replacesEmail() {
                UserResponse result = userService.anonymize(guestId);

                assertThat(result.getEmail()).endsWith("@anonimizirano.local");
                assertThat(result.getEmail()).doesNotContain("novi@test.hr");
            }

            @Test
            @DisplayName("deaktivira račun")
            void deactivatesAccount() {
                guest.setActive(true);

                userService.anonymize(guestId);

                assertThat(guest.isActive()).isFalse();
            }

            @Test
            @DisplayName("briše tragove vanjske prijave i potvrde adrese")
            void clearsProviderAndToken() {
                guest.setProviderId("111545373032881004436");
                guest.setVerificationToken("token123");

                userService.anonymize(guestId);

                assertThat(guest.getProviderId()).isNull();
                assertThat(guest.getVerificationToken()).isNull();
                assertThat(guest.getVerificationTokenExpiry()).isNull();
            }

            @Test
            @DisplayName("mijenja lozinku u neupotrebljivu")
            void scramblesPassword() {
                guest.setPassword("$2a$10$hash");

                userService.anonymize(guestId);

                assertThat(guest.getPassword()).isNotEqualTo("$2a$10$hash");
            }

            @Test
            @DisplayName("zadržava razred i istraživačku skupinu")
            void keepsResearchData() {
                when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));

                userService.anonymize(student.getId());

                assertThat(student.getSchoolClass()).isEqualTo(schoolClass);
                assertThat(student.getResearchGroup()).isEqualTo(ResearchGroup.EXPERIMENTAL);
            }

            @Test
            @DisplayName("odbija ponovnu anonimizaciju")
            void rejectsSecondAnonymization() {
                guest.setEmail("obrisan-nesto@anonimizirano.local");

                assertThatThrownBy(() -> userService.anonymize(guestId))
                        .isInstanceOf(BadRequestException.class)
                        .hasMessageContaining("već anonimiziran");

                verify(userRepository, never()).save(any());
            }

            @Test
            @DisplayName("baca iznimku za nepostojećeg korisnika")
            void missingUser() {
                assertThatThrownBy(() -> userService.anonymize(unknownId))
                        .isInstanceOf(ResourceNotFoundException.class);
            }
        }
    }


}