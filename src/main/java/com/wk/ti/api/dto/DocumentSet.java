package com.wk.ti.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentSet {
    private List<Item> documents;

    @JsonIgnore
    public static DocumentSet of(List<Document> results) {
        List<Item> documents = results.stream()
                .map(item ->
                        Item.builder()
                                .title(item.title())
                                .type(item.type())
                                .url(item.source())
                                .similarity(item.similarity())
                                .build())
                .collect(Collectors.toList());
        return DocumentSet.builder()
                .documents(documents)
                .build();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE)
                .append("documents", documents)
                .toString();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Item {
        private String title;
        private String type;

        private String url;
        private Double similarity;

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE)
                    .append("title", title)
                    .append("type", type)
                    .append("url", url)
                    .toString();
        }
    }
}
