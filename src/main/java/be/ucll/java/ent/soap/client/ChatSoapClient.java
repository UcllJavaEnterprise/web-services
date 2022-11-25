package be.ucll.java.ent.soap.client;

import be.ucll.java.ent.domain.ChatMessageDTO;
import be.ucll.java.ent.soap.model.v1.ChatRequest;
import be.ucll.java.ent.soap.model.v1.ChatResponse;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

@Component
public class ChatSoapClient extends WebServiceGatewaySupport {
    private String uri;

    /**
     * Constructor initializing the marchalling/unmarchalling engine
     *   while indicating where are the (generated) java classes to use.
     */
    public ChatSoapClient() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("be.ucll.java.ent.soap.model.v1"); // Base java package of Jaxb generated classes
        this.setMarshaller(marshaller);
        this.setUnmarshaller(marshaller);
    }

    /**
     * Effectively send out the chat message to the SOAP Endpoint which listens on the URI/URL
     * @param cm The ChatMessage to process
     * @return The received response from the SOAP Endpoint
     */
    public String sendAndReceiveMessage(ChatMessageDTO cm) {
        // Prepare the Chat request using the generate jaxb classes
        ChatRequest request = new ChatRequest();
        request.setMessage(cm.getMessage());
        request.setSender(cm.getSender());

        // Send request and receive response
        ChatResponse response = (ChatResponse) getWebServiceTemplate().marshalSendAndReceive(uri, request);

        // If everything is OK return nothing, otherwise return the error message
        if (response.getCode() == 0) {
            return null;
        } else {
            return response.getFeedback();
        }
    }

    /* Getter and Setter */

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
