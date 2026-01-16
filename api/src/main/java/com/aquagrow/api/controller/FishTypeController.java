package com.aquagrow.api.controller;

import com.aquagrow.api.model.FishType;
import com.aquagrow.api.repository.FishTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fish-types")
@RequiredArgsConstructor
public class FishTypeController {

    private final FishTypeRepository repository;

    @GetMapping
    public List<FishType> getAll(@AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        return repository.findByTenantId(tenantId);
    }

    @PostMapping
    public FishType create(@RequestBody FishType fishType, @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        fishType.setTenantId(tenantId);
        return repository.save(fishType);
    }

    @PutMapping("/{id}")
    public FishType update(@PathVariable String id, @RequestBody FishType update, @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        return repository.findById(id)
                .filter(ft -> ft.getTenantId().equals(tenantId))
                .map(ft -> {
                    ft.setName(update.getName());
                    ft.setScientificName(update.getScientificName());
                    return repository.save(ft);
                })
                .orElseThrow(() -> new RuntimeException("Fish type not found"));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        repository.findById(id)
                .filter(ft -> ft.getTenantId().equals(tenantId))
                .ifPresent(repository::delete);
    }
}
