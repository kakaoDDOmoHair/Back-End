package com.paymate.paymate_server.domain.modification.controller;

import com.paymate.paymate_server.domain.member.enums.UserRole; // 👈 Role enum import 필수!
import com.paymate.paymate_server.domain.modification.dto.ModificationRequestDto;
import com.paymate.paymate_server.domain.modification.dto.ModificationResponseDto;
import com.paymate.paymate_server.domain.modification.enums.RequestStatus;
import com.paymate.paymate_server.domain.modification.service.ModificationService;
import com.paymate.paymate_server.global.jwt.CustomUserDetails;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/modifications")
@RequiredArgsConstructor
public class ModificationController {

    private final ModificationService modificationService;

    // 1. 정정 요청 등록 (POST)
    @PostMapping
    public ResponseEntity<?> createModification(
            HttpServletRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ModificationRequestDto requestDto) {

        if (userDetails == null) {
            String authHeader = request.getHeader("Authorization");
            boolean hasAuth = StringUtils.hasText(authHeader);
            boolean startsWithBearer = hasAuth && authHeader.startsWith("Bearer ");
            log.info("[401 디버깅] POST /modifications | Authorization존재={} | Bearer공백시작={} | response=401 로그인이 필요합니다.", hasAuth, startsWithBearer);
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        ModificationResponseDto response = modificationService.createModification(userDetails.getId(), requestDto);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "requestId", response.getRequestId(),
                        "status", response.getStatus()
                )
        ));
    }

    // 2. 정정 요청 목록 조회 (GET)
    @GetMapping
    public ResponseEntity<List<ModificationResponseDto>> getModifications(
            @RequestParam Long storeId,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Long requesterId) {

        List<ModificationResponseDto> list = modificationService.getModifications(storeId, status, requesterId);
        return ResponseEntity.ok(list);
    }

    // 3. 정정 요청 상세 조회 (GET)
    @GetMapping("/{requestId}")
    public ResponseEntity<ModificationResponseDto> getModificationDetail(@PathVariable Long requestId) {
        return ResponseEntity.ok(modificationService.getModificationDetail(requestId));
    }

    // 4. 요청 승인/거절 처리 (PATCH) - 🛡️ [보안 강화] 사장님만 가능!
    @PatchMapping("/{requestId}/status")
    public ResponseEntity<?> updateStatus(
            HttpServletRequest request,
            @PathVariable Long requestId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            String authHeader = request.getHeader("Authorization");
            boolean hasAuth = StringUtils.hasText(authHeader);
            boolean startsWithBearer = hasAuth && authHeader.startsWith("Bearer ");
            log.info("[401 디버깅] PATCH /modifications/{}/status | Authorization존재={} | Bearer공백시작={} | response=401 로그인이 필요합니다.", requestId, hasAuth, startsWithBearer);
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        // 🚨 [보안 검문소] 사장님(OWNER)이 아니면 403 Forbidden 리턴
        if (userDetails.getUser().getRole() != UserRole.OWNER) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "사장님만 승인/거절을 할 수 있습니다."
            ));
        }

        RequestStatus newStatus = RequestStatus.valueOf(body.get("status"));
        ModificationResponseDto response = modificationService.updateStatus(requestId, newStatus);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "처리 완료",
                "data", Map.of(
                        "requestId", response.getRequestId(),
                        "finalStatus", response.getStatus()
                )
        ));
    }

    // 5. 정정 요청 취소 (DELETE)
    @DeleteMapping("/{requestId}")
    public ResponseEntity<?> deleteModification(@PathVariable Long requestId) {
        modificationService.deleteModification(requestId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "요청이 삭제되었습니다."
        ));
    }
}