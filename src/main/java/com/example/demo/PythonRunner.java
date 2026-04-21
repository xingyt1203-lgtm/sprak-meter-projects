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
            // 统一以项目根目录作为工作目录，避免相对路径在不同启动方式下失效
            File projectRoot = new File(System.getProperty("user.dir"));
            File venvPython = new File(projectRoot, ".venv/bin/python");
            String pythonCmd = venvPython.exists() ? venvPython.getAbsolutePath() : "python3";

            // 优先使用项目虚拟环境解释器，不依赖手工 source activate
            ProcessBuilder pb = new ProcessBuilder(pythonCmd, "api.py");
            pb.directory(projectRoot);

            // 让 Python 的输出打印到 Java 的控制台里
            pb.inheritIO();

            // 启动
            pb.start();
            System.out.println("✅ Python AI 引擎已成功挂载！");
            System.out.println("ℹ️ Python 解释器: " + pythonCmd);

        } catch (Exception e) {
            System.out.println("❌ 唤醒 Python 引擎失败，请检查环境: " + e.getMessage());
        }
    }
}
