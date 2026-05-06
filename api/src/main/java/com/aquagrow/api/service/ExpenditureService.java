package com.aquagrow.api.service;

import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.Batch;
import com.aquagrow.api.model.Expenditure;
import com.aquagrow.api.repository.BatchRepository;
import com.aquagrow.api.repository.ExpenditureRepository;
import com.aquagrow.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenditureService {

    private final ExpenditureRepository expenditureRepository;
    private final BatchRepository batchRepository;

    @Transactional(readOnly = true)
    public List<Expenditure> getAllExpenditures() {
        return expenditureRepository.findAll();
    }

    @Transactional
    public Expenditure logExpenditure(Expenditure expenditure) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException("Tenant context is required");
        }
        expenditure.setTenantId(tenantId);
        if (expenditure.getDate() == null) {
            expenditure.setDate(LocalDate.now());
        }
        return expenditureRepository.save(expenditure);
    }

    @Transactional(readOnly = true)
    public List<Expenditure> getExpendituresByBatch(String batchId) {
        return expenditureRepository.findByBatchId(batchId);
    }

    @Transactional(readOnly = true)
    public Map<String, Double> getMonthlySummary(LocalDate date) {
        LocalDate start = date.withDayOfMonth(1);
        LocalDate end = date.withDayOfMonth(date.lengthOfMonth());

        List<Expenditure> expenditures = expenditureRepository.findByTenantIdAndDateBetween(
                TenantContext.getTenantId(), start, end);

        return expenditures.stream()
                .collect(Collectors.groupingBy(
                        Expenditure::getCategory,
                        Collectors.summingDouble(Expenditure::getAmount)));
    }

    @Transactional
    public Expenditure logExpenditureByName(String batchName, String category, Double amount, String description) {
        String batchId = null;
        if (batchName != null && !batchName.equalsIgnoreCase("Unallocated") && !batchName.equalsIgnoreCase("None")) {
            batchId = batchRepository.findByName(batchName)
                    .map(Batch::getId)
                    .orElseThrow(() -> new BusinessException("Batch not found: " + batchName));
        }

        // Determine macro investment based on category (rough logic, can be refined)
        String macroInvestment = "INVESTMENT";
        if (category.equalsIgnoreCase("NoReturn") || category.equalsIgnoreCase("Maintenance")) {
            macroInvestment = "NO_RETURN";
        } else if (category.equalsIgnoreCase("Lease")) {
            macroInvestment = "LEASE";
        } else if (category.equalsIgnoreCase("Advance")) {
            macroInvestment = "ADVANCE";
        }

        Expenditure expenditure = Expenditure.builder()
                .batchId(batchId)
                .date(LocalDate.now())
                .category(category)
                .macroInvestment(macroInvestment)
                .amount(amount)
                .description(description)
                .build();

        return logExpenditure(expenditure);
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctMacroInvestments() {
        return expenditureRepository.findDistinctMacroInvestmentsByTenantId(TenantContext.getTenantId());
    }

    @Transactional
    public Expenditure updateExpenditure(String id, Expenditure updated) {
        String tenantId = TenantContext.getTenantId();
        Expenditure existing = expenditureRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Expenditure not found: " + id));
        if (!existing.getTenantId().equals(tenantId)) {
            throw new BusinessException("Expenditure not found: " + id);
        }
        existing.setBatchId(updated.getBatchId());
        existing.setDate(updated.getDate() != null ? updated.getDate() : existing.getDate());
        existing.setCategory(updated.getCategory());
        existing.setMacroInvestment(updated.getMacroInvestment());
        existing.setSubType(updated.getSubType());
        existing.setAmount(updated.getAmount());
        existing.setDescription(updated.getDescription());
        return expenditureRepository.save(existing);
    }

    @Transactional
    public void deleteExpenditure(String id) {
        String tenantId = TenantContext.getTenantId();
        Expenditure existing = expenditureRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Expenditure not found: " + id));
        if (!existing.getTenantId().equals(tenantId)) {
            throw new BusinessException("Expenditure not found: " + id);
        }
        expenditureRepository.delete(existing);
        log.info("Deleted expenditure: {} for tenant: {}", id, tenantId);
    }

    @Transactional
    public Expenditure logExpenditureExtended(
            LocalDate date,
            String batchName,
            Double amount,
            String macro,
            String category,
            String subType,
            String description) {

        String batchId = null;
        if (batchName != null && !batchName.equalsIgnoreCase("Unallocated") && !batchName.equalsIgnoreCase("None")) {
            batchId = batchRepository.findByName(batchName)
                    .map(Batch::getId)
                    .orElseThrow(() -> new BusinessException("Batch not found: " + batchName));
        }

        Expenditure expenditure = Expenditure.builder()
                .batchId(batchId)
                .date(date != null ? date : LocalDate.now())
                .category(category)
                .macroInvestment(macro)
                .subType(subType)
                .amount(amount)
                .description(description)
                .build();

        return logExpenditure(expenditure);
    }
}
