package edu.thejoeun.member.model.service;


import edu.thejoeun.common.exception.ForbiddenException;
import edu.thejoeun.common.exception.UnauthorizedException;
import edu.thejoeun.common.util.FileUploadService;
import edu.thejoeun.common.util.SessionUtil;
import edu.thejoeun.member.model.dto.Member;
import edu.thejoeun.member.model.mapper.MemberMapper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;
    private final FileUploadService fileUploadService;
    BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    @Override
    public Member login(String memberEmail, String memberPassword) {
        Member member = memberMapper.getMemberByEmail(memberEmail);
        if (member == null) {
            return null;
        }
        if (!bCryptPasswordEncoder.matches(memberPassword, member.getMemberPassword())) {
            return null;
        }
        member.setMemberPassword(null);
        return member;
    }

    @Transactional
    @Override
    public void saveMember(Member member, MultipartFile profileImage) {

        try{
            if (profileImage != null && !profileImage.isEmpty()) {
                // throw new IllegalArgumentException("파일이 비어있습니다.");
                String contentType = profileImage.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
                }
                if (profileImage.getSize() > 5 * 1024 * 1024) {
                    throw new IllegalArgumentException("파일 크기는 5MB를 초과할 수 없습니다.");
                }
                String imageUrl = fileUploadService.uploadProfileImage(profileImage);
                member.setMemberProfileImage(imageUrl);
                log.info("프로필 이미지 업로드 완료");
            } else {
                /*
                한 번 더 처리한다.
                재활용할 수 있도록 설정하지 않는 한 모든 경우의 수를 대비해야 한다.
                 */
                member.setMemberProfileImage(null);
                log.info("기본 프로필 이미지 설정");
            }
        } catch (Exception e) {
            log.error("회원가입 중 오류 발생 : {}", e.getMessage());;
        }

        String originPW = member.getMemberPassword(); // 기존 클라이언트 비밀번호 가져오기
        String encodedPw = bCryptPasswordEncoder.encode(originPW); // 비밀번호 암호화
        member.setMemberPassword(encodedPw); // 암호화처리된 비밀번호로 교체

        // 교체된 비밀번호 포함해서 저장
        // 비밀번호 암호화해서 저장
        memberMapper.saveMember(member);
    }

    @Transactional
    @Override
    public Map<String, Object> updateMember(Member member, String currentPassword, HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        try {
            // 현재 로그인된 사용자 정보 가져오기
            Member loginUser = SessionUtil.getLoginUser(session);
            if (loginUser == null) {
                res.put("success", false);
                res.put("message", "로그인이 필요합니다.");
                return res;
            }
            // DB 에서 최신 정보 가져오기
            Member m = memberMapper.getMemberByEmail(member.getMemberEmail());
            // id where 조건으로 / 현재 비밀번호와 비밀번호 변경할 때 작성한 현재 비밀번호가 일치하는지 확인
            // 비밀번호 변경하는 경우
            if (currentPassword != null && !currentPassword.isEmpty()) {
                // 현재 비밀번호와 DB에 저장된 비밀번호가 일치하는지 확인
                if (!bCryptPasswordEncoder.matches(currentPassword, m.getMemberPassword())) {
                    res.put("success", false);
                    res.put("message", "wrongPassword");
                    log.warn("비밀번호 불일치 - 이메일 : {}", loginUser.getMemberEmail());
                    return res;
                }

                // 새 비밀번호 암호화 처리해서 저장할 수 있도록 로직 작성
                if (member.getMemberPassword() != null && !member.getMemberPassword().isEmpty()) {
                    String encodePw = bCryptPasswordEncoder.encode(member.getMemberPassword());
                    member.setMemberPassword(encodePw);
                }

            } else {
                // 비밀번호 변경하지 않은 경우 기존 비밀번호 유지
                member.setMemberPassword(m.getMemberPassword());
            }
            member.setMemberId(m.getMemberId());
            memberMapper.updateMember(member);
            // 수정된 db 내역으로 session 업데이트
            Member updateMember = memberMapper.getMemberByEmail(member.getMemberEmail());
            updateMember.setMemberPassword(null);
            SessionUtil.setLoginUser(session, updateMember);

            res.put("success", true);
            res.put("message", "success");
            log.info("회원정보 수정 성공 - 이메일 : {}", loginUser.getMemberEmail());

        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "회원정보 수정 중 오류가 발생했습니다.");
            log.error("회원정보 수정 실패 - 에러 {}", e.getMessage());
        }
        return res;
    }

    public Map<String, Object> loginProcess(String memberEmail, String memberPassword, HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        Member m = login(memberEmail, memberPassword);
        if (m == null) {
            res.put("success", false);
            res.put("message", "이메일 또는 비밀번호가 일치하지 않습니다.");
            log.warn("로그인 실패: {}", memberEmail);
            return res;
        }
        SessionUtil.setLoginUser(session, m);
        res.put("success", true);
        res.put("message", "로그인 성공");
        res.put("user", m);
        log.info("로그인 성공 : {}", m.getMemberEmail());
        return res;
    }

    /**
     * 로그아웃 처리
     *
     * @param session 로그인된 세션 정보 가져와서 로그아웃 처리 후
     * @return 처리결과 반환
     */
    public Map<String, Object> logoutProcess(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        SessionUtil.invalidateLoginUser(session);
        res.put("success", true);
        res.put("message", "로그아웃 성공");

        return res;
    }

    /**
     * 로그인 상태확인
     *
     * @param session 현재 세션을 가져온 후
     * @return 로그인 이 되어있으면 로그인이 되어있는 상태로 반환
     */
    public Map<String, Object> checkLoginStatus(HttpSession session) {
        Map<String, Object> res = new HashMap<>();
        Member loginUser = (Member) session.getAttribute("loginUser");

        if (loginUser == null) {
            res.put("loggedIn", false);
            res.put("user", null);
            log.debug("로그인 상태 확인: 로그인되지 않음");
        } else {
            res.put("loggedIn", true);
            res.put("user", loginUser);
            log.debug("로그인 상태 확인 : {}", loginUser.getMemberEmail());
        }
        return res;
    }

    // 클라이언트측에서 발생하는 문제를 이중으로 보안하기도 하고,
    // 개발 해커 블랙클라이언트로부터 회사 서비스를 보호하기 위한 예외 차단 처리
    @Transactional
    @Override
    public String updateProfileImage(Member loginUser, String memberEmail, MultipartFile file, HttpSession session) throws IOException {
        // UnauthorizedException = IllegalStateException
        if (loginUser == null) {
            throw new UnauthorizedException("로그인이 필요합니다.");
        }
        //  ForbiddenException  =  SecurityException
        // 본인 확인
        if (!loginUser.getMemberEmail().equals(memberEmail)) {
            throw new ForbiddenException("본인의 프로필만 수정할 수 있습니다.");
        }

        // 파일 유효성 검증
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }

        // 이미지 파일인지 확인
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("파일 크기는 5MB를 초과할 수 없습니다.");
        }

        // 기존 프로필 이미지 삭제
        if (loginUser.getMemberProfileImage() != null) {
            // 삭제 관련 기능 FileUploadService 에서 작성 후 기능 추가
        }

        // 새 이미지 업로드
        // memberProfileImage 을 넣어주어야함 setImageUrl 사용
        // file ->
        String imageUrl = fileUploadService.uploadProfileImage(file);

        // DB  업데이트
        // 작동하기 전에 중간에  상태 확인 후 작동
        // 세션 업데이트
        loginUser.setMemberProfileImage(imageUrl);
        SessionUtil.setLoginUser(session, loginUser);

        memberMapper.updateProfileImage(memberEmail, imageUrl);

        log.info("프로필 이미지 DB 업데이트 완료 - 이메일: {}", memberEmail);

        return imageUrl;
    }
}
