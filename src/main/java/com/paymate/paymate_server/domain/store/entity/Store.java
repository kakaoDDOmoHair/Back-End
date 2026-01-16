package com.paymate.paymate_server.domain.store.entity; // 👈 패키지명 확인!

import com.paymate.paymate_server.domain.store.enums.StorePayRule;
import com.paymate.paymate_server.domain.store.enums.TaxType;
import com.paymate.paymate_server.domain.member.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
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
    private User owner; // 사장님 연결

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String address;

    @Column(name = "detail_address", length = 200)
    private String detailAddress;

    @Column(name = "business_number", length = 20)
    private String businessNumber;

    @Column(name = "pay_day")
    private Integer payDay;

    @Column(name = "president_name", length = 50)
    private String presidentName;

    @Column(name = "opening_date")
    private LocalDate openingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type")
    private TaxType taxType;

    @Column(length = 50)
    private String category;

    @Column(name = "wifi_info", length = 100)
    private String wifiInfo;

    @Column(name = "store_phone", length = 20)
    private String storePhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "pay_rule")
    private StorePayRule payRule;
}