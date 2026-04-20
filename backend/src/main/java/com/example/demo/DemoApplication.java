package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // 👈 增加第 1 处：导入定时任务的包

/**
 * 启用Spring的定时任务功能
 * 通过添加@EnableScheduling注解来开启定时任务的总开关
 * 这样Spring容器才能识别并执行@Scheduled注解标记的方法
 */
@EnableScheduling // 👈 增加第 2 处：加上这个注解，正式开启定时任务总开关！
/**
 * Spring Boot应用程序的主启动类
 * @SpringBootApplication注解是一个复合注解，包含了@Configuration、@EnableAutoConfiguration和@ComponentScan
 * 这些注解共同作用，使得Spring Boot能够自动进行配置和组件扫描
 */
@SpringBootApplication
public class DemoApplication {
    /**
     * 程序的入口方法
     * @param args 命令行参数
     * 通过SpringApplication.run()方法来启动Spring Boot应用程序
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}