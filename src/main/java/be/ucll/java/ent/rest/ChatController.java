package be.ucll.java.ent.rest;

import be.ucll.java.ent.domain.ChatMessageDTO;
import be.ucll.java.ent.controller.MessageController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Test URL for swagger documentation: http://localhost:8180/chat/swagger-ui.html
 */

@RestController
@RequestMapping("/rest")
@Api(description = "Via de onderstaande service is het mogelijk een Chat message te ontvangen.", tags = "Chat")
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private static final Logger msgLogger = LoggerFactory.getLogger("messagelogger");

    private static final String REST_URL_ENDPOINT = "v1/chatreceiver";

    @Autowired
    private MessageController msgProcessor;

    @ApiOperation(value = "Een ontvangen chat bericht verwerken", notes = "Dit is een service die via het POST protocol een chatmessage ontvangt en verwerkt.")
    @RequestMapping(value = REST_URL_ENDPOINT, method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity processMessage(@RequestBody ChatMessageDTO cm) {
        // 1. Log the incoming event and the data
        if (cm != null && cm.getMessage() != null && cm.getSender() != null){
            logger.debug("Received (http-post) message '" + cm.getMessage() + "' from " + cm.getSender());
            msgLogger.info(cm.getSender() + " | " + cm.getMessage());
        } else {
            return new ResponseEntity("{\"error\": \"Input ontbreekt\"}", HttpStatus.BAD_REQUEST);
        }

        try {
            // 2. Process the message (call Business Logic layer and pass the DTO on)
            msgProcessor.process(cm);

            return new ResponseEntity(cm, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("An unexpected exception occured", e);
            return new ResponseEntity("{\"error\": \"Algemene fout\"}", HttpStatus.BAD_REQUEST);
        }
    }
}
