package com.wk.ti.rag.dto;

import java.util.List;

public record InterviewQuestionResponse(
        List<InterviewQuestion> questions
) {
}
