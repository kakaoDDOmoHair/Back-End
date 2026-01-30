package com.paymate.paymate_server.domain.member.service;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.repository.AccountRepository;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import com.paymate.paymate_server.domain.store.entity.Employment;
import com.paymate.paymate_server.domain.store.entity.Store;
import com.paymate.paymate_server.domain.store.repository.EmploymentRepository;
import com.paymate.paymate_server.domain.member.dto.MemberResponseDto;
import com.paymate.paymate_server.domain.member.dto.PasswordChangeRequestDto;
import com.paymate.paymate_server.domain.member.dto.MemberDetailResponseDto;
import com.paymate.paymate_server.domain.member.dto.WithdrawRequestDto;
import com.paymate.paymate_server.domain.member.entity.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional; // 🌟 [필수] 이게 빠져있었습니다!

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmploymentRepository employmentRepository;
    private final AccountRepository accountRepository;

    /**
     * 회원가입 로직
     */
    @Transactional
    public Long join(User user) {
        validateDuplicateMember(user);
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.updatePassword(encodedPassword);
        memberRepository.save(user);
        return user.getId();
    }

    private void validateDuplicateMember(User user) {
        memberRepository.findByEmail(user.getEmail())
                .ifPresent(m -> { throw new IllegalStateException("이미 존재하는 이메일입니다."); });

        if (memberRepository.existsByUsername(user.getUsername())) {
            throw new IllegalStateException("이미 존재하는 아이디입니다.");
        }
    }

    /**
     * 내 정보 조회 (알바생 storeId 로직 포함)
     */
    public MemberResponseDto getMyInfo(String username) {
        // 1. 유저 조회
        User user = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        // 2. Store 찾기 로직 (사장님 vs 알바생)
        Store store = null;
        Long storeId = null;
        
        // 사장님인 경우: user.getStore()에서 직접 가져옴
        if (user.getStore() != null) {
            store = user.getStore();
            storeId = store.getId();
        } else {
            // 알바생인 경우: Employment 테이블에서 조회
            Optional<Employment> employment = employmentRepository.findByEmployee_Id(user.getId());
            if (employment.isPresent()) {
                store = employment.get().getStore();
                storeId = store.getId();
            }
        }
        
        // Store 엔티티의 lazy loading 필드들을 명시적으로 초기화
        if (store != null) {
            // 필드 접근으로 프록시 초기화 (Hibernate가 자동으로 처리)
            store.getLatitude();
            store.getLongitude();
            store.getWifiInfo();
        }

        // 🌟 3. [추가] accountId(계좌 ID) 찾기 로직
        // ID가 가장 높은(가장 최근 등록된) 계좌 하나만 가져옵니다.
        Long accountId = accountRepository.findFirstByUserOrderByIdDesc(user)
                .map(Account::getId)
                .orElse(null);

        // 4. DTO 생성 (storeId, accountId, store 정보를 같이 넘김)
        return MemberResponseDto.of(user, storeId, accountId, store);
    }

    /**
     * 회원 탈퇴
     */
    @Transactional
    public void withdraw(WithdrawRequestDto dto) {
        User user = memberRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        memberRepository.delete(user);
    }

    /**
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(PasswordChangeRequestDto dto) {
        User user = memberRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호가 일치하지 않습니다.");
        }

        String encodedNewPassword = passwordEncoder.encode(dto.getNewPassword());
        user.updatePassword(encodedNewPassword);
    }

    /**
     * 알바생 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public MemberDetailResponseDto getMemberDetail(String username) {
        User user = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        return MemberDetailResponseDto.of(user);
    }

    /**
     * FCM 토큰 업데이트 (수정됨: username 기반)
     */
    @Transactional
    public void updateFcmToken(String username, String token) { // 📍 email -> username 변경
        // 컨트롤러에서 userDetails.getUsername()을 넘겨주므로 여기서도 username으로 찾아야 정확합니다.
        User user = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        user.updateFcmToken(token);
    }

    /**
     * 생일 등록/업데이트
     */
    @Transactional
    public void updateBirthDate(String username, String birthDate) {
        // 생일 형식 검증 (6자리 숫자)
        if (birthDate == null || !birthDate.matches("^\\d{6}$")) {
            throw new IllegalArgumentException("생년월일은 6자리 숫자(예: 980101)로 입력해주세요.");
        }

        User user = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        user.updateBirthDate(birthDate);
    }
}