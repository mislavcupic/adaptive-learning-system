package hr.algebra.adaptive.learning.backend.dto.ml;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLAncovaRequest {

    private List<Record> records;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Record {
        private String group;
        private double pretest;
        private double posttest;
    }
}
