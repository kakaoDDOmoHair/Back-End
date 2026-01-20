package com.paymate.paymate_server.domain.manual.entity;

import com.paymate.paymate_server.domain.manual.enums.ManualCategory;
import com.paymate.paymate_server.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "manuals")
public class Manual {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "manual_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING) // 👈 String 대신 Enum 사용 (DB에는 문자열로 저장됨)
    @Column(length = 20)
    private ManualCategory category;

    @Column(name = "image_url")
    private String imageUrl;

    @CreationTimestamp // 👈 저장 시 날짜 자동 생성
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp // 👈 수정 시 날짜 자동 갱신
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 💡 수정 편의 메서드 (서비스에서 사용)
    public void update(String title, String content, ManualCategory category) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (category != null) this.category = category;
    }
}