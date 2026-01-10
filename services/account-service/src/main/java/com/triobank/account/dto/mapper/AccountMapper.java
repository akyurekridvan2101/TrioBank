package com.triobank.account.dto.mapper;

import com.triobank.account.domain.model.Account;
import com.triobank.account.dto.response.AccountResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Hesap Nesne Dönüştürücüsü (Mapper)
 * 
 * Veritabanı varlıkları (Entity) ile API nesneleri (DTO) arasındaki
 * köprüyü kurar. MapStruct kütüphanesini kullanarak kod tekrarını ve
 * manuel set/get işlemlerini ortadan kaldırır.
 * 
 * Neden kullanıyoruz?
 * - Service katmanında "Entity -> DTO" dönüşüm mantığını gizlemek için.
 * - Kodun okunabilirliğini artırmak için.
 */
@Mapper(componentModel = "spring")
public interface AccountMapper {

    /**
     * Account entity'sini AccountResponse DTO'suna çevirir.
     * Alan isimleri birebir aynı olduğu için çoğu otomatik eşleşir.
     * 
     * @param account Veritabanından gelen kaynak nesne
     * @return API'ye dönülecek hedef nesne
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "accountNumber", source = "accountNumber")
    @Mapping(target = "productCode", source = "productCode")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "configurations", source = "configurations")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    AccountResponse toResponse(Account account);
}
