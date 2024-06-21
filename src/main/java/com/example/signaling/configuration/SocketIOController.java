package com.example.signaling.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.example.signaling.dto.MessageDto;
import com.example.signaling.enums.MessageType;
import com.example.signaling.enums.SignalType;
import com.example.signaling.service.SocketIOService;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SocketIOController {

    @Autowired
    private SocketIOService service;
    
    private final SocketIOServer server;
    private int connectedClient;
    private String clientId;
    private static final Map<String, String> users = new HashMap<>();

    public SocketIOController(SocketIOServer server, SocketIOService service) throws Exception {
        this.server = server;
        this.service = service;
        server.addListeners(this);
        server.start();
    }

    @OnConnect
    public void onConnect(SocketIOClient client) {
        clientId = client.getSessionId().toString();
        log.info("Client connected: {}", clientId);
        users.put(clientId, "");
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        clientId = client.getSessionId().toString();
        String room = users.get(clientId);
        if (!Objects.isNull(room)) {
            log.info("Client disconnected: {}", clientId);
            users.remove(clientId);
            client.getNamespace().getRoomOperations(room).sendEvent(SignalType.DISCONNECTED.toString(), 
                clientId);
        }
    }

    @OnEvent("JOIN_ROOM")
    public void onJoinRoom(SocketIOClient client, MessageDto payload, AckRequest ackRequest) {
        String room = payload.getRoom();
        connectedClient = server.getRoomOperations(room).getClients().size();
        switch (connectedClient) {
            case 0:
                client.joinRoom(room);
                ackRequest.sendAckData("Room " + room + " has been created!");
                log.info("Room " + room + " has been created!");
                log.info(clientId + " has joined room " + room);
                users.replace(clientId, room);
                break;
            case 1:
                client.joinRoom(room);
                ackRequest.sendAckData(clientId + " has joined room " + room);
                client.sendEvent(SignalType.JOINED.name(), 
                    new MessageDto(MessageType.SERVER, clientId + " has joined room " + room));
                log.info(clientId + " has joined room " + room);
                users.replace(clientId, room);
                break;
            default:
                ackRequest.sendAckData("Room " + room + " has already full!");
                client.leaveRoom(room);
                log.info("Full room!");
                break;
        }
        log.info("{}: {}", SignalType.JOIN_ROOM.toString(), users);
        log.info("Room size: {}", connectedClient+1);
    }
    
    @OnEvent("LEAVE_ROOM")
    public void onLeaveRoom(SocketIOClient client, MessageDto payload, AckRequest ackRequest) {
        client.leaveRoom(payload.getRoom());
        users.replace(clientId, "");
        ackRequest.sendAckData("You have leave room " + payload.getRoom());
        log.info(client.getSessionId() + " is left room " + payload.getRoom());
        log.info("{}: {}", SignalType.LEAVE_ROOM, client.getSessionId());
    }

    @OnEvent("SEND_MESSAGE")
    public void onSendMessage(SocketIOClient client, MessageDto payload, AckRequest ackRequest) {
        if (users.get(clientId).equals("")) {
            ackRequest.sendAckData(new MessageDto(MessageType.SERVER, "You need to join any room first!"));
            log.info("ID {} need to join any room first!", client.getSessionId());
        } else {
            service.sendEvent(payload.getRoom(), SignalType.GET_MESSAGE.toString(), client, payload.getMessage());
            ackRequest.sendAckData("Message sent!");
            log.info(client.getSessionId() + " send message: " + payload.getMessage());
            log.info("{}: {}", client.getSessionId(), payload.getMessage());
        }
    }

    @OnEvent("ACK_EVENT")
    public void demoAckEvent(SocketIOClient client, AckRequest ackRequest) {
        String str = client.getHandshakeData().getSingleUrlParam("angka");
        if (str.matches("[0-9]+")) {
            int angka = Integer.parseInt(str);
            String gage = angka % 2 == 0 ? "genap" : "ganjil";
            ackRequest.sendAckData("Angka " + gage);
        } else {
            ackRequest.sendAckData("Isi param bukan angka!");
        }
    }
}