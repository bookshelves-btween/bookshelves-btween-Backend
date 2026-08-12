package com.bookshelves.domain.meeting.event;

import java.time.LocalDateTime;

public record MeetingCreatedEvent(Long meetingId, LocalDateTime startDate) {}
