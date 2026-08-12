package com.bookshelves.domain.chat.entity;

import com.bookshelves.domain.member.entity.Member;
import com.bookshelves.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

  public static final int MAX_MESSAGE_LENGTH = 500;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "chatroom_id", nullable = false)
  private ChatRoom chatRoom;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_member_id", nullable = false)
  private Member senderMember;

  // 요청 DTO의 최대 길이와 동일하게 유지한다.
  @Column(name = "message", nullable = false, length = MAX_MESSAGE_LENGTH)
  private String message;

  @Builder
  private ChatMessage(ChatRoom chatRoom, Member senderMember, String message) {
    this.chatRoom = chatRoom;
    this.senderMember = senderMember;
    this.message = message;
  }
}
