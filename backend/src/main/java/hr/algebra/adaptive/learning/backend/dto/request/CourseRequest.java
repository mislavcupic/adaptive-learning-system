package hr.algebra.adaptive.learning.backend.dto.request;

import hr.algebra.adaptive.learning.backend.domain.enums.LanguageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseRequest {

    @NotBlank(message = "Course name is required")
    @Size(max = 255, message = "Course name must be less than 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    @NotNull(message = "Language type is required")
    private LanguageType languageType;
}
