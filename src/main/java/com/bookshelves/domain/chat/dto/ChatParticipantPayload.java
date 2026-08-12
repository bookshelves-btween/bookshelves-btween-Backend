package com.bookshelves.domain.chat.dto;

// 접속 인원 변동을 전달하는 PARTICIPANT 프레임 데이터.
public record ChatParticipantPayload(
    String event, String nickname, int connected, int requiredVotes, int currentVotes) {}
