package com.example.signaling.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
    private static final Map<String, String> rooms = new HashMap<>();

    public SocketIOController(SocketIOServer server, SocketIOService service) throws Exception {
        this.server = server;
        this.service = service;
        server.addListeners(this);
        Future<Void> serverStart = server.startAsync();
        
        if (serverStart.isCancelled() || serverStart.cancel(true)) {
            // restart 3 kali
            int attempt = 1;
            while (attempt < Integer.MAX_VALUE) {
                log.info("Retry: {}", attempt);
                boolean restart = server.startAsync().await(1, TimeUnit.MINUTES);
                if (restart == true) break;
                else attempt++;
            }
        }
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
    public void onJoinRoom(SocketIOClient client, MessageDto payload) {
        String room = payload.getRoom();
        connectedClient = server.getRoomOperations(room).getClients().size();
        switch (connectedClient) {
            case 0:
                client.joinRoom(room);
                client.sendEvent(SignalType.CREATED.toString(), "Room " + room + " has been created!");
                log.info("Room " + room + " has been created!");
                log.info(clientId + " has joined room " + room);
                users.put(clientId, room);
                rooms.put(room, clientId);
                break;
            case 1:
                client.joinRoom(room);
                client.sendEvent(SignalType.JOINED.name(), 
                    new MessageDto(MessageType.SERVER, clientId + " has joined room " + room));
                log.info(clientId + " has joined room " + room);
                users.put(clientId, room);
                client.sendEvent(SignalType.SET_CALLER.name(), rooms.get(room));
                break;
            default:
                client.sendEvent(SignalType.FULL_ROOM.name(), 
                    new MessageDto(MessageType.SERVER, "Full room!"));
                log.info("Full room!");
                break;
        }
        log.info("{}: {}", SignalType.JOIN_ROOM.toString(), users);
        log.info("Room size: {}", connectedClient+1);
    }
    
    @OnEvent("LEAVE_ROOM")
    public void onLeaveRoom(SocketIOClient client, MessageDto payload) {
        client.leaveRoom(payload.getRoom());
        log.info(client.getSessionId() + " is left room " + payload.getRoom());
        log.info("{}: {}", SignalType.LEAVE_ROOM, client.getSessionId());
    }

    @OnEvent("SEND_MESSAGE")
    public void onSendMessage(SocketIOClient client, MessageDto payload) {
        service.sendEvent(payload.getRoom(), SignalType.GET_MESSAGE.toString(), client, payload.getMessage());
        log.info(client.getSessionId() + " send message: " + payload.getMessage());
        log.info("{}: {}", client.getSessionId(), payload.getMessage());
    }
    
    @OnEvent("READY")
    public void onReady(SocketIOClient client, MessageDto payload, AckRequest ackRequest) {
        client.getNamespace().getBroadcastOperations().sendEvent(SignalType.READY.name(), 
            payload.getRoom());
        log.info(client.getSessionId() + " ready in room " + payload.getRoom());
        log.info("{}: {}", SignalType.READY, client.getSessionId());
    }
    
    @OnEvent("RINGING")
    public void onRinging(SocketIOClient client, MessageDto payload) {
        String room = payload.getRoom();
        Object sdp = payload.getSdp(); // session description, adanya di client
        client.getNamespace().getRoomOperations(room).sendEvent(SignalType.RINGING.toString(), sdp);
        log.info(client.getSessionId() + " ringing!");
        log.info("{}: {}", SignalType.RINGING, client.getSessionId());
        log.info("sdp: {}", sdp);
    }
    
    @OnEvent("ANSWER")
    public void onAnswer(SocketIOClient client, MessageDto payload) {
        String room = payload.getRoom();
        Object sdp = payload.getSdp(); // session description, adanya di client
        client.getNamespace().getRoomOperations(room).sendEvent(SignalType.ANSWER.toString(), sdp);
        log.info(client.getSessionId() + " answered the call!");
        log.info("{}: {}", SignalType.ANSWER, client.getSessionId());
        log.info("sdp: {}", sdp);
    }
    
    @OnEvent("WAIT_ROOM")
    public void onWait(SocketIOClient client, MessageDto payload) {
        String room = payload.getRoom();
        client.getNamespace().getRoomOperations(room).sendEvent(SignalType.WAIT_ROOM.toString(), payload);
        log.info(client.getSessionId() + " waiting in room " + room);
        log.info("{}: {}", SignalType.WAIT_ROOM, client.getSessionId());
        log.info("Room: {}", room);
    }
}