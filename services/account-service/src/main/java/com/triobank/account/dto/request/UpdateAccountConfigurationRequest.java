package com.triobank.account.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * UpdateAccountConfigurationRequest
 * 
 * Hesap yapılandırma güncelleme isteği.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountConfigurationRequest {

    @NotNull(message = "Yapılandırma bilgisi zorunludur.")
    private Map<String, Object> configurations;
}
