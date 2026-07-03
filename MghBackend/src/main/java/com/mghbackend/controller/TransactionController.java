package com.mghbackend.controller;

import com.mghbackend.exception.BusinessException;

import com.mghbackend.dto.StatistiquesFinanceDto;
import com.mghbackend.dto.TransactionDto;
import com.mghbackend.dto.reponse.ApiResponse;
import com.mghbackend.enums.TypeTransaction;
import com.mghbackend.security.CustomUserPrincipal;
import com.mghbackend.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;

    // ─── CREATE ────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_MODIFIER_COMPTABILITE')")
    public ResponseEntity<ApiResponse<TransactionDto>> createTransaction(
            @RequestBody TransactionDto dto,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            Long userId = "USER".equals(principal.getAccountType()) ? principal.getId() : null;
            TransactionDto transaction = transactionService.createTransaction(
                    principal.getHotelId(), dto, userId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Transaction créée avec succès", transaction));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── READ ──────────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_COMPTABILITE')")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> getTransactions(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            List<TransactionDto> transactions = transactionService
                    .getTransactionsByHotel(principal.getHotelId());
            return ResponseEntity.ok(ApiResponse.success(transactions));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_COMPTABILITE')")
    public ResponseEntity<ApiResponse<TransactionDto>> getTransaction(@PathVariable Long id) {
        try {
            TransactionDto transaction = transactionService.getTransactionById(id);
            return ResponseEntity.ok(ApiResponse.success(transaction));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_COMPTABILITE')")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> getTransactionsByType(
            @PathVariable TypeTransaction type,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            List<TransactionDto> transactions = transactionService
                    .getTransactionsByHotelAndType(principal.getHotelId(), type);
            return ResponseEntity.ok(ApiResponse.success(transactions));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/en-attente")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_COMPTABILITE')")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> getTransactionsEnAttente(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            List<TransactionDto> transactions = transactionService
                    .getTransactionsEnAttente(principal.getHotelId());
            return ResponseEntity.ok(ApiResponse.success(transactions));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_COMPTABILITE')")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> searchTransactions(
            @RequestParam String keyword,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            List<TransactionDto> transactions = transactionService
                    .searchTransactions(principal.getHotelId(), keyword);
            return ResponseEntity.ok(ApiResponse.success(transactions));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── UPDATE ────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_MODIFIER_COMPTABILITE')")
    public ResponseEntity<ApiResponse<TransactionDto>> updateTransaction(
            @PathVariable Long id,
            @RequestBody TransactionDto dto) {
        try {
            TransactionDto transaction = transactionService.updateTransaction(id, dto);
            return ResponseEntity.ok(ApiResponse.success("Transaction mise à jour", transaction));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── ACTIONS ───────────────────────────────────────────────────────────────

    @PostMapping("/{id}/valider")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_MODIFIER_COMPTABILITE')")
    public ResponseEntity<ApiResponse<TransactionDto>> validerTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            String validePar = principal.getEmail();
            TransactionDto transaction = transactionService.validerTransaction(id, validePar);
            return ResponseEntity.ok(ApiResponse.success("Transaction validée avec succès", transaction));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_MODIFIER_COMPTABILITE')")
    public ResponseEntity<ApiResponse<TransactionDto>> annulerTransaction(
            @PathVariable Long id,
            @RequestParam(required = false) String motif) {
        try {
            TransactionDto transaction = transactionService.annulerTransaction(id, motif);
            return ResponseEntity.ok(ApiResponse.success("Transaction annulée", transaction));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── DELETE ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_MODIFIER_COMPTABILITE')")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable Long id) {
        try {
            transactionService.deleteTransaction(id);
            return ResponseEntity.ok(ApiResponse.success("Transaction supprimée", null));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── STATISTIQUES ──────────────────────────────────────────────────────────

    @GetMapping("/statistiques")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_COMPTABILITE')")
    public ResponseEntity<ApiResponse<StatistiquesFinanceDto>> getStatistiques(
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            StatistiquesFinanceDto stats = transactionService
                    .getStatistiques(principal.getHotelId());
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─── EXPORT ────────────────────────────────────────────────────────────────

    @GetMapping("/export")
    @PreAuthorize("hasRole('HOTEL') or hasAuthority('PERMISSION_VOIR_COMPTABILITE')")
    public ResponseEntity<byte[]> exportTransactions(
            @RequestParam String format,
            @RequestParam(required = false) String dateDebut,
            @RequestParam(required = false) String dateFin,
            @AuthenticationPrincipal CustomUserPrincipal principal) {
        try {
            byte[] data = transactionService.exportTransactions(
                    principal.getHotelId(), format, dateDebut, dateFin);

            String contentType = "EXCEL".equalsIgnoreCase(format)
                    ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    : "application/pdf";
            String extension = "EXCEL".equalsIgnoreCase(format) ? ".xlsx" : ".pdf";

            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Content-Disposition",
                            "attachment; filename=\"transactions" + extension + "\"")
                    .body(data);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}