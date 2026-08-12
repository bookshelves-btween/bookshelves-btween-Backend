package com.bookshelves.global.config;

import com.bookshelves.global.websocket.StompAuthChannelInterceptor;
import com.bookshelves.global.websocket.StompErrorFrameHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  // presence 판단을 위해 10초 간격으로 주고받는다.
  private static final long[] HEARTBEAT = {10000, 10000};

  private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
  private final StompErrorFrameHandler stompErrorFrameHandler;
  // 브로커용 스케줄러를 다른 TaskScheduler와 구분해 주입한다.
  private final ThreadPoolTaskScheduler webSocketTaskScheduler;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws-stomp").setAllowedOriginPatterns("*");
    // CONNECT·SUBSCRIBE 오류도 HTTP와 같은 ApiResponse 형식으로 전달한다.
    registry.setErrorHandler(stompErrorFrameHandler);
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry
        .enableSimpleBroker("/sub")
        .setHeartbeatValue(HEARTBEAT)
        .setTaskScheduler(webSocketTaskScheduler);
    registry.setApplicationDestinationPrefixes("/pub");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(stompAuthChannelInterceptor);
  }
}
