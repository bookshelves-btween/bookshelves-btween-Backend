package com.bookshelves.domain.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bookshelves.domain.notification.service.NotificationCommandService;
import com.bookshelves.domain.notification.service.NotificationQueryService;
import com.bookshelves.global.security.AuthenticationFacade;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class NotificationControllerValidationTest {

  @Test
  void getNotificationsValidatesPageAndSize() throws NoSuchMethodException {
    NotificationController controller =
        new NotificationController(
            mock(NotificationCommandService.class),
            mock(NotificationQueryService.class),
            mock(AuthenticationFacade.class));
    Method method =
        NotificationController.class.getMethod("getNotifications", Integer.class, Integer.class);

    try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
      Validator validator = validatorFactory.getValidator();

      assertThat(
              validator
                  .forExecutables()
                  .validateParameters(controller, method, new Object[] {0, 51}))
          .hasSize(2);
      assertThat(
              validator
                  .forExecutables()
                  .validateParameters(controller, method, new Object[] {1, 20}))
          .isEmpty();
    }
  }
}
