package com.study.flashsale.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {

    @Bean("orderTaskExecutor")
    public ThreadPoolTaskExecutor orderTaskExecutor(FlashSaleProperties flashSaleProperties) {
        FlashSaleProperties.ThreadPool threadPoolProperties = flashSaleProperties.getThreadPool();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(threadPoolProperties.getOrderCoreSize());
        executor.setMaxPoolSize(threadPoolProperties.getOrderMaxSize());
        executor.setQueueCapacity(threadPoolProperties.getOrderQueueCapacity());
        executor.setKeepAliveSeconds(threadPoolProperties.getOrderKeepAliveSeconds());
        executor.setThreadNamePrefix(threadPoolProperties.getOrderThreadNamePrefix());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

        executor.initialize();
        return executor;
    }
}
