package com.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configures async task executors used for fire-and-forget background work: -
 * shipmentTaskExecutor : shipment creation + Shiprocket API calls after payment -
 * razorpayTaskExecutor : Razorpay payment capture (best-effort, non-blocking)
 */
@Configuration
@EnableAsync
public class AsyncConfig {

	/**
	 * Dedicated thread pool for shipment creation tasks triggered after payment success.
	 * These involve multiple DB writes and outbound Shiprocket API calls that should not
	 * block the payment-verify HTTP response.
	 */
	@Bean(name = "shipmentTaskExecutor")
	public Executor shipmentTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(50);
		executor.setThreadNamePrefix("shipment-async-");
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(30);
		executor.initialize();
		return executor;
	}

	/**
	 * Dedicated thread pool for Razorpay payment capture calls. Capture is best-effort;
	 * failures are logged and do not affect order status.
	 */
	@Bean(name = "razorpayTaskExecutor")
	public Executor razorpayTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(5);
		executor.setQueueCapacity(20);
		executor.setThreadNamePrefix("razorpay-async-");
		executor.setWaitForTasksToCompleteOnShutdown(false);
		executor.initialize();
		return executor;
	}

}
