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
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 로직 (아이디 & 이메일 중복체크 포함)
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

        // [필수] 아이디 중복 체크
        if (memberRepository.existsByUsername(user.getUsername())) {
            throw new IllegalStateException("이미 존재하는 아이디입니다.");
        }
    }

    /**
     * [변경] 내 정보 조회 (Email -> Username)
     */
    @Transactional(readOnly = true)
    public MemberResponseDto getMyInfo(String username) { // 📍 인자 이름 변경
        // 📍 findByEmail -> findByUsername
        User user = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));
        return MemberResponseDto.of(user);
    }

    /**
     * [변경] 회원 탈퇴 (Email -> Username)
     */
    @Transactional
    public void withdraw(WithdrawRequestDto dto) {
        // DTO 안에도 email 대신 username이 들어있어야 합니다!
        // (만약 DTO를 아직 안 고쳤다면, 컨트롤러에서 넘겨준 username을 바로 쓰셔도 됩니다)

        // 📍 findByEmail -> findByUsername
        User user = memberRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        memberRepository.delete(user);
    }

    /**
     * [변경] 비밀번호 변경 (Email -> Username)
     */
    @Transactional
    public void changePassword(PasswordChangeRequestDto dto) {
        // 📍 findByEmail -> findByUsername
        User user = memberRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("기존 비밀번호가 일치하지 않습니다.");
        }

        String encodedNewPassword = passwordEncoder.encode(dto.getNewPassword());
        user.updatePassword(encodedNewPassword);
    }

    /**
     * [변경] 알바생 상세 정보 조회
     */
    @Transactional(readOnly = true)
    public MemberDetailResponseDto getMemberDetail(String username) { // 📍 Email -> Username
        // 📍 findByEmail -> findByUsername
        User user = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        return MemberDetailResponseDto.of(user);
    }
}