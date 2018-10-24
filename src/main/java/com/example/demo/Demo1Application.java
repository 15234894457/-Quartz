package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class Demo1Application {

	/**
	 * logback
	 */
	private static Logger logger = LoggerFactory.getLogger(Demo1Application.class);

	public static void main(String[] args) {

		SpringApplication.run(Demo1Application.class, args);

		logger.info("【【【【【【定时任务分布式节点 - quartz-cluster-node-first 已启动】】】】】】");
	}
}
