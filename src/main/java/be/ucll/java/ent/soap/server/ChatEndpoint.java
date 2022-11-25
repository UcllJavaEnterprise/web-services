package be.ucll.java.ent.soap.server;

import be.ucll.java.ent.domain.ChatMessageDTO;
import be.ucll.java.ent.controller.MessageController;
import be.ucll.java.ent.soap.model.v1.ChatRequest;
import be.ucll.java.ent.soap.model.v1.ChatResponse;
import be.ucll.java.ent.soap.model.v1.STypeProcessOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
/**
 * Endpoint effectively dealing with the incoming SOAP message
 * delegating the further processing to the MessageProcessor
 */
public class ChatEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(ChatEndpoint.class);
    private static final Logger msgLogger = LoggerFactory.getLogger("messagelogger");

    @Autowired
    private MessageController msgProcessor;

    // Namespace URI exactly as in the targetNamespace attribute of the XSD

    @PayloadRoot(namespace = "http://ucll.be/java/ent/chat", localPart = "ChatRequest")
    @ResponsePayload
    public ChatResponse processMessage(@RequestPayload ChatRequest request) {
        // 1. Convert JAXB generated Request object into DTO for further internal processing
        ChatMessageDTO cm = new ChatMessageDTO(request.getMessage(), request.getSender());

        ChatResponse response = new ChatResponse();
        try {
            if (cm.getMessage() == null || cm.getMessage().trim().length() == 0) throw new IllegalArgumentException("Mandatory message missing");

            // 2. Log the incoming event and the data
            logger.debug("Received message '" + cm.getMessage() + "' from " + cm.getSender());
            msgLogger.info(cm.getSender() + " | " + cm.getMessage());

            // 3. Process the message (call Business Logic layer and pass the DTO on)
            msgProcessor.process(cm);

            // 4a. Prepare the JAXB generated Response. All processing went OK
            response.setCode(0);
            response.setType(STypeProcessOutcome.INFO);
        } catch (IllegalArgumentException e) {
            // 4b. Prepare the JAXB generated Response. Something went wrong. Inform the caller of the error
            response.setCode(1);
            response.setType(STypeProcessOutcome.ERROR);
            response.setFeedback(e.getMessage());
        } catch (Exception e) {
            logger.error("An unexpected exception occured", e);

            // 4c. Prepare the JAXB generated Response. Something went wrong. Inform the caller of the error
            response.setCode(1);
            response.setType(STypeProcessOutcome.ERROR);
            response.setFeedback("An unexpected exception occured");
        }

        // 5. Effectively return the SOAP Web-Service response
        return response;
    }

}
