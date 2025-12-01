package edu.thejoeun.member.controller;

import edu.thejoeun.common.exception.ForbiddenException;
import edu.thejoeun.common.exception.UnauthorizedException;
import edu.thejoeun.common.util.FileUploadService;
import edu.thejoeun.common.util.SessionUtil;
import edu.thejoeun.member.model.dto.Member;
import edu.thejoeun.member.model.service.MemberServiceImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class MemberAPIController {
     private final MemberServiceImpl memberService;
     private final FileUploadService fileUploadService;

    @PostMapping("/login")
    public Map<String, Object> login(
            @RequestBody Map<String, String> loginData, HttpSession session){
        String memberEmail = loginData.get("memberEmail");
        String memberPassword = loginData.get("memberPassword");
        Map<String, Object> res = memberService.loginProcess(memberEmail, memberPassword,session);
        return res;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session){
        return  memberService.logoutProcess(session);
    }

    @GetMapping("/check")
    public Map<String, Object> checkLoginStatus(HttpSession session){
        return memberService.checkLoginStatus(session);
    }

    // const res = axios.post("/api/auth/signup",signupData);
    // PostMapping 만들기
    // mapper.xml -> mapper.java -> service.java -> serviceImpl.java apiController.java
    // 완성

    /**
     * @RequestPart     multipart/form-data 파일 + JSON 파트 데이터를 받을 때
     * @RequestParam    URL 쿼리 파라미터 / HTML Form 파라미터
     * @RequestBody     요청 전체를 객체로 받을 때
     * @param member
     * required = false는 @PathVariable @RequestBody @RequestParam @RequestPart 모두 쓸 수 있는 속성으로,
     * 각 데이터가 필수로 존재하지 않아도 될 때 사용하며, 기본값은 true이다.
     */
    @PostMapping("/signup")
    public void saveSignup(@RequestBody Member member, @RequestPart(required=false) MultipartFile profileImage){
      log.info("===회원가입 요청===");
      log.info("요청 데이터 - 이름 : {}, 이메일 : {}, 프로필 : {}", member.getMemberName(),member.getMemberEmail(), profileImage);

      try {
          memberService.saveMember(member, profileImage);
          log.info("회원가입 성공 - 이메일 : {}",member.getMemberEmail());
      } catch (Exception e){
          log.error("회원가입 실패 - 이메일 : {}, 에러 : {}",member.getMemberEmail(),e.getMessage());
        }

    }


    /**
     * 상품 이미지 업로드를 프로필 사진 업로드처럼 product-images 폴더에 업데이트 되도록 설정
     * -> 이미지 업로드 한 데이터 가져오고 가져가는 서버 - 웹 페이지 작업
     *
     * // fetchMypageEditWithProfile(axios, formData, profileFile,navigate, setIsSubmitting);
     * 받기위한 매개변수 수정 일어날 것
     * @param updateData
     * @param session
     * @return
     */
    @PutMapping("/update")
    public Map<String, Object> updateMypage(@RequestBody Map<String, Object> updateData, HttpSession session){
        log.info("회원정보 수정 요청");
        try{
            Member m = new Member();
            m.setMemberPhone(updateData.get("memberPhone").toString());
            m.setMemberEmail(updateData.get("memberEmail").toString());
            m.setMemberName(updateData.get("memberName").toString());
            m.setMemberAddress(updateData.get("memberAddress").toString());

            // 새 비밀번호가 있는 경우
            String newPassword = (String) updateData.get("memberPassword");
            if(newPassword != null && !newPassword.isEmpty()) {
                m.setMemberPassword(newPassword);
            }

            // 현재 비밀번호
            String currentPassword = (String) updateData.get("currentPassword");
            Map<String, Object> res = memberService.updateMember(m, currentPassword, session);
            // 서비스에서 성공실패에 대한 결과를 res 담고 프론트엔드에 전달
            log.info("회원정보 수정 결과 : {}", res.get("message"));
            return  res;

        } catch (Exception e) {
            log.error("서비스 접근했거나, 서비스 가기 전에 문제가 발생해서 회원정보 수정 실패 - 에러 : {} ",e.getMessage());
            Map<String, Object> res = new HashMap<>();
            res.put("success",false);
            res.put("message","회원정보 수정 중 오류가 발생했습니다.");
            return res;
        }
    }
    // 요청을 받아서 Service로 넘기고 결과를 응답하는 역할만 함
    @PostMapping("/profile-image")
    public ResponseEntity<Map<String, Object>> uploadProfileImage(
            @RequestParam("file")MultipartFile file,
            @RequestParam("memberEmail") String memberEmail, HttpSession session ){

        Map<String, Object> res = new HashMap<>();
        try{
            Member loginUser = SessionUtil.getLoginUser(session);
            String imageUrl = memberService.updateProfileImage(loginUser,memberEmail,file,session);

            res.put("success",true);
            res.put("message","프로필 이미지가 업데이트 되었습니다.");
            res.put("imageUrl",imageUrl);
            log.info("프로필 이미지 업로드 성공 - 이메일:{}, 파일명:{}", memberEmail, file.getOriginalFilename());
            return ResponseEntity.ok(res);
        } catch (UnauthorizedException e) {
                res.put("success",false);
                res.put("message",e.getMessage());
                return ResponseEntity.status(401).body(res);

        } catch (ForbiddenException e) {
            res.put("success",false);
            res.put("message",e.getMessage());
            return ResponseEntity.status(403).body(res);

        } catch (IllegalArgumentException e) {
            res.put("success",false);
            res.put("message",e.getMessage());
            return ResponseEntity.badRequest().body(res);

        } catch (Exception e) {
            log.error("프로필 이미지 업로드 실패 - 이메일:{}, 오류:{}", memberEmail, e.getMessage());
            res.put("success",false);
            res.put("message","서버 오류가 발생했습니다.");
            return ResponseEntity.status(500).body(res);
        }

    }
}


