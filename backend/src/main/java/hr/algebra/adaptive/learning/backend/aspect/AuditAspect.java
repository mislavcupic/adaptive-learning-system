package hr.algebra.adaptive.learning.backend.aspect;

import hr.algebra.adaptive.learning.backend.domain.entity.AuditLog;
import hr.algebra.adaptive.learning.backend.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    // Presretni SVE metode u controller paketu (osim get)...
    @Pointcut("execution(* hr.algebra.adaptive.learning.backend.controller..*(..))")
    public void controllerMethods() {}

    @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public void mutatingMethods() {}

    @Around("controllerMethods() && mutatingMethods()")
    public Object auditAction(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        String action = joinPoint.getSignature().getName();
        String httpMethod = null;
        String endpoint = null;
        String ipAddress = null;

        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                httpMethod = req.getMethod();
                endpoint = req.getRequestURI();
                ipAddress = extractIp(req);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        UserInfo user = currentUser();

        boolean success = true;
        String errorMessage = null;

        try {
            Object result;
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            success = false;
            errorMessage = ex.getMessage();
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - start;
            try {
                AuditLog logEntry = AuditLog.builder()
                        .userId(user.id())
                        .userEmail(user.email())
                        .userRole(user.role())
                        .action(action)
                        .httpMethod(httpMethod)
                        .endpoint(endpoint)
                        .success(success)
                        .errorMessage(errorMessage)
                        .durationMs(duration)
                        .ipAddress(ipAddress)
                        .build();
                auditLogRepository.save(logEntry);
            } catch (Exception e) {
                log.error("Failed to write audit log: {}", e.getMessage());
            }
        }
    }

    private UserInfo currentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return new UserInfo(null, null, null);
            }
            String email = auth.getName();
            String role = auth.getAuthorities().stream()
                    .map(Object::toString)
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.substring(5))
                    .findFirst()
                    .orElse(null);
            return new UserInfo(null, email, role);
        } catch (Exception e) {
            log.error(e.getMessage());
            return new UserInfo(null, null, null);
        }
    }

    private String extractIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private record UserInfo(java.util.UUID id, String email, String role) {}
}