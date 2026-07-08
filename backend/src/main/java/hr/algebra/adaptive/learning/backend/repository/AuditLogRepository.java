package hr.algebra.adaptive.learning.backend.repository;

import hr.algebra.adaptive.learning.backend.domain.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    // Svi logovi, najnoviji prvo (za ADMIN)
    @Query("SELECT a FROM AuditLog a ORDER BY a.createdAt DESC")
    List<AuditLog> findAllOrderByCreatedAtDesc();

    // Logovi bez ADMIN akcija (za TEACHER) — ne prikazuje akcije administratora
    @Query("SELECT a FROM AuditLog a WHERE a.userRole IS NULL OR a.userRole <> 'ADMIN' ORDER BY a.createdAt DESC")
    List<AuditLog> findNonAdminOrderByCreatedAtDesc();

    // Filtriranje po korisniku
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId ORDER BY a.createdAt DESC")
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);
}