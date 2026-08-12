package com.bookshelves.domain.notification.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookshelves.domain.notification.code.NotificationErrorCode;
import com.bookshelves.domain.notification.exception.NotificationException;
import com.bookshelves.domain.notification.service.NotificationCommandService;
import com.bookshelves.domain.notification.service.NotificationQueryService;
import com.bookshelves.global.exception.GeneralExceptionAdvice;
import com.bookshelves.global.security.AuthenticationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

class NotificationControllerValidationTest {

  private NotificationCommandService notificationCommandService;
  private NotificationQueryService notificationQueryService;
  private AuthenticationFacade authenticationFacade;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    notificationCommandService = mock(NotificationCommandService.class);
    notificationQueryService = mock(NotificationQueryService.class);
    authenticationFacade = mock(AuthenticationFacade.class);
    NotificationController controller =
        new NotificationController(
            notificationCommandService, notificationQueryService, authenticationFacade);
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    ProxyFactory proxyFactory = new ProxyFactory(controller);
    proxyFactory.setProxyTargetClass(true);
    proxyFactory.addAdvice(
        new MethodValidationInterceptor((jakarta.validation.Validator) validator));

    mockMvc =
        MockMvcBuilders.standaloneSetup(proxyFactory.getProxy())
            .setControllerAdvice(new GeneralExceptionAdvice())
            .build();
  }

  @Test
  void getNotificationsReturnsBadRequestWhenPageIsLessThanOne() throws Exception {
    mockMvc
        .perform(get("/api/v1/notifications").param("page", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.code").value("COMMON400_1"))
        .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
        .andExpect(jsonPath("$.result").isMap());

    verifyNoInteractions(notificationQueryService);
  }

  @Test
  void getNotificationsReturnsBadRequestWhenSizeExceedsMaximum() throws Exception {
    mockMvc
        .perform(get("/api/v1/notifications").param("size", "51"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.code").value("COMMON400_1"))
        .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
        .andExpect(jsonPath("$.result").isMap());

    verifyNoInteractions(notificationQueryService);
  }

  @Test
  void getNewNotificationsReturnsBadRequestWhenAfterIdIsNegative() throws Exception {
    mockMvc
        .perform(get("/api/v1/notifications/new").param("afterId", "-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.code").value("COMMON400_1"))
        .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
        .andExpect(jsonPath("$.result").isMap());

    verifyNoInteractions(notificationQueryService);
  }

  @Test
  void getNewNotificationsReturnsBadRequestWhenSizeExceedsMaximum() throws Exception {
    mockMvc
        .perform(get("/api/v1/notifications/new").param("afterId", "0").param("size", "51"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.code").value("COMMON400_1"))
        .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
        .andExpect(jsonPath("$.result").isMap());

    verifyNoInteractions(notificationQueryService);
  }

  @Test
  void registerFcmTokenReturnsKoreanValidationMessageWhenTokenIsBlank() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/notifications/fcm/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fcmToken\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.code").value("COMMON400_1"))
        .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
        .andExpect(jsonPath("$.result.fcmToken").value("FCM 토큰은 필수입니다."));

    verifyNoInteractions(notificationCommandService);
  }

  @Test
  void registerFcmTokenReturnsKoreanValidationMessageWhenTokenIsTooLong() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/notifications/fcm/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fcmToken\":\"" + "a".repeat(256) + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.code").value("COMMON400_1"))
        .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
        .andExpect(jsonPath("$.result.fcmToken").value("FCM 토큰은 255자 이하여야 합니다."));

    verifyNoInteractions(notificationCommandService);
  }

  @Test
  void deleteNotificationReturnsBadRequestWhenNotificationIdIsLessThanOne() throws Exception {
    mockMvc
        .perform(delete("/api/v1/notifications/0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.code").value("COMMON400_1"))
        .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
        .andExpect(jsonPath("$.result").isMap());

    verifyNoInteractions(notificationCommandService);
  }

  @Test
  void deleteNotificationReturnsNotFoundWhenNotificationIsMissingOrNotOwned() throws Exception {
    when(authenticationFacade.getCurrentMemberId()).thenReturn(1L);
    org.mockito.Mockito.doThrow(
            new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND))
        .when(notificationCommandService)
        .deleteNotification(101L, 1L);

    mockMvc
        .perform(delete("/api/v1/notifications/101"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.isSuccess").value(false))
        .andExpect(jsonPath("$.code").value("NOTI404_1"))
        .andExpect(jsonPath("$.message").value("존재하지 않는 알림입니다."))
        .andExpect(jsonPath("$.result").isMap());
  }
}
