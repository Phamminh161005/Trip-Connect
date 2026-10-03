package com.tripconnect.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "agent_service_areas")
@Getter
@Setter
@NoArgsConstructor
public class AgentServiceArea {

    @EmbeddedId
    private AgentServiceAreaId id = new AgentServiceAreaId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("agentId")
    @JoinColumn(name = "agent_id")
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("locationId")
    @JoinColumn(name = "location_id")
    private Location location;
}