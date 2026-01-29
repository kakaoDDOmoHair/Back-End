package com.paymate.paymate_server.domain.manual.service;

import com.paymate.paymate_server.domain.manual.dto.ManualRequest;
import com.paymate.paymate_server.domain.manual.dto.ManualResponse;
import com.paymate.paymate_server.domain.manual.entity.Manual;
import com.paymate.paymate_server.domain.manual.repository.ManualRepository;
import com.paymate.paymate_server.domain.store.entity.Store;
import com.paymate.paymate_server.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ManualService {

    private final ManualRepository manualRepository;
    private final StoreRepository storeRepository;

    // 생성
    public Long createManual(ManualRequest request) {
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("매장을 찾을 수 없습니다."));

        Manual manual = Manual.builder()
                .store(store)
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .build();

        return manualRepository.save(manual).getId();
    }

    // 조회
    @Transactional(readOnly = true)
    public List<ManualResponse> getManuals(Long storeId) {
        return manualRepository.findAllByStoreIdOrderByUpdatedAtDesc(storeId).stream()
                .map(ManualResponse::new)
                .collect(Collectors.toList());
    }

    // 상세 조회
    @Transactional(readOnly = true)
    public ManualResponse getManualDetail(Long manualId) {
        Manual manual = manualRepository.findById(manualId)
                .orElseThrow(() -> new IllegalArgumentException("매뉴얼을 찾을 수 없습니다."));
        return new ManualResponse(manual);
    }

    // 수정
    @Transactional
    public void updateManual(Long manualId, ManualRequest request) {
        Manual manual = manualRepository.findById(manualId)
                .orElseThrow(() -> new IllegalArgumentException("매뉴얼을 찾을 수 없습니다."));

        // 엔티티의 update 메서드 사용 (편리함!)
        manual.update(request.getTitle(), request.getContent(), request.getCategory());
    }

    // 삭제
    public void deleteManual(Long manualId) {
        Manual manual = manualRepository.findById(manualId)
                .orElseThrow(() -> new IllegalArgumentException("매뉴얼을 찾을 수 없습니다."));
        manualRepository.delete(manual);
    }
}