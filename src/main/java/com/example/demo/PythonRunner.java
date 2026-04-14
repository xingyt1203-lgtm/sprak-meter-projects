package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.io.File;
import java.util.concurrent.TimeUnit;

@Component
public class PythonRunner implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        System.out.println(" [系统启动] 正在自动唤醒 Python AI 引擎...");
        
        try {
            // 1. 指定 Python 路径和脚本
            ProcessBuilder pb = new ProcessBuilder("python3", "api.py");
            
            // 2. 设置工作目录：这确保了 api.py 能找到同文件夹下的其它文件
            pb.directory(new File("/root/spark-meter-backend/python_scripts/"));

            // 3. 让 Python 的报错和日志直接显示在 Java 控制台
            pb.inheritIO(); 
            
            // 4. 异步启动
            pb.start();

            //  关键步骤：等待 Python 服务完全启动
            // 给它 5 秒钟缓冲时间，确保 Uvicorn 端口 8000 已经监听
            System.out.println(" 正在等待 Python 服务初始化 (约 5 秒)...");
            Thread.sleep(10000); 

            System.out.println(" Python AI 引擎已成功挂载");
            
        } catch (Exception e) {
            System.out.println(" 唤醒 Python 引擎失败，请检查环境: " + e.getMessage());
        }
    }
}