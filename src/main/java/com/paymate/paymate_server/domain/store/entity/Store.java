package com.paymate.paymate_server.domain.store.entity;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.store.enums.StorePayRule;
import com.paymate.paymate_server.domain.store.enums.TaxType;
import com.paymate.paymate_server.global.util.AccountNumberConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 안전성을 위해 PROTECTED 권장
@AllArgsConstructor
@Builder
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // --- 기본 정보 ---
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String address;

    @Column(name = "detail_address", length = 200)
    private String detailAddress;

    @Column(name = "business_number", length = 20)
    private String businessNumber;

    @Column(name = "president_name", length = 50)
    private String presidentName;

    @Column(name = "opening_date")
    private LocalDate openingDate;

    // --- 운영 정보 ---
    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type")
    private TaxType taxType;

    @Column(length = 50)
    private String category; // 업종/업태

    @Column(name = "wifi_info", length = 100)
    private String wifiInfo;

    @Column(name = "store_phone", length = 20)
    private String storePhone;

    // --- 위치 정보 (출근 확인용) ---
    @Column(name = "latitude")
    private Double latitude; // 위도

    @Column(name = "longitude")
    private Double longitude; // 경도

    // --- 급여 및 정산 정보 (추가됨) ---
    @Column(name = "pay_day")
    private Integer payDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "pay_rule")
    private StorePayRule payRule;

    @Column(name = "bank_name", length = 20)
    private String bankName; // 예: 국민은행

    // ★ 암호화 적용된 계좌번호
    @Convert(converter = AccountNumberConverter.class)
    @Column(name = "account_number", length = 500) // 암호화되면 길이가 늘어나므로 넉넉하게
    private String accountNumber;

    // --- 알바생 초대 정보 (추가됨) ---
    @Column(name = "invite_code", length = 20, unique = true)
    private String inviteCode; // 난수 코드
}