package com.paymate.paymate_server.domain.modification.service;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.repository.MemberRepository; // 👈 이걸로 변경!import com.paymate.paymate_server.domain.modification.dto.ModificationRequestDto;
import com.paymate.paymate_server.domain.modification.dto.ModificationRequestDto;
import com.paymate.paymate_server.domain.modification.dto.ModificationResponseDto;
import com.paymate.paymate_server.domain.modification.entity.ModificationRequest;
import com.paymate.paymate_server.domain.modification.enums.RequestStatus;
import com.paymate.paymate_server.domain.modification.enums.RequestTargetType;
import com.paymate.paymate_server.domain.modification.repository.ModificationRepository;
import com.paymate.paymate_server.domain.store.entity.Store;
import com.paymate.paymate_server.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModificationService {

    private final ModificationRepository modificationRepository;
    private final MemberRepository memberRepository; // 👈 이름 변경
    private final StoreRepository storeRepository;

    // TODO: 🤝 추후 팀원들이 만든 Service 주입 필요 (AttendanceService, ScheduleService)
    // private final AttendanceService attendanceService;
    // private final ScheduleService scheduleService;

    // 1. 등록 (로그인한 유저 ID 사용)
    @Transactional
    public ModificationResponseDto createModification(Long userId, ModificationRequestDto dto) {
        // 토큰에서 뽑은 ID로 유저 찾기
        User requester = memberRepository.findById(userId) // ✅ 해결!
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Store store = storeRepository.findById(dto.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + dto.getStoreId()));

        ModificationRequest request = ModificationRequest.builder()
                .requester(requester)
                .store(store)
                .targetType(dto.getTargetType())
                .targetId(dto.getTargetId()) // REGISTER인 경우 null일 수 있음
                .requestType(dto.getRequestType())
                .beforeValue(dto.getBeforeValue())
                .afterValue(dto.getAfterValue())
                .targetDate(dto.getTargetDate())
                .reason(dto.getReason())
                .status(RequestStatus.PENDING) // ✅ 초기 상태는 무조건 PENDING
                .build();

        return new ModificationResponseDto(modificationRepository.save(request));
    }

    // 2. 목록 조회
    public List<ModificationResponseDto> getModifications(Long storeId, RequestStatus status, Long requesterId) {
        List<ModificationRequest> requests;

        if (requesterId != null) {
            // 알바생: 내 요청만 보기
            requests = modificationRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId);
        } else if (status != null) {
            // 사장님: 상태별 조회 (예: 대기중인 것만)
            requests = modificationRepository.findByStoreIdAndStatusOrderByCreatedAtDesc(storeId, status);
        } else {
            // 사장님: 전체 조회
            requests = modificationRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
        }

        return requests.stream()
                .map(ModificationResponseDto::new)
                .collect(Collectors.toList());
    }

    // 3. 상세 조회
    public ModificationResponseDto getModificationDetail(Long requestId) {
        ModificationRequest request = modificationRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));
        return new ModificationResponseDto(request);
    }

    // 4. 상태 변경 (승인/거절)
    @Transactional
    public ModificationResponseDto updateStatus(Long requestId, RequestStatus newStatus) {
        ModificationRequest request = modificationRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        // 상태 변경 실행 (Dirty Checking)
        request.updateStatus(newStatus);

        // ★ 승인(APPROVED)일 경우, 실제 데이터 반영 로직 실행
        if (newStatus == RequestStatus.APPROVED) {
            applyModificationToTarget(request);
        }

        return new ModificationResponseDto(request);
    }

    // 5. 삭제 (취소)
    @Transactional
    public void deleteModification(Long requestId) {
        ModificationRequest request = modificationRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        // PENDING 상태가 아니면 삭제 불가
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 요청은 삭제할 수 없습니다.");
        }

        modificationRepository.delete(request);
    }

    // [내부 메서드] 실제 데이터 반영 로직 (Facade)
    private void applyModificationToTarget(ModificationRequest request) {
        System.out.println(">>> [AUTO UPDATE] " + request.getTargetType() + " 수정 로직 실행...");

        /* TODO: 🛠️ 팀원들과 코드 합칠 때 아래 주석 해제 및 구현!

         if (request.getTargetType() == RequestTargetType.ATTENDANCE) {
             // attendanceService.updateByRequest(request.getTargetId(), request.getAfterValue());
         } else if (request.getTargetType() == RequestTargetType.SCHEDULE) {
             // scheduleService.updateByRequest(request.getTargetId(), request.getAfterValue());
         }
        */
    }
}