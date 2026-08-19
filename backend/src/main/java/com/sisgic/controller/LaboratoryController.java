package com.sisgic.controller;

import com.sisgic.dto.*;
import com.sisgic.service.LaboratoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/laboratories")
@CrossOrigin(origins = "*")
public class LaboratoryController {

    @Autowired
    private LaboratoryService laboratoryService;

    @GetMapping
    public ResponseEntity<Page<LaboratoryDTO>> listLaboratories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nameEs") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer clusterId,
            @RequestParam(required = false) Long directorId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean hasActiveMembers) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(laboratoryService.findAll(
            status, clusterId, directorId, search, hasActiveMembers, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LaboratoryDTO> getLaboratory(
            @PathVariable Long id,
            @RequestParam(required = false) Integer clusterId) {
        try {
            return laboratoryService.findById(id, clusterId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<LaboratoryDTO> createLaboratory(@RequestBody LaboratoryDTO dto) {
        try {
            return ResponseEntity.ok(laboratoryService.create(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<LaboratoryDTO> updateLaboratory(
            @PathVariable Long id,
            @RequestParam(required = false) Integer clusterId,
            @RequestBody LaboratoryDTO dto) {
        try {
            Integer effectiveClusterId = clusterId != null ? clusterId : dto.getClusterId();
            return laboratoryService.update(id, effectiveClusterId, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<LaboratoryDTO> activateLaboratory(
            @PathVariable Long id,
            @RequestParam(required = false) Integer clusterId) {
        try {
            return laboratoryService.activate(id, clusterId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<LaboratoryDTO> deactivateLaboratory(
            @PathVariable Long id,
            @RequestParam(required = false) Integer clusterId) {
        try {
            return laboratoryService.deactivate(id, clusterId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLaboratory(
            @PathVariable Long id,
            @RequestParam(required = false) Integer clusterId) {
        try {
            if (laboratoryService.delete(id, clusterId)) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                "message", e.getMessage() != null ? e.getMessage() : "Cannot delete laboratory"
            ));
        }
    }

    @PostMapping("/translate")
    public ResponseEntity<TranslateLaboratoryResponse> translateLaboratory(
            @RequestBody TranslateLaboratoryRequest request) {
        try {
            return ResponseEntity.ok(laboratoryService.translate(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(null);
        }
    }

    @PostMapping("/{id}/translate")
    public ResponseEntity<TranslateLaboratoryResponse> translateLaboratoryById(
            @PathVariable Long id, @RequestBody TranslateLaboratoryRequest request) {
        return translateLaboratory(request);
    }

    @PostMapping("/{id}/validate-translation")
    public ResponseEntity<LaboratoryDTO> validateTranslation(
            @PathVariable Long id,
            @RequestParam(required = false) Integer clusterId) {
        return laboratoryService.validateTranslation(id, clusterId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/memberships")
    public ResponseEntity<List<LaboratoryMembershipDTO>> getMemberships(
            @PathVariable Long id,
            @RequestParam(required = false) Integer clusterId,
            @RequestParam(required = false) String membershipType) {
        try {
            return ResponseEntity.ok(laboratoryService.getMemberships(id, clusterId, membershipType));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/memberships")
    public ResponseEntity<LaboratoryMembershipDTO> addMembership(
            @PathVariable Long id,
            @RequestParam(required = false) Integer clusterId,
            @RequestBody LaboratoryMembershipDTO dto) {
        try {
            return ResponseEntity.ok(laboratoryService.addMembership(id, clusterId, dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/memberships/{membershipKey}")
    public ResponseEntity<LaboratoryMembershipDTO> updateMembership(
            @PathVariable Long id,
            @PathVariable String membershipKey,
            @RequestParam(required = false) Integer clusterId,
            @RequestBody LaboratoryMembershipDTO dto) {
        try {
            return laboratoryService.updateMembership(id, clusterId, membershipKey, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/memberships/{membershipKey}/end")
    public ResponseEntity<LaboratoryMembershipDTO> endMembership(
            @PathVariable Long id,
            @PathVariable String membershipKey,
            @RequestParam(required = false) Integer clusterId,
            @RequestBody(required = false) EndLaboratoryMembershipRequest request) {
        try {
            var endDate = request != null ? request.getEndDate() : null;
            return laboratoryService.endMembership(id, clusterId, membershipKey, endDate)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/memberships/{membershipKey}")
    public ResponseEntity<Void> deleteMembershipPermanently(
            @PathVariable Long id,
            @PathVariable String membershipKey,
            @RequestParam(required = false) Integer clusterId) {
        try {
            if (laboratoryService.deleteMembershipPermanently(id, clusterId, membershipKey)) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/memberships/{membershipKey}/restore")
    public ResponseEntity<LaboratoryMembershipDTO> restoreMembership(
            @PathVariable Long id,
            @PathVariable String membershipKey,
            @RequestParam(required = false) Integer clusterId) {
        try {
            return laboratoryService.restoreMembership(id, clusterId, membershipKey)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/director")
    public ResponseEntity<LaboratoryDTO> assignDirector(
            @PathVariable Long id,
            @RequestParam(required = false) Integer clusterId,
            @RequestBody AssignLabDirectorRequest request) {
        try {
            return laboratoryService.assignDirector(id, clusterId, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/director")
    public ResponseEntity<LaboratoryDTO> clearDirector(
            @PathVariable Long id,
            @RequestParam(required = false) Integer clusterId) {
        try {
            return laboratoryService.clearDirector(id, clusterId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("/{id}/director/contact")
    public ResponseEntity<LaboratoryDTO> updateDirectorContact(
            @PathVariable Long id,
            @RequestParam(required = false) Integer clusterId,
            @RequestBody AssignLabDirectorRequest request) {
        try {
            return laboratoryService.updateDirectorContact(id, clusterId, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/lab-manager/change")
    public ResponseEntity<LaboratoryMembershipDTO> changeLabManager(
            @PathVariable Long id,
            @RequestParam(required = false) Integer clusterId,
            @RequestBody ChangeLabManagerRequest request) {
        try {
            return ResponseEntity.ok(laboratoryService.changeLabManager(id, clusterId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
