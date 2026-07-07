package hr.algebra.adaptive.learning.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLAncovaResponse {

    @JsonProperty("n_total")
    private Integer nTotal;

    private List<String> groups;

    private List<GroupDescriptive> descriptives;

    private Ancova ancova;

    private String warning;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GroupDescriptive {
        private String group;
        private Integer n;
        @JsonProperty("pretest_mean")
        private Double pretestMean;
        @JsonProperty("pretest_sd")
        private Double pretestSd;
        @JsonProperty("posttest_mean")
        private Double posttestMean;
        @JsonProperty("posttest_sd")
        private Double posttestSd;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ancova {
        private List<TableRow> table;
        @JsonProperty("group_effect")
        private Effect groupEffect;
        private Boolean significant;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableRow {
        private String source;
        private Double ss;
        private Double df;
        private Double f;
        private Double p;
        @JsonProperty("partial_eta_sq")
        private Double partialEtaSq;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Effect {
        private String source;
        private Double ss;
        private Double df;
        private Double f;
        private Double p;
        @JsonProperty("partial_eta_sq")
        private Double partialEtaSq;
    }
}