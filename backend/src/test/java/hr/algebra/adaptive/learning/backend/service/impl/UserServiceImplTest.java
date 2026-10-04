package hr.algebra.adaptive.learning.backend.service.impl;

import hr.algebra.adaptive.learning.backend.domain.entity.SchoolClass;
import hr.algebra.adaptive.learning.backend.domain.entity.User;
import hr.algebra.adaptive.learning.backend.domain.enums.ResearchGroup;
import hr.algebra.adaptive.learning.backend.domain.enums.UserRole;
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
    private User guest;
    private SchoolClass schoolClass;

    @BeforeEach
    void setUp() {
        guestId = UUID.randomUUID();
        classId = UUID.randomUUID();

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

        when(userRepository.findById(guestId)).thenReturn(Optional.of(guest));
        when(schoolClassRepository.findById(classId)).thenReturn(Optional.of(schoolClass));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
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
            UUID unknown = UUID.randomUUID();
            when(userRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.approveUser(unknown, classId, false))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("baca iznimku za nepostojeći razred")
        void throwsForUnknownClass() {
            UUID unknown = UUID.randomUUID();
            when(schoolClassRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.approveUser(guestId, unknown, false))
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
        @DisplayName("mijenja ulogu")
        void updatesRole() {
            UserResponse result = userService.updateRole(guestId, UserRole.TEACHER);

            assertThat(result.getRole()).isEqualTo(UserRole.TEACHER);
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
}