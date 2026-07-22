package com.bookshelves.domain.chat.dto;

// PARTICIPANT 프레임 — 접속 인원(presence) 변동. event: JOINED | LEFT
public record ChatParticipantPayload(
    String event, String nickname, int connected, int requiredVotes, int currentVotes) {}
