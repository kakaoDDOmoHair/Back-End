package com.paymate.paymate_server.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass // 이 클래스를 상속받는 엔티티들이 아래 필드들을 컬럼으로 인식하게 함
@EntityListeners(AuditingEntityListener.class) // 자동으로 시간을 기록해주는 리스너
public abstract class BaseTimeEntity {

    @CreatedDate // 엔티티가 생성될 때 시간이 자동 저장됨
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate // 엔티티가 수정될 때 시간이 자동 저장됨
    private LocalDateTime updatedAt;
}