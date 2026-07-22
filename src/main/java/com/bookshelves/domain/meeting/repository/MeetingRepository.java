package com.bookshelves.domain.meeting.repository;

import com.bookshelves.domain.meeting.entity.Meeting;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

  @EntityGraph(attributePaths = "book")
  Optional<Meeting> findWithBookById(Long id);

  @EntityGraph(attributePaths = "book")
  Page<Meeting> findByBookTitleContainingIgnoreCase(String title, Pageable pageable);
}
