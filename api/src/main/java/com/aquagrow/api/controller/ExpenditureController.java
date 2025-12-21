package com.aquagrow.api.controller;

import com.aquagrow.api.model.Expenditure;
import com.aquagrow.api.repository.ExpenditureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenditures")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExpenditureController {

    private final ExpenditureRepository expenditureRepository;

    @GetMapping
    public List<Expenditure> getAllExpenditures() {
        return expenditureRepository.findAll();
    }

    @PostMapping
    public Expenditure logExpenditure(@RequestBody Expenditure expenditure) {
        return expenditureRepository.save(expenditure);
    }

    @GetMapping("/batch/{batchId}")
    public List<Expenditure> getExpendituresByBatch(@PathVariable String batchId) {
        return expenditureRepository.findByBatchId(batchId);
    }
}
