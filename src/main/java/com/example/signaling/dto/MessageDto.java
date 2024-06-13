package com.example.signaling.dto;

import com.example.signaling.enums.MessageType;
import com.example.signaling.enums.SignalType;

import lombok.Data;

@Data
public class MessageDto {
    private MessageType messageType;
    private SignalType signalType;
    private String message;
    private String room;
        
    public MessageDto() {
    }
    
    public MessageDto(String room) {
        this.room = room;
    }

    public MessageDto(MessageType messageType, String message) {
        this.messageType = messageType;
        this.message = message;
    }
}
