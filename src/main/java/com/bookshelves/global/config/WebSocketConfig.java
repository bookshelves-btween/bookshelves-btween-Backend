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

  // presence 판단용 heart-beat 주기(ms) — 클라이언트와 10초 간격으로 주고받는다
  private static final long[] HEARTBEAT = {10000, 10000};

  private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
  private final StompErrorFrameHandler stompErrorFrameHandler;
  // 필드명이 빈 이름(webSocketTaskScheduler)과 일치 → 브로커 자체 스케줄러와 구분해 주입
  private final ThreadPoolTaskScheduler webSocketTaskScheduler;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws-stomp").setAllowedOriginPatterns("*");
    // CONNECT·SUBSCRIBE 실패를 ERROR 프레임에 ApiResponse envelope로 실어 보낸다.
    // 기본 핸들러는 예외 메시지 원문을 그대로 내보내 HTTP 응답과 형식이 어긋난다.
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
