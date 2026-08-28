package com.wk.ti.api.executor;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class AgentExecutorConfig {

    // Store executor reference to call shutdown
    private ExecutorService agentExecutor;

    @Bean(name = "agentExecutor")
    public ExecutorService agentExecutor(
            @Value("${agent.queue-capacity:500}") Integer queueCapacity) {
        int cores = Runtime.getRuntime().availableProcessors();
        int minThreads = Math.max(4, cores);           // tune to your environment
        int maxThreads = minThreads * 2;
        agentExecutor = new ThreadPoolExecutor(
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
        return this.agentExecutor;
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (agentExecutor != null && !agentExecutor.isShutdown()) {
            agentExecutor.shutdown(); // Stop accepting new tasks
            try {
                // Wait for ongoing tasks to finish (max 60 seconds)
                if (!agentExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    agentExecutor.shutdownNow(); // Force shutdown if not terminated
                }
            } catch (InterruptedException ex) {
                agentExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}