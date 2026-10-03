package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.TourCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TourCategoryRepository extends JpaRepository<TourCategory, Long> {
}