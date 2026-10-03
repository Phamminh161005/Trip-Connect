package com.tripconnect.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "agent_specialties")
@Getter
@Setter
@NoArgsConstructor
public class AgentSpecialty {

    @EmbeddedId
    private AgentSpecialtyId id = new AgentSpecialtyId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("agentId")
    @JoinColumn(name = "agent_id")
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("categoryId")
    @JoinColumn(name = "category_id")
    private TourCategory category;
}