package com.bookshelves.domain.ai.repository;

import com.bookshelves.domain.ai.entity.AIRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AIRecommendationRepository extends JpaRepository<AIRecommendation, Long> {}
