package com.wk.ti.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.wk.ti.rag.dto.InterviewQuestionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.stream.Collectors;

import static java.lang.String.format;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentAgentResponse {
    private static final String FALLBACK_MESSAGE_TEMPLATE = "Agent [%s] processed question: [%d]. %s";

    private String conversationId;
    private Long questionId;
    private String termList;
    private AgentType agentType = AgentType.DOCUMENT;
    private SourceSet sourceSet;
    private DocumentSet documentSet;
    private String summary;

    @JsonIgnore
    public static String toSummary(InterviewQuestionResponse response) {
        return response.questions().stream()
                .map(q -> q.number() + "."
                        + q.question()
                        + "\nAnswer:"
                        + q.answer())
                .collect(Collectors.joining("\n\n"));
    }

    @JsonIgnore
    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE)
                .append("conversationId", conversationId)
                .append("questionId", questionId)
                .append("termList", termList)
                .append("agentType", agentType)
                .append("sourceSet", sourceSet)
                .append("summary", summary)
                .toString();
    }

    @JsonIgnore
    public static DocumentAgentResponse fallback(String conversationId, Long questionId, String errorCause) {
        return DocumentAgentResponse.builder()
                .conversationId(conversationId)
                .questionId(questionId)
                .termList(null)
                .agentType(AgentType.DOCUMENT)
                .sourceSet(SourceSet.builder().build())
                .documentSet(DocumentSet.builder().build())
                .summary(format(FALLBACK_MESSAGE_TEMPLATE, AgentType.DOCUMENT, questionId, errorCause))
                .build();
    }

    public enum AgentType {
        DOCUMENT
    }
}
