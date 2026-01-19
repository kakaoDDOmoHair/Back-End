package com.paymate.paymate_server.domain.store.dto;

import com.paymate.paymate_server.domain.store.entity.Store;
import lombok.Getter;

@Getter
public class StoreResponse {
    private Long storeId;
    private String storeName;
    private String ownerName;
    private String businessNumber;
    private String openingDate;
    private String sector;
    private String address;
    private String phone;
    private String wifi;
    private String payday;
    private String account;

    public StoreResponse(Store store) {
        this.storeId = store.getId();
        this.storeName = store.getName();
        this.ownerName = store.getPresidentName();
        this.businessNumber = store.getBusinessNumber();

        // 👇 [핵심 수정] 날짜가 비어있으면 null을 넣고, 아니면 문자열로 변환! (에러 방지)
        this.openingDate = (store.getOpeningDate() != null) ? store.getOpeningDate().toString() : null;

        this.sector = store.getCategory();
        this.address = store.getAddress() + " " + store.getDetailAddress();
        this.phone = store.getStorePhone();
        this.wifi = store.getWifiInfo();
        this.payday = "매월 " + store.getPayDay() + "일";
        this.account = store.getBankName() + " " + store.getAccountNumber();
    }
}