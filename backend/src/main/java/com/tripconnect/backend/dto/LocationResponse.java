package com.tripconnect.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LocationResponse {
    private Long id;
    private String country;
    private String province;
}