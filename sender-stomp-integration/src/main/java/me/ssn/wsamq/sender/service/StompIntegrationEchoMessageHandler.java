package me.ssn.wsamq.sender.service;

import lombok.extern.slf4j.Slf4j;
import me.ssn.wsamq.dto.MessageOrigin;
import me.ssn.wsamq.dto.ServerMessageDto;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.integration.stomp.StompSessionManager;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StompIntegrationEchoMessageHandler implements EchoMessageHandler {

    private StompSession stompSession;

    private final StompSessionManager stompSessionManager;

    public StompIntegrationEchoMessageHandler(StompSessionManager stompSessionManager) {
        this.stompSessionManager = stompSessionManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() {
        stompSessionManager.connect(new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                StompIntegrationEchoMessageHandler.this.stompSession = session;
            }
        });
    }

    @Override
    public void handle(String username, String socketSessionId, String message) {
        if (stompSession != null && stompSession.isConnected()) {
            final ServerMessageDto echo = new ServerMessageDto(MessageOrigin.INTEGRATION, System.currentTimeMillis(),
                    message);
            stompSession.send("/exchange/amq.direct/echo.user-user" + username, echo);
            stompSession.send("/exchange/amq.direct/echo.window-user" + socketSessionId, echo);
        } else {
            log.warn("Session is not yet ready");
        }
    }

    @Override
    public void handleGlobal(String message) {
        if (stompSession != null && stompSession.isConnected()) {
            ServerMessageDto messageDto = new ServerMessageDto(MessageOrigin.INTEGRATION, System.currentTimeMillis(),
                    message);
            stompSession.send("/topic/global", messageDto);
        } else {
            log.warn("Session is not yet ready");
        }
    }

}
