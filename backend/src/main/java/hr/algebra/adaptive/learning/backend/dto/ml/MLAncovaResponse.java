package hr.algebra.adaptive.learning.backend.dto.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MLAncovaResponse {

    @JsonProperty("n_total")
    private Integer nTotal;

    private List<String> groups;

    private List<GroupDescriptive> descriptives;

    private Ancova ancova;

    @JsonProperty("adjusted_means")
    private List<AdjustedMean> adjustedMeans;

    @JsonProperty("effect_size")
    private EffectSize effectSize;

    private Assumptions assumptions;

    private String warning;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnoreProperties(ignoreUnknown = true)
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Effect {
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
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdjustedMean {
        private String group;
        private Integer n;
        @JsonProperty("observed_mean")
        private Double observedMean;
        @JsonProperty("adjusted_mean")
        private Double adjustedMean;
        private Double se;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EffectSize {
        @JsonProperty("cohens_d")
        private Double cohensD;
        @JsonProperty("hedges_g")
        private Double hedgesG;
        private Double se;
        @JsonProperty("ci_lower")
        private Double ciLower;
        @JsonProperty("ci_upper")
        private Double ciUpper;
        @JsonProperty("mean_difference")
        private Double meanDifference;
        @JsonProperty("pooled_sd")
        private Double pooledSd;
        private String magnitude;
        private String favors;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Assumptions {
        @JsonProperty("homogeneity_of_slopes")
        private AssumptionTest homogeneityOfSlopes;
        private AssumptionTest levene;
        private AssumptionTest shapiro;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AssumptionTest {
        private Double f;
        private Double statistic;
        private Double p;
        private Boolean satisfied;
        private String note;
    }
}