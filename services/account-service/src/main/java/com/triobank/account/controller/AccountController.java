package com.triobank.account.controller;

import com.triobank.account.dto.mapper.AccountMapper;
import com.triobank.account.dto.request.CreateAccountRequest;
import com.triobank.account.dto.request.UpdateAccountStatusRequest;
import com.triobank.account.dto.response.AccountResponse;
import com.triobank.account.dto.response.AccountValidationResponse;
import com.triobank.account.domain.model.Account;
import com.triobank.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Hesap Yönetimi API
 *
 * Mobil uygulama veya web sitesinden gelen hesapla ilgili istekleri karşılar.
 * Validasyon işlemlerini yapıp, asıl işi Service katmanına devreder.
 */
@RestController
@RequestMapping("/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;

    /**
     * [POST] Yeni Hesap Açılışı
     *
     * Vadesiz hesap, altın hesabı vb. açılış işlemleri burada yapılır.
     * Başarılı olursa 201 Created döner.
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        // İşi servise devret
        Account account = accountService.createAccount(request);

        // Entity -> Response DTO dönüşümü ve cevap
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountMapper.toResponse(account));
    }

    /**
     * [GET] Hesap Detayı Görüntüleme
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String id) {
        Account account = accountService.getAccount(id);
        return ResponseEntity.ok(accountMapper.toResponse(account));
    }

    /**
     * [GET] Müşteri Hesap Listesi
     *
     * Müşterinin tüm hesaplarını listeler.
     * İsteğe bağlı statü filtresi (ACTIVE, FROZEN vb.) verilebilir.
     */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getCustomerAccounts(
            @RequestParam String customerId,
            @RequestParam(required = false) List<com.triobank.account.domain.model.AccountStatus> status) {

        List<Account> accounts = accountService.getCustomerAccounts(customerId, status);

        // List<Entity> -> List<Response DTO> dönüşümü
        List<AccountResponse> responses = accounts.stream()
                .map(accountMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * [PATCH] Hesap Durumu Güncelleme
     *
     * Hesabı dondurma (Freeze), kapatma (Close) veya aktifleştirme işlemleri.
     * Hassas işlem olduğu için sebep (reason) belirtilmesi zorunludur.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateAccountStatusRequest request) {

        accountService.changeStatus(id, request.getStatus(), request.getReason());

        // İşlem başarılı, geri dönecek veri yok (204 No Content)
        return ResponseEntity.noContent().build();
    }

    /**
     * [PATCH] Hesap Yapılandırması Güncelleme
     *
     * Hesaba özel ayarların (bildirim tercihleri vs) güncellenmesi.
     */
    @PatchMapping("/{id}/configuration")
    public ResponseEntity<Void> updateConfiguration(
            @PathVariable String id,
            @Valid @RequestBody com.triobank.account.dto.request.UpdateAccountConfigurationRequest request) {

        accountService.updateConfiguration(id, request.getConfigurations());
        return ResponseEntity.ok().build();
    }

    /**
     * [GET] Transaction Validation
     *
     * Transaction Service tarafından işlem öncesi kontrol (limit, statü) için çağrılır.
     *
     * @param id Hesap ID
     * @return Validasyon sonucu
     */
    @GetMapping("/{id}/validation")
    public ResponseEntity<AccountValidationResponse> validateForTransaction(@PathVariable String id) {
        AccountValidationResponse response = accountService.validateForTransaction(id);
        return ResponseEntity.ok(response);
    }
}
