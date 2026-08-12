package com.bookshelves.domain.meeting.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bookshelves.domain.meeting.dto.request.MeetingCreateReqDTO;
import com.bookshelves.domain.meeting.service.MeetingCommandService;
import com.bookshelves.domain.meeting.service.MeetingQueryService;
import com.bookshelves.global.util.ServiceTime;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class MeetingControllerValidationTest {

  @Test
  void validatesMeetingCreationRules() {
    LocalDateTime validStart = ServiceTime.now().plusHours(8);

    try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
      Validator validator = validatorFactory.getValidator();

      assertThat(validator.validate(requestAt(validStart, 3, 5))).isEmpty();
      assertThat(validator.validate(requestAt(validStart, 6, 60))).isEmpty();
      assertThat(validator.validate(requestAt(validStart, 2, 30))).isNotEmpty();
      assertThat(validator.validate(requestAt(validStart, 7, 30))).isNotEmpty();
      assertThat(validator.validate(requestAt(validStart, 4, 4))).isNotEmpty();
      assertThat(validator.validate(requestAt(validStart, 4, 61))).isNotEmpty();
      assertThat(validator.validate(requestAt(validStart, 4, 32))).isNotEmpty();
      assertThat(validator.validate(requestAt(ServiceTime.now().plusHours(6), 4, 30))).isNotEmpty();
    }
  }

  private MeetingCreateReqDTO requestAt(LocalDateTime start, int maxParticipants, int duration) {
    return new MeetingCreateReqDTO(
        "9788966262281",
        start.toLocalDate(),
        start.toLocalTime().withSecond(0).withNano(0).toString(),
        maxParticipants,
        duration);
  }

  @Test
  void inheritedParameterConstraintsDoNotConflict() throws NoSuchMethodException {
    MeetingController controller =
        new MeetingController(mock(MeetingCommandService.class), mock(MeetingQueryService.class));
    Method searchMethod =
        MeetingController.class.getMethod(
            "searchMeetings", String.class, Integer.class, Integer.class);
    Method createMethod =
        MeetingController.class.getMethod("createMeeting", MeetingCreateReqDTO.class);
    Method myMeetingsMethod =
        MeetingController.class.getMethod(
            "getMyMeetings",
            boolean.class,
            Integer.class,
            Integer.class,
            Integer.class,
            Integer.class);
    MeetingCreateReqDTO request =
        new MeetingCreateReqDTO("9788966262281", LocalDate.now().plusDays(1), "20:00", 4, 60);
    MeetingCreateReqDTO invalidRequest = new MeetingCreateReqDTO(null, null, null, null, null);

    try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
      Validator validator = validatorFactory.getValidator();

      assertThat(
              validator
                  .forExecutables()
                  .validateParameters(controller, searchMethod, new Object[] {null, 0, 51}))
          .hasSize(3);
      assertThat(
              validator
                  .forExecutables()
                  .validateParameters(controller, createMethod, new Object[] {request}))
          .isEmpty();
      assertThat(
              validator
                  .forExecutables()
                  .validateParameters(controller, createMethod, new Object[] {invalidRequest}))
          .hasSize(5);
      assertThat(
              validator
                  .forExecutables()
                  .validateParameters(
                      controller, myMeetingsMethod, new Object[] {true, 2026, 13, 0, 51}))
          .hasSize(3);
    }
  }
}
