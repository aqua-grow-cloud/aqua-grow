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
        expenditure.setTenantId(com.aquagrow.api.security.TenantContext.getTenantId());
        return expenditureRepository.save(expenditure);
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
        }, pageable);
    }
}
