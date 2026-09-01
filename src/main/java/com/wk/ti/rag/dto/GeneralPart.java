package com.wk.ti.rag.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class GeneralPart {
    protected List<String> headers;
    protected List<List<String>> rows;

    public abstract <T extends GeneralPart> T of(List<String> headers, List<List<String>> rows);

    @JsonIgnore
    public <T extends GeneralPart> T merge(List<T> items) {
        //Clean up empty data before grouping
        var sets = Optional.ofNullable(items)
                .orElse(new ArrayList<>())
                .stream().filter(Objects::nonNull)
                .filter(item -> !item.isEmpty())
                .toList();
        if (sets.isEmpty()) {
            return of(List.of(), List.of());
        }

        // Group DocumentSets by their headers
        Map<List<String>, List<T>> grouped =
                sets.stream()
                        .filter(ss -> ss.getHeaders() != null && !ss.getHeaders().isEmpty())
                        .collect(Collectors.groupingBy(T::getHeaders));

        if (grouped.size() == 1) {
            // All headers are the same → merge rows
            List<String> headerList = grouped.keySet().iterator().next();
            List<List<String>> mergedRows = grouped.values().stream()
                    .flatMap(list -> list.stream()
                            .filter(ds -> ds.getRows() != null)
                            .flatMap(ds -> ds.getRows().stream()))
                    .collect(Collectors.toList());

            return of(headerList, mergedRows);
        } else {
            // Headers differ → return the one with the largest rows count
            return sets.stream()
                    .max(Comparator.comparingInt(ss -> ss.getRows() != null ? ss.getRows().size() : 0))
                    .orElse(sets.getFirst());
        }
    }


    public boolean isEmpty() {
        return rows == null || rows.isEmpty() ||
                headers == null || headers.isEmpty() ||
                (headers.size() == 1 && headers.getFirst().isEmpty());
    }

}
