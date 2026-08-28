package com.wk.ti.rag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GenerateInterviewRequest(

        String question,

        @Min(1)
        @Max(20)
        Integer questionCount

) {

    public int questionCountOrDefault(int defaultValue) {
        return questionCount == null ? defaultValue : questionCount;
    }
}
