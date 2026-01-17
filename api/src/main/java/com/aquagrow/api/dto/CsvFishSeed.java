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
public class CsvFishSeed {

    @CsvBindByName(column = "Date")
    private String date;

    @CsvBindByName(column = "BatchName")
    private String batchName;

    @CsvBindByName(column = "PondName")
    private String pondName;

    @CsvBindByName(column = "Type")
    private String type;

    @CsvBindByName(column = "Quantity")
    private Integer quantity;

    @CsvBindByName(column = "Size ( inches)")
    private Double sizeInches;

    @CsvBindByName(column = "Size(grams)")
    private Double sizeGrams;
}
