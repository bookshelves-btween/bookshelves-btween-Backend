package com.bookshelves.domain.notification.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bookshelves.domain.notification.service.NotificationCommandService;
import com.bookshelves.domain.notification.service.NotificationQueryService;
import com.bookshelves.global.exception.GeneralExceptionAdvice;
import com.bookshelves.global.security.AuthenticationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

class NotificationControllerValidationTest {

  private NotificationQueryService notificationQueryService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    notificationQueryService = mock(NotificationQueryService.class);
    NotificationController controller =
        new NotificationController(
            mock(NotificationCommandService.class),
            notificationQueryService,
            mock(AuthenticationFacade.class));
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
}
