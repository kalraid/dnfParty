package com.dfparty.backend.dto;

import lombok.Data;

@Data
public class ManualStatsUpdateDto {
    private Long buffPower;
    private Long totalDamage;
    private Long buffPower4p;
    private Long buffPower3p;
    private Long buffPower2p;
    private Long totalDamage4p;
    private Long totalDamage3p;
    private Long totalDamage2p;
    private String updatedBy;
}
