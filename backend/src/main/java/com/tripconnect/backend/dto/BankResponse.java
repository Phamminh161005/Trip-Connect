package com.tripconnect.backend.dto;

import com.tripconnect.backend.entity.Bank;

public record BankResponse(String bin, String code, String shortName, String name, String logoUrl) {

    public static BankResponse from(Bank bank) {
        return bank == null ? null
                : new BankResponse(bank.getBin(), bank.getCode(), bank.getShortName(), bank.getName(), bank.getLogoUrl());
    }
}
