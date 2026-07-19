package com.bookshelves.domain.ai.repository;

import com.bookshelves.domain.ai.entity.MeetingSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingSummaryRepository extends JpaRepository<MeetingSummary, Long> {}
