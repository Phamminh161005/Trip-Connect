package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
}