package com.aquagrow.api.controller;

import java.util.Objects;

import com.aquagrow.api.dto.CsvExpenditure;
import com.aquagrow.api.model.Batch;
import com.aquagrow.api.model.Expenditure;
import com.aquagrow.api.repository.BatchRepository;
import com.aquagrow.api.repository.ExpenditureRepository;
import com.aquagrow.api.service.ExpenditureService;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expenditures")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExpenditureController {

    private final ExpenditureService expenditureService;
    private final ExpenditureRepository expenditureRepository;
    private final BatchRepository batchRepository;

    @GetMapping
    public List<Expenditure> getAllExpenditures() {
        return expenditureService.getAllExpenditures();
    }

    @PostMapping
    public Expenditure logExpenditure(@RequestBody Expenditure expenditure) {
        return expenditureService.logExpenditure(expenditure);
    }

    @GetMapping("/batch/{batchId}")
    public List<Expenditure> getExpendituresByBatch(@PathVariable String batchId) {
        return expenditureRepository.findByBatchId(batchId);
    }

    @GetMapping("/search")
    public org.springframework.data.domain.Page<Expenditure> searchExpenditures(
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String macroInvestment,
            @RequestParam(required = false) String category,
            org.springframework.data.domain.Pageable pageable) {

        return expenditureRepository.findAll((root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            if (batchId != null && !batchId.isEmpty()) {
                predicates.add(cb.equal(root.get("batchId"), batchId));
            }
            if (macroInvestment != null && !macroInvestment.isEmpty()) {
                predicates.add(cb.equal(root.get("macroInvestment"), macroInvestment));
            }
            if (category != null && !category.isEmpty()) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, Objects.requireNonNull(pageable));
    }

    @GetMapping("/export")
    public void exportCsv(HttpServletResponse response) throws Exception {
        String filename = "expenditures.csv";
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        List<Expenditure> expenditures = expenditureRepository.findAll();
        List<CsvExpenditure> csvExpenditures = expenditures.stream()
                .map(e -> {
                    String batchName = "Unallocated";
                    if (e.getBatchId() != null) {
                        batchName = batchRepository.findById(Objects.requireNonNull(e.getBatchId()))
                                .map(Batch::getName)
                                .orElse("Unknown Batch");
                    }
                    return CsvExpenditure.builder()
                            .batchName(batchName)
                            .date(e.getDate())
                            .category(e.getCategory())
                            .macroInvestment(e.getMacroInvestment())
                            .subType(e.getSubType())
                            .amount(e.getAmount())
                            .description(e.getDescription())
                            .build();
                })
                .collect(Collectors.toList());

        StatefulBeanToCsv<CsvExpenditure> writer = new StatefulBeanToCsvBuilder<CsvExpenditure>(response.getWriter())
                .withQuotechar(com.opencsv.CSVWriter.NO_QUOTE_CHARACTER)
                .withSeparator(com.opencsv.CSVWriter.DEFAULT_SEPARATOR)
                .withOrderedResults(false)
                .build();

        writer.write(csvExpenditures);
    }

    @PostMapping("/import")
    public List<Expenditure> importCsv(@RequestParam("file") MultipartFile file) throws Exception {
        String tenantId = com.aquagrow.api.security.TenantContext.getTenantId();

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            CsvToBean<CsvExpenditure> csvToBean = new CsvToBeanBuilder<CsvExpenditure>(reader)
                    .withType(CsvExpenditure.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<CsvExpenditure> csvExpenditures = csvToBean.parse();
            List<Expenditure> expenditures = csvExpenditures.stream()
                    .map(csv -> {
                        String batchId = null;
                        if (csv.getBatchName() != null && !csv.getBatchName().isEmpty()) {
                            batchId = batchRepository.findByName(csv.getBatchName())
                                    .map(Batch::getId)
                                    .orElse(null); // Or throw error? For now just unallocated
                        }
                        return Expenditure.builder()
                                .batchId(batchId)
                                .date(csv.getDate())
                                .category(csv.getCategory())
                                .macroInvestment(csv.getMacroInvestment())
                                .subType(csv.getSubType())
                                .amount(csv.getAmount())
                                .description(csv.getDescription())
                                .tenantId(tenantId)
                                .build();
                    })
                    .collect(Collectors.toList());

            return (List<Expenditure>) expenditureRepository.saveAll(expenditures);
        }
    }
}
