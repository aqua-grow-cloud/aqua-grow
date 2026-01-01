package com.aquagrow.api.dto;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CsvExpenditure {

    @CsvBindByName(column = "Batch Name")
    private String batchName;

    @CsvBindByName(column = "Date")
    @CsvDate("yyyy-MM-dd")
    private LocalDate date;

    @CsvBindByName(column = "Category")
    private String category;

    @CsvBindByName(column = "Macro Investment")
    private String macroInvestment;

    @CsvBindByName(column = "Sub Type")
    private String subType;

    @CsvBindByName(column = "Amount")
    private Double amount;

    @CsvBindByName(column = "Description")
    private String description;
}
