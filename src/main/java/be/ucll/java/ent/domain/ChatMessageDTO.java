package be.ucll.java.ent.domain;

public class ChatMessageDTO {

    private String message;
    private String sender;

    /* Constructors */

    public ChatMessageDTO() {
    }

    public ChatMessageDTO(String message, String sender) {
        this.message = message;
        this.sender = sender;
    }

    /* Getter and Setter */

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}

