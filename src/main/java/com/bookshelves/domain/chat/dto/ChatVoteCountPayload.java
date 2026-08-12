package com.bookshelves.domain.chat.dto;

// 질문 공개 투표 현황을 전달하는 VOTE_COUNT 프레임 데이터.
public record ChatVoteCountPayload(int currentVotes, int requiredVotes) {}
