package com.bookshelves.domain.meeting.repository;

import com.bookshelves.domain.meeting.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {}
