package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.TourCategoryResponse;
import com.tripconnect.backend.entity.TourCategory;
import com.tripconnect.backend.repository.TourCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tour-categories")
@RequiredArgsConstructor
public class TourCategoryController {

    private final TourCategoryRepository tourCategoryRepository;

    @GetMapping
    public List<TourCategoryResponse> getAllCategories() {
        return tourCategoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private TourCategoryResponse toResponse(TourCategory category) {
        return new TourCategoryResponse(category.getId(), category.getName());
    }
}