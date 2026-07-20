package com.bookshelves.domain.meeting.repository;

import com.bookshelves.domain.meeting.entity.NoShowEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoShowEventRepository extends JpaRepository<NoShowEvent, Long> {}
