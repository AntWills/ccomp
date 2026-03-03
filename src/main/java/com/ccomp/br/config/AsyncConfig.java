package com.ccomp.br.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String VIRTUAL_TASK_EXECUTOR = "virtualTaskExecutor";

    @Bean(name = VIRTUAL_TASK_EXECUTOR)
    public Executor taskExecutor() {
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("vthread-", 0).factory()
        );
    }
}
