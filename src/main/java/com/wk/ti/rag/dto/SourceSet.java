package com.wk.ti.rag.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.wk.ti.util.ParserUtil.listListToString;
import static com.wk.ti.util.ParserUtil.listToString;

@SuppressWarnings("unchecked")
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Slf4j
public class SourceSet extends GeneralPart implements Serializable {
    public static final String NO_RESULT = "no results are available in AW DB resource";

    private Integer rawCount;

    @Builder
    public SourceSet(List<String> headers, List<List<String>> rows, int rawCount) {
        super(headers, rows);
        this.rawCount = rawCount;
    }

    @JsonIgnore
    public static SourceSet of(SourceSet sourceSet, String awUrl, int producedResult) {
        if (sourceSet == null || sourceSet.getRows() == null) {
            return fallbackSummary();
        }
        List<List<String>> results = sourceSet.getRows();
        List<List<String>> rows;
        int rawCount = results.isEmpty() ? 0 : results.size();
        rawCount = Math.max(rawCount, producedResult);
        int studyIdIdx = sourceSet.getHeaders().indexOf("study_id");
        if (studyIdIdx < 0) {
            return SourceSet.builder()
                    .rawCount(rawCount)
                    .headers(List.of())
                    .rows(List.of())
                    .build();
        } else {
            rows = results.stream()
                    .map(item -> List.of(
                            awUrl.replace("<ANALYSIS_ID>", String.valueOf(item.get(studyIdIdx)))
                    ))
                    .collect(Collectors.toList());
        }

        return SourceSet.builder()
                .rawCount(rawCount)
                .headers(List.of("url"))
                .rows(rows)
                .build();
    }

    @JsonIgnore
    public static SourceSet fallbackSummary() {
        return SourceSet.builder()
                .rawCount(0)
                .headers(List.of())
                .rows(List.of())
                .build();
    }

    @JsonIgnore
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE)
                .append("rawCount", rawCount)
                .append("headers", listToString(headers))
                .append("rows", listListToString(rows))
                .toString();
    }

    @JsonIgnore
    public static int getRawCount(List<SourceSet> sets) {
        if (sets == null || sets.isEmpty()) {
            return 0;
        }
        return sets.stream()
                .filter(Objects::nonNull)
                .mapToInt(SourceSet::getRawCount)
                .sum();
    }

    @Override
    public <T extends GeneralPart> T of(List<String> headers, List<List<String>> rows) {
        return (T) SourceSet.builder()
                .rawCount(rows != null ? rows.size() : 0)
                .headers(headers)
                .rows(rows)
                .build();
    }
}
