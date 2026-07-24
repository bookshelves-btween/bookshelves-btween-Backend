package com.bookshelves.domain.notification.service;

import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.domain.member.repository.MemberRepository;
import com.bookshelves.domain.notification.entity.DeviceToken;
import com.bookshelves.domain.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationCommandService {

  private final DeviceTokenRepository deviceTokenRepository;
  private final MemberRepository memberRepository;

  public void registerFcmToken(Long memberId, String fcmToken) {
    Member member = memberRepository.getReferenceById(memberId);

    deviceTokenRepository
        .findByFcmToken(fcmToken)
        .ifPresentOrElse(
            deviceToken -> deviceToken.assignTo(member),
            () -> deviceTokenRepository.save(DeviceToken.create(member, fcmToken)));
  }
}
