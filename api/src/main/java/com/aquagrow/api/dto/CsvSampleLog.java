package com.aquagrow.api.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvSampleLog {

    @CsvBindByName(column = "Date")
    private String date;

    @CsvBindByName(column = "PondName")
    private String pondName;

    @CsvBindByName(column = "FishType")
    private String fishType;

    @CsvBindByName(column = "Pieces")
    private Integer pieces;

    @CsvBindByName(column = "TotalWeight")
    private Double totalWeight;

    @CsvBindByName(column = "Weight Per Unit")
    private Double weightPerUnit;

    @CsvBindByName(column = "BatchName")
    private String batchName;
}
