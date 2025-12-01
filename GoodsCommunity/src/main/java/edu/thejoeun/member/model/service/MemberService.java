package edu.thejoeun.member.model.service;

import edu.thejoeun.member.model.dto.Member;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface MemberService {
    /**
     * 프로젝트에서 기능 명칭을 지정하는 공간
     */
    Member login(String memberEmail, String memberPassword);

    void saveMember(Member member, MultipartFile profileImage);

    Map<String, Object> updateMember(Member member, String currentPassword, HttpSession session);

    String updateProfileImage(Member loginUser, String memberEmail, MultipartFile file, HttpSession session) throws IOException;

}
