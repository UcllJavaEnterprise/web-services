package be.ucll.java.ent.views;

import be.ucll.java.ent.domain.ChatMessageDTO;
import be.ucll.java.ent.controller.MessageListener;
import be.ucll.java.ent.controller.MessageController;
import be.ucll.java.ent.soap.client.ChatSoapClient;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;

@Route("main")
@RouteAlias("")
@Push
public class ChatView extends VerticalLayout implements MessageListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String REST_URL_ENDPOINT = "rest/v1/chatreceiver";
    private static final String SOAP_URL_ENDPOINT = "soap/v1/chatreceiver";

    @Autowired
    private MessageController msgProcessor;

    @Autowired
    private MessageSource labelTexts;

    @Autowired
    private ChatSoapClient csc;

    @Value("${server.port}")
    private String port;

    @Value("${server.servlet.contextPath}")
    private String contextPath;

    private HorizontalLayout hl;
    private TextField txtName;
    private TextField txtIPAddress;
    private RadioButtonGroup<String> rbg;

    private SplitLayout sl;

    private VerticalLayout vl1;
    private TextArea taMessage;
    private Button btnSend;

    private VerticalLayout vl2;
    private Label lblMessages;
    private ArrayList<ChatMessageDTO> messages;
    private Grid<ChatMessageDTO> grdMessages;
    private Button btnReset;

    public ChatView() {
        // Default constructor
        // All initialisation moved to @PostConstruct to give Spring a chance to initialize all the Beans.
    }

    @PostConstruct
    private void ChatViewPostConstruct() {
        sl = new SplitLayout();
        sl.setSizeFull();

        vl1 = new VerticalLayout();

        hl = new HorizontalLayout();
        txtName = new TextField(labelTexts.getMessage("lbl.name", null, getLocale()));
        txtName.setAutofocus(true);
        txtIPAddress = new TextField(labelTexts.getMessage("lbl.server.ip", null, getLocale()));
        txtIPAddress.setValue("127.0.0.1");
        txtIPAddress.setRequiredIndicatorVisible(true);
        txtIPAddress.addValueChangeListener(event -> handleValueChanged());

        rbg = new RadioButtonGroup<>();
        rbg.setLabel(labelTexts.getMessage("lbl.delivery", null, getLocale()));
        rbg.setItems("rest", "soap");
        rbg.setValue("rest"); // Default

        hl.add(txtName, txtIPAddress, rbg);

        taMessage = new TextArea(labelTexts.getMessage("lbl.message", null, getLocale()));
        taMessage.setHeight("200px");
        taMessage.setWidthFull();
        taMessage.setValueChangeMode(ValueChangeMode.EAGER);
        taMessage.setRequiredIndicatorVisible(true);
        taMessage.addValueChangeListener(event -> handleValueChanged());
        btnSend = new Button(labelTexts.getMessage("lbl.send", null, getLocale()));
        btnSend.setEnabled(false);
        btnSend.addClickListener(this::handleSendButtonClick);

        vl1.add(hl, taMessage, btnSend);
        vl1.setWidth("40%");
        sl.addToPrimary(vl1);

        vl2 = new VerticalLayout();

        lblMessages = new Label(labelTexts.getMessage("lbl.table", null, getLocale()));

        grdMessages = new Grid<>();
        grdMessages.addColumn(ChatMessageDTO::getSender).setHeader(labelTexts.getMessage("lbl.sender", null, getLocale())).setWidth("20%");
        grdMessages.addColumn(new ComponentRenderer<>(msg -> {
            TextArea tmp = new TextArea();
            tmp.setValue(msg.getMessage());
            tmp.setReadOnly(true);
            tmp.setSizeFull();
            return tmp;
        })).setHeader(labelTexts.getMessage("lbl.message", null, getLocale())).setWidth("80%");
        // Initialize the list of messages
        messages = new ArrayList<>();
        grdMessages.setItems(messages);

        btnReset = new Button(labelTexts.getMessage("lbl.reset", null, getLocale()));
        btnReset.addClickListener(event -> handleResetButtonClick(event));

        vl2.add(lblMessages, grdMessages, btnReset);
        vl2.setWidth("60%");

        sl.addToSecondary(vl2);

        add(sl);

        setSizeFull();
    }

    private void handleSendButtonClick(ClickEvent<Button> buttonClickEvent) {
        // Prepare message create and fill pojo
        ChatMessageDTO msg = new ChatMessageDTO();
        if (txtName.getValue() != null && txtName.getValue().trim().length() > 0) {
            msg.setSender(txtName.getValue().trim());
        } else {
            msg.setSender("Anoniem");
        }
        msg.setMessage(taMessage.getValue().trim());

        // Decide to use REST or SOAP as Web Service
        if (rbg.getValue() != null && rbg.getValue().equalsIgnoreCase("rest")) {
            RestTemplate rt = new RestTemplate();
            rt.getMessageConverters().add(new StringHttpMessageConverter());

            // Build up REST URI
            String uri = "http://" + txtIPAddress.getValue().trim() + ":" + port + contextPath + "/" + REST_URL_ENDPOINT;
            try {
                ChatMessageDTO response = rt.postForObject(uri, msg, ChatMessageDTO.class);
                if (response != null) {
                    Notification.show("Bericht is succesvol afgeleverd via REST", 3000, Notification.Position.TOP_CENTER);
                    taMessage.setValue("");
                    taMessage.focus();
                }
            } catch (HttpClientErrorException | HttpServerErrorException ex) {
                // Error handling
                logger.error(ex.getMessage(), ex);
                if (HttpStatus.BAD_REQUEST.equals(ex.getStatusCode())) {
                    Notification.show("Het bericht kon niet worden afgeleverd.", 5000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
                } else {
                    Notification.show(ex.getMessage(), 5000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        } else if (rbg.getValue() != null && rbg.getValue().equalsIgnoreCase("soap")) {
            // Build up SOAP URI
            String uri = "http://" + txtIPAddress.getValue().trim() + ":" + port + contextPath + "/" + SOAP_URL_ENDPOINT;
            csc.setUri(uri);
            String errorMsg = csc.sendAndReceiveMessage(msg);
            if (errorMsg == null) {
                Notification.show("Bericht is succesvol afgeleverd via SOAP", 3000, Notification.Position.TOP_CENTER);
                taMessage.setValue("");
                taMessage.focus();
            } else {
                logger.error(errorMsg);
                Notification.show("Het bericht kon niet worden afgeleverd: " + errorMsg, 5000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }

    private void handleResetButtonClick(ClickEvent<Button> buttonClickEvent) {
        // Request confirmation in popup dialog before effective cleanup

        VerticalLayout vl = new VerticalLayout();
        vl.setPadding(false);

        Dialog dialog = new Dialog(vl);
        dialog.setCloseOnEsc(false);
        dialog.setCloseOnOutsideClick(false);

        // The warning message requesting confirmation
        Label lblWarningMsg = new Label("Alle berichten verwijderen?");

        // The Yes/No buttons and its click event listeners
        HorizontalLayout hl = new HorizontalLayout();
        hl.setJustifyContentMode(JustifyContentMode.EVENLY);
        hl.setAlignItems(Alignment.BASELINE);

        Button btnYes = new Button("Ja", event -> {
            // Re-initialise the app. Remove the chat messages
            messages = new ArrayList<>();
            grdMessages.setItems(messages);

            dialog.close();
        });

        Button btnNo = new Button("Nee", event -> {
            dialog.close();
        });

        hl.add(btnYes, btnNo);

        vl.add(lblWarningMsg, hl);

        dialog.open();
    }

    private void handleValueChanged() {
        if (txtIPAddress.getValue() != null && txtIPAddress.getValue().trim().length() > 0
                && taMessage.getValue() != null && taMessage.getValue().trim().length() > 0) {
            btnSend.setEnabled(true);
        } else {
            btnSend.setEnabled(false);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        msgProcessor.register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        msgProcessor.unregister(this);
        super.onDetach(detachEvent);
    }

    @Override
    public void messageReceived(ChatMessageDTO cm) {
        // Must lock the Vaadin session to execute logic safely via the 'access' method
        getUI().get().access((Command) () -> {
            messages.add(cm);
            grdMessages.setItems(messages);
        });
    }

}