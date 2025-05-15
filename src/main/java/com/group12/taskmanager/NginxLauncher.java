package com.group12.taskmanager;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class NginxLauncher {

    @PostConstruct
    public void startNginx() {
        try {
            String nginxPath = "C:\\nginx-1.27.5\\nginx.exe";

            ProcessBuilder builder = new ProcessBuilder(nginxPath);
            builder.directory(new File("C:\\nginx-1.27.5"));

            builder.start();
            System.out.println("✅ NGINX lanzado correctamente.");
        } catch (IOException e) {
            System.err.println("❌ No se pudo iniciar NGINX: " + e.getMessage());
        }
    }

    @PreDestroy
    public void stopNginx() {
        try {
            new ProcessBuilder("taskkill", "/F", "/IM", "nginx.exe").start();
            System.out.println("🛑 NGINX detenido.");
        } catch (IOException e) {
            System.err.println("❌🛑 Error al detener NGINX: " + e.getMessage());
        }
    }
}
