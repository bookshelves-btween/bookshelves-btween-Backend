package com.bookshelves.domain.chat.dto;

// VOTE_COUNT 프레임 data — 질문 생성 투표 현황 변경 시 전원 broadcast
public record ChatVoteCountPayload(int currentVotes, int requiredVotes) {}
