package com.example.signaling.service;

import org.springframework.stereotype.Service;

import com.corundumstudio.socketio.SocketIOClient;
import com.example.signaling.dto.MessageDto;
import com.example.signaling.enums.MessageType;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SocketIOService {
    public void sendEvent(String room, String eventName, SocketIOClient senderClient, String message) {
        for (SocketIOClient client : senderClient.getNamespace().getRoomOperations(room).getClients()) {
            log.info("Client ID: {}", client.getSessionId());
            if (!client.getSessionId().equals(senderClient.getSessionId())) {
                client.sendEvent(eventName, new MessageDto(MessageType.SERVER, message));
            }
        }
    }
}
