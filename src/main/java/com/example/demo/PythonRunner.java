package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.io.File;

@Component
public class PythonRunner implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🤖 [系统启动] 正在自动唤醒底层的 Python AI 引擎...");

        try {
            // 注意：确保 python 已安装且 api.py 在项目根目录下
            ProcessBuilder pb = new ProcessBuilder("python", "api.py");    

            // 让 Python 的输出打印到 Java 的控制台里
            pb.inheritIO();

            // 启动
            pb.start();
            System.out.println("✅ Python AI 引擎已成功挂载！");    

        } catch (Exception e) {
            System.out.println("❌ 唤醒 Python 引擎失败，请检查环境: " + e.getMessage());
        }
    }
}
