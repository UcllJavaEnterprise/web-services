package be.ucll.java.ent.controller;

import be.ucll.java.ent.domain.ChatMessageDTO;
import com.vaadin.flow.spring.annotation.SpringComponent;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringComponent
/**
 * The Message processor holds a list of Java classes interested on being informed when a new message arrived
 * When a new message arrives the entire list is informed one after the other.
 */
public class MessageController implements Serializable {
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private LinkedList<MessageListener> listeners = new LinkedList();

    public void register(MessageListener lnr) {
        listeners.add(lnr);
    }

    public void unregister(MessageListener lnr) {
        listeners.remove(lnr);
    }

    public void process(final ChatMessageDTO cm) {
        for (MessageListener lnr : listeners) {
            executorService.execute(() -> lnr.messageReceived(cm));
        }
    }

}
