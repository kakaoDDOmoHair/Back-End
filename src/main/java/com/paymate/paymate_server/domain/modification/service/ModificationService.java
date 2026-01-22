package com.paymate.paymate_server.domain.modification.service;

import com.paymate.paymate_server.domain.attendance.service.AttendanceService;
import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import com.paymate.paymate_server.domain.modification.dto.ModificationRequestDto;
import com.paymate.paymate_server.domain.modification.dto.ModificationResponseDto;
import com.paymate.paymate_server.domain.modification.entity.ModificationRequest;
import com.paymate.paymate_server.domain.modification.enums.RequestStatus;
import com.paymate.paymate_server.domain.modification.enums.RequestTargetType;
import com.paymate.paymate_server.domain.modification.repository.ModificationRepository;
import com.paymate.paymate_server.domain.notification.entity.Notification;
import com.paymate.paymate_server.domain.notification.enums.NotificationType;
import com.paymate.paymate_server.domain.notification.repository.NotificationRepository;
import com.paymate.paymate_server.domain.schedule.service.ScheduleService;
import com.paymate.paymate_server.domain.store.entity.Store;
import com.paymate.paymate_server.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModificationService {

    private final ModificationRepository modificationRepository;
    private final MemberRepository memberRepository;
    private final StoreRepository storeRepository;

    // ✅ 알림 저장을 위한 리포지토리 주입 (추가됨!)
    private final NotificationRepository notificationRepository;

    private final AttendanceService attendanceService;
    private final ScheduleService scheduleService;

    // 1. 정정 요청 등록
    @Transactional
    public ModificationResponseDto createModification(Long userId, ModificationRequestDto dto) {
        User requester = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Store store = storeRepository.findById(dto.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + dto.getStoreId()));

        ModificationRequest request = ModificationRequest.builder()
                .requester(requester)
                .store(store)
                .targetType(dto.getTargetType())
                .targetId(dto.getTargetId())
                .requestType(dto.getRequestType())
                .beforeValue(dto.getBeforeValue())
                .afterValue(dto.getAfterValue())
                .targetDate(dto.getTargetDate())
                .reason(dto.getReason())
                .status(RequestStatus.PENDING)
                .build();

        // 🔔 (선택사항) 사장님에게 "새로운 정정 요청이 들어왔습니다" 알림을 보낼 수도 있음

        return new ModificationResponseDto(modificationRepository.save(request));
    }

    // 2. 정정 요청 목록 조회
    public List<ModificationResponseDto> getModifications(Long storeId, RequestStatus status, Long requesterId) {
        List<ModificationRequest> requests;

        if (requesterId != null) {
            requests = modificationRepository.findByRequesterIdOrderByCreatedAtDesc(requesterId);
        } else if (status != null) {
            requests = modificationRepository.findByStoreIdAndStatusOrderByCreatedAtDesc(storeId, status);
        } else {
            requests = modificationRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
        }

        return requests.stream()
                .map(ModificationResponseDto::new)
                .collect(Collectors.toList());
    }

    // 3. 정정 요청 상세 조회
    public ModificationResponseDto getModificationDetail(Long requestId) {
        ModificationRequest request = modificationRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));
        return new ModificationResponseDto(request);
    }

    // 4. 요청 승인/거절 처리 (알림 기능 추가됨!)
    @Transactional
    public ModificationResponseDto updateStatus(Long requestId, RequestStatus newStatus) {
        ModificationRequest request = modificationRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("이미 처리가 완료된 요청입니다.");
        }

        // 상태 변경
        request.updateStatus(newStatus);

        // 승인(APPROVED)일 경우 실제 데이터 수정
        if (newStatus == RequestStatus.APPROVED) {
            applyModificationToTarget(request);
        }

        // 🔔 [알림 전송] 결과(승인/거절)를 알바생에게 알림
        sendNotificationToRequester(request, newStatus);

        return new ModificationResponseDto(request);
    }

    // 5. 요청 삭제
    @Transactional
    public void deleteModification(Long requestId) {
        ModificationRequest request = modificationRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("이미 승인/거절된 요청은 삭제할 수 없습니다.");
        }
        modificationRepository.delete(request);
    }

    // 내부 메서드 1: 실제 데이터 반영
    private void applyModificationToTarget(ModificationRequest request) {
        log.info(">>> [AUTO UPDATE] {} 수정 로직 실행. TargetID: {}", request.getTargetType(), request.getTargetId());

        if (request.getTargetType() == RequestTargetType.ATTENDANCE) {
            attendanceService.updateByRequest(request.getTargetId(), request.getAfterValue());
        } else if (request.getTargetType() == RequestTargetType.SCHEDULE) {
            scheduleService.updateSchedule(request.getTargetId(), request.getAfterValue());
        }
    }

    // 🔔 내부 메서드 2: 알림 전송 로직 분리
    private void sendNotificationToRequester(ModificationRequest request, RequestStatus status) {
        String typeKr = (request.getTargetType() == RequestTargetType.ATTENDANCE) ? "근태" : "스케줄";
        String statusKr = (status == RequestStatus.APPROVED) ? "승인" : "거절";

        String title = "정정 요청 " + statusKr;
        String message = String.format("요청하신 %s(%s) 정정 건이 %s되었습니다.",
                typeKr, request.getTargetDate(), statusKr);

        notificationRepository.save(Notification.builder()
                .user(request.getRequester()) // 요청한 알바생
                .title(title)
                .message(message)
                .type(NotificationType.WORK) // WORK 타입 사용
                .isRead(false)
                .build());

        log.info("🔔 [Notification] 알림 전송 완료: User ID {}", request.getRequester().getId());
    }
}