package com.paymate.paymate_server.domain.member.service;

import com.paymate.paymate_server.domain.member.entity.User;
import com.paymate.paymate_server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.paymate.paymate_server.domain.member.dto.MemberResponseDto;
import com.paymate.paymate_server.domain.member.dto.PasswordChangeRequestDto;
import com.paymate.paymate_server.domain.member.dto.MemberDetailResponseDto;
import com.paymate.paymate_server.domain.member.dto.WithdrawRequestDto;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용으로 설정하여 성능을 최적화합니다.
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 로직
     */
    @Transactional // 저장 작업을 위해 읽기 전용을 해제합니다.
    public Long join(User user) {
        // 1. 중복 회원 검증 (이메일 기준)
        validateDuplicateMember(user.getEmail());

        // 2. 비밀번호 암호화 (보안 필수!)
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.updatePassword(encodedPassword);

        // 3. DB 저장
        memberRepository.save(user);
        return user.getId();
    }

    private void validateDuplicateMember(String email) {
        memberRepository.findByEmail(email)
                .ifPresent(m -> {
                    throw new IllegalStateException("이미 존재하는 이메일입니다.");
                });
    }
    @Transactional(readOnly = true)
    public MemberResponseDto getMyInfo(String email) { // 📍 Long userId -> String email
        User user = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        // MemberResponseDto.from(user) 등의 변환 로직
        return MemberResponseDto.of(user);
    }

    @Transactional
    public void withdraw(WithdrawRequestDto dto) {

        // 1. DTO에 있는 이메일로 사용자를 찾습니다.
        User user = memberRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 사용자를 찾을 수 없습니다: " + dto.getEmail()));

        // 2. 비밀번호 재검증
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        memberRepository.delete(user);
    }
    @Transactional

    public void changePassword(PasswordChangeRequestDto dto) {

        // 1. DTO 안에 들어있는 '이메일'로 사용자를 찾습니다. (ID 대신)
        User user = memberRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 사용자를 찾을 수 없습니다: " + dto.getEmail()));

        // 2. 비밀번호 검증 (기존 로직)
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호가 일치하지 않습니다.");
        }

        // 3. 비밀번호 변경 (기존 로직)
        String encodedNewPassword = passwordEncoder.encode(dto.getNewPassword());
        user.updatePassword(encodedNewPassword);
    }

    /**
     * 알바생 상세 정보 조회 (JOIN 로직 대체)
     */
    @Transactional(readOnly = true)
    public MemberDetailResponseDto getMemberDetail(String email) { // 📍 Long userId -> String email
        User user = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

        return MemberDetailResponseDto.of(user);
    }
}