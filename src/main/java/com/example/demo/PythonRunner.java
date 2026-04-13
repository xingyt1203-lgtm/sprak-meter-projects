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
            // 这里写你的 api.py 所在的绝对路径或相对路径
            // 注意：如果你的 api.py 跟你运行 Java 的地方不在同一个目录，请把 "api.py" 换成完整路径
            ProcessBuilder pb = new ProcessBuilder("python3", "api.py");
            
            // 如果需要指定运行的目录，可以取消下面这行的注释并修改路径
            // pb.directory(new File("/绝对路径/指向/你放 api.py 的文件夹"));

            // 让 Python 的输出打印到 Java 的控制台里
            pb.inheritIO(); 
            
            // 启动！
            pb.start();
            System.out.println("✅ Python AI 引擎已成功挂载！");
            
        } catch (Exception e) {
            System.out.println("❌ 唤醒 Python 引擎失败，请检查环境: " + e.getMessage());
        }
    }
}
