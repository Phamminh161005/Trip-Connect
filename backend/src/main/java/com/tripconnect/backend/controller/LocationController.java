package com.tripconnect.backend.controller;

import com.tripconnect.backend.dto.LocationResponse;
import com.tripconnect.backend.entity.Location;
import com.tripconnect.backend.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationRepository locationRepository;

    @GetMapping
    public List<LocationResponse> getAllLocations() {
        return locationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private LocationResponse toResponse(Location location) {
        return new LocationResponse(location.getId(), location.getCountry(), location.getProvince());
    }
}