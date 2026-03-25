package hr.algebra.adaptive.learning.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class SchoolClassRequest {

    @NotBlank(message = "Class name is required")
    @Size(max = 255, message = "Class name must be less than 255 characters")
    private String name;

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;

    private String academicYear;

    private Set<UUID> courseIds;
}
