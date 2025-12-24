package com.aquagrow.api.controller;

import com.aquagrow.api.model.Supplier;
import com.aquagrow.api.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@CrossOrigin(origins = "*")
public class SupplierController {

    @Autowired
    private SupplierRepository supplierRepository;

    private String getTenantId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getClaimAsString("tenant_id");
    }

    @GetMapping
    public List<Supplier> getAllSuppliers() {
        // User requested global list of suppliers
        return supplierRepository.findAll();
    }

    @GetMapping("/attached")
    public ResponseEntity<Supplier> getAttachedSupplier(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        // Extract external ID from token - assuming claim name 'supplierExternalId'
        // based on user request: "supplierExternalId should match with supplier table
        // externalId"
        String externalId = jwt.getClaimAsString("supplierExternalId");

        if (externalId == null) {
            return ResponseEntity.notFound().build();
        }

        return supplierRepository.findByExternalId(externalId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Supplier createSupplier(@RequestBody Supplier supplier, Authentication authentication) {
        String tenantId = getTenantId(authentication);
        supplier.setTenantId(tenantId);
        return supplierRepository.save(supplier);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Supplier> updateSupplier(
            @PathVariable String id,
            @RequestBody Supplier supplierDetails,
            Authentication authentication) {
        // Allow updating any supplier if it's a global list?
        // For safety, we might want to restrict this, but request implies "global
        // list".
        // Keeping it simple: if it exists, update it.
        return supplierRepository.findById(id)
                .map(supplier -> {
                    supplier.setName(supplierDetails.getName());
                    supplier.setPhone(supplierDetails.getPhone());
                    supplier.setWhatsappNumber(supplierDetails.getWhatsappNumber());
                    supplier.setEmail(supplierDetails.getEmail());
                    supplier.setAddress(supplierDetails.getAddress());
                    supplier.setFeedTypes(supplierDetails.getFeedTypes());
                    return ResponseEntity.ok(supplierRepository.save(supplier));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable String id, Authentication authentication) {
        // Global delete
        return supplierRepository.findById(id)
                .map(supplier -> {
                    supplierRepository.delete(supplier);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
