package com.tripconnect.backend.repository;

import com.tripconnect.backend.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankRepository extends JpaRepository<Bank, String> {

    List<Bank> findByActiveTrueOrderByShortNameAsc();

    Optional<Bank> findByBinAndActiveTrue(String bin);
}
