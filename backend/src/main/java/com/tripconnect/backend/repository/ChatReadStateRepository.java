package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.ChatReadState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatReadStateRepository extends JpaRepository<ChatReadState, ChatReadState.Key> {
}
