package com.wk.ti.rag.executor;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("deprecation")
@Configuration
public class AgentExecutorConfig {

    // Store executor reference to call shutdown
    private ExecutorService internalExecutor;

    @Bean(name = "agentExecutor")
    public ExecutorService agentExecutor(
            @Value("${agent.queue-capacity:500}") Integer queueCapacity) {
        int cores = Runtime.getRuntime().availableProcessors();
        int minThreads = Math.max(4, cores);           // tune to your environment
        int maxThreads = minThreads * 2;
        internalExecutor = new ThreadPoolExecutor(
                minThreads,
                maxThreads,
                30,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                runnable -> {
                    Thread t = new Thread(runnable);
                    t.setName("agent-worker-" + t.getId());
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
        return this.internalExecutor;
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (internalExecutor != null && !internalExecutor.isShutdown()) {
            internalExecutor.shutdown(); // Stop accepting new tasks
            try {
                // Wait for ongoing tasks to finish (max 60 seconds)
                if (!internalExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    internalExecutor.shutdownNow(); // Force shutdown if not terminated
                }
            } catch (InterruptedException ex) {
                internalExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}