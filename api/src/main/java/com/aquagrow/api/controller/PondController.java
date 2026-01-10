package com.aquagrow.api.controller;

import com.aquagrow.api.model.Pond;
import com.aquagrow.api.repository.PondRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ponds")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PondController {

    private final PondRepository pondRepository;

    @GetMapping
    public List<Pond> getAllPonds() {
        return pondRepository.findAll();
    }

    @PostMapping
    public Pond createPond(@RequestBody Pond pond) {
        pond.setTenantId(com.aquagrow.api.security.TenantContext.getTenantId());
        return pondRepository.save(pond);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pond> getPondById(@PathVariable String id) {
        return pondRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pond> updatePond(@PathVariable String id, @RequestBody Pond pondDetails) {
        return pondRepository.findById(id)
                .map(pond -> {
                    pond.setName(pondDetails.getName());
                    pond.setArea(pondDetails.getArea());
                    pond.setDepth(pondDetails.getDepth());
                    pond.setType(pondDetails.getType());
                    pond.setStatus(pondDetails.getStatus());
                    pond.setCurrentBatchId(pondDetails.getCurrentBatchId());
                    pond.setLatitude(pondDetails.getLatitude());
                    pond.setLongitude(pondDetails.getLongitude());
                    return ResponseEntity.ok(pondRepository.save(pond));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePond(@PathVariable String id) {
        return pondRepository.findById(id)
                .map(pond -> {
                    pondRepository.delete(pond);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
