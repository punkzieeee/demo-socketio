package com.example.signaling.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOServer;
import jakarta.annotation.PreDestroy;

@org.springframework.context.annotation.Configuration
public class SocketIOConfig {
    
    @Value("${socket.host}")
    private String SOCKETHOST;
    @Value("${socket.port}")
    private int SOCKETPORT;
    private SocketIOServer server;

    @Bean
    public SocketIOServer socketIOServer() {
        SocketConfig config = new SocketConfig();
        config.setTcpKeepAlive(true);
        config.setReuseAddress(true);

        Configuration configuration = new Configuration();
        configuration.setHostname(SOCKETHOST);
        configuration.setPort(SOCKETPORT);
        configuration.setPingTimeout(600000);
        configuration.setSocketConfig(config);
        
        server = new SocketIOServer(configuration);       
        return server;
    }

    @PreDestroy
    public void stopSocketIOServer() {
        this.server.stop();
    }
}
