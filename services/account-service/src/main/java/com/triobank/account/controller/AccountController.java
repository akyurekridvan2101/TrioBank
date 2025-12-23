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
 * Bu kontrolcü (Controller) dış dünyanın (Mobil Uygulama, Web Sitesi)
 * bankacılık sistemimizle konuştuğu kapıdır.
 * 
 * Sorumlulukları:
 * 1. Gelen HTTP isteklerini karşılamak.
 * 2. Gelen veriyi doğrulamak (@Valid).
 * 3. İşi asıl yapacak olan "Service" katmanına devretmek.
 * 4. Sonucu dış dünyaya "Response DTO" olarak dönmek.
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
     * Müşteri yeni bir vadesiz hesap, altın hesabı vb. açmak istediğinde buraya
     * gelir.
     * Başarılı olursa 201 (Created) döner.
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
     * 
     * Tekil bir hesabın detaylarını (Bakiye, IBAN vb.) getirir.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String id) {
        Account account = accountService.getAccount(id);
        return ResponseEntity.ok(accountMapper.toResponse(account));
    }

    /**
     * [GET] Müşteri Hesap Listesi
     * 
     * Bir müşterinin sahip olduğu tüm hesapları listeler.
     * İsteğe bağlı olarak statü filtresi verilebilir.
     * Örn: ?customerId=123
     * Örn: ?customerId=123&status=ACTIVE
     * Örn: ?customerId=123&status=ACTIVE&status=FROZEN
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
     * Hesabı dondurmak (Freeze), kapatmak (Close) veya tekrar açmak (Activate) için
     * kullanılır.
     * Bu işlem hassas olduğu için "neden" (reason) bilgisi de istenir.
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
     * Hesaba özel ayarların güncellenmesi için.
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
     * Transaction Service bir işlem başlatmadan önce hesabın durumunu,
     * ürün kurallarını ve limitleri kontrol etmek için bu endpoint'i çağırır.
     *
     * @param id Hesap ID
     * @return Validation bilgileri
     */
    @GetMapping("/{id}/validation")
    public ResponseEntity<AccountValidationResponse> validateForTransaction(@PathVariable String id) {
        AccountValidationResponse response = accountService.validateForTransaction(id);
        return ResponseEntity.ok(response);
    }
}
