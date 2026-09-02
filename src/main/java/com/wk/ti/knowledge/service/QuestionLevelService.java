package com.wk.ti.knowledge.service;

import com.wk.ti.knowledge.entity.QuestionLevel;
import com.wk.ti.knowledge.repository.QuestionLevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@Service
@RequiredArgsConstructor
public class QuestionLevelService {
    private final QuestionLevelRepository questionLevelRepository;
    private List<QuestionLevel> questionLevels;

    public String levels() {
        if (questionLevels == null || questionLevels.isEmpty()) {
            questionLevels = questionLevelRepository.findAll();
        }
        return String.join(",", questionLevels.stream().map(QuestionLevel::getCode).toList());
    }

    public Optional<QuestionLevel> findByCode(String code) {
        return questionLevelRepository.findByCode(code);
    }

    public QuestionLevel getDefaultLevel() {
        return questionLevelRepository.findByCode("A1").get();
    }
}
