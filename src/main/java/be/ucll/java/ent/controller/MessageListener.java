package be.ucll.java.ent.controller;

import be.ucll.java.ent.domain.ChatMessageDTO;

public interface MessageListener {
    void messageReceived(ChatMessageDTO message);
}
