package com.wk.ti.question.generation.executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class QuestionGenerationAsyncConfig {

    @Bean(name = "questionGenerationExecutor")
    public Executor questionGenerationExecutor() {

        SimpleAsyncTaskExecutor executor =
                new SimpleAsyncTaskExecutor("question-generation-");

        executor.setVirtualThreads(true);
        executor.setConcurrencyLimit(4);

        return executor;
    }
}
