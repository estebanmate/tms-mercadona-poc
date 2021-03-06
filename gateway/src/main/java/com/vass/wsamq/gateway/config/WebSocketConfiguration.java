package me.ssn.wsamq.gateway.config;

import lombok.AllArgsConstructor;
import me.ssn.wsamq.common.StompBrokerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@AllArgsConstructor
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private final String STOMP_HEADER_USE_SOCKET_SESSION = "useSocketSession";

    private final StompBrokerProperties stompBrokerProperties;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/sockjs")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableStompBrokerRelay("/topic", "/exchange")
                // remove noise from heartbeats for debug purposes
                .setSystemHeartbeatSendInterval(0)
                .setSystemHeartbeatReceiveInterval(0)
                .setRelayHost(stompBrokerProperties.getHost())
                .setRelayPort(stompBrokerProperties.getPort())
                .setSystemLogin(stompBrokerProperties.getUser())
                .setSystemPasscode(stompBrokerProperties.getPassword())
                .setClientLogin(stompBrokerProperties.getUser())
                .setClientPasscode(stompBrokerProperties.getPassword())
                .setVirtualHost(stompBrokerProperties.getVirtualHost())
                .setUserRegistryBroadcast("/topic/simp-user-registry");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {

        // Extract sockjs sessionId, and use it as a Principal name, in order to target a particular web socket session
        // rather than all web socket sessions for a username when sending to "user" destinations with
        // simpMessagingTemplate
        registration.interceptors(new ChannelInterceptor() {

            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {

                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {

                    List<String> useWindowSession = accessor.getNativeHeader(STOMP_HEADER_USE_SOCKET_SESSION);
                    if (useWindowSession != null && useWindowSession.size() > 0) {
                        // Extract sockjs sessionId, and use it as a Principal name, in order to target a particular web
                        // socket session rather than all web socket sessions for a username when sending to "user"
                        // destinations with simpMessagingTemplate

                        Principal user = accessor.getUser();
                        if (user != null) {
                            String socketSessionId = accessor.getSessionId();
                            SocketDestinationPrincipal customUser = new SocketDestinationPrincipal(
                                    user.getName(), socketSessionId);
                            accessor.setUser(customUser);
                        }
                    }
                } else if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    // Configure some RabbitMQ Stomp specific stuff
                    // delete when the last downstream consumers for a queue goes away
                    accessor.setNativeHeader("auto-delete", "true");
                    // delete when declaring connection closes
                    accessor.setNativeHeader("exclusive", "true");
                    // How long a message published to a queue can live before it is discarded (milliseconds).
                    accessor.setNativeHeader("x-message-ttl", "3000");
                    // How long a queue can be unused for before it is automatically deleted (milliseconds).
                    accessor.setNativeHeader("x-expires", String.valueOf(20 * 60 * 1000));
                }

                return message;
            }
        });
    }

    // @Bean
    // public MessageConverter amqpMessageConverter() {
    //     return new Jackson2JsonMessageConverter();
    // }

    // uncomment for echo.user routes to work with RabbitTemplate and StompSession senders
    // though simpMessagingTemplate won't send to "username" destinations for /exchange/amq.direct/echo.user destination
    @Bean
    public UsernameUserDestinationResolver userDestinationResolver(SimpUserRegistry simpUserRegistry) {
        return new UsernameUserDestinationResolver(simpUserRegistry);
    }
}
