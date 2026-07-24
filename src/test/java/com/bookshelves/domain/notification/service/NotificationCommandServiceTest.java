package com.bookshelves.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.enums.Provider;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.domain.notification.entity.DeviceToken;
import com.bookshelves.domain.notification.enums.Platform;
import com.bookshelves.domain.notification.repository.DeviceTokenRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationCommandServiceTest {

  private final DeviceTokenRepository deviceTokenRepository = mock(DeviceTokenRepository.class);
  private final MemberRepository memberRepository = mock(MemberRepository.class);
  private final NotificationCommandService notificationCommandService =
      new NotificationCommandService(deviceTokenRepository, memberRepository);

  @Test
  void registerFcmTokenCreatesIosDeviceToken() {
    Member member = Member.createSocialMember(Provider.APPLE, "apple-id");
    when(memberRepository.getReferenceById(1L)).thenReturn(member);
    when(deviceTokenRepository.findByFcmToken("fcm-token")).thenReturn(Optional.empty());

    notificationCommandService.registerFcmToken(1L, "fcm-token");

    ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
    verify(deviceTokenRepository).save(captor.capture());
    assertThat(captor.getValue().getMember()).isSameAs(member);
    assertThat(captor.getValue().getFcmToken()).isEqualTo("fcm-token");
    assertThat(captor.getValue().getPlatform()).isEqualTo(Platform.IOS);
  }

  @Test
  void registerFcmTokenAssignsExistingTokenToCurrentMember() {
    Member previousMember = Member.createSocialMember(Provider.KAKAO, "previous-id");
    Member currentMember = Member.createSocialMember(Provider.APPLE, "current-id");
    DeviceToken deviceToken = DeviceToken.create(previousMember, "fcm-token");
    when(memberRepository.getReferenceById(2L)).thenReturn(currentMember);
    when(deviceTokenRepository.findByFcmToken("fcm-token")).thenReturn(Optional.of(deviceToken));

    notificationCommandService.registerFcmToken(2L, "fcm-token");

    assertThat(deviceToken.getMember()).isSameAs(currentMember);
    verify(deviceTokenRepository, never()).save(deviceToken);
  }
}
