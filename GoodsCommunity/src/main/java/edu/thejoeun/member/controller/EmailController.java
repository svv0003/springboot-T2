package edu.thejoeun.member.controller;


import edu.thejoeun.member.model.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@SessionAttributes("{authKey}")
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Slf4j
public class EmailController {
    private final EmailService emailService;


    @PostMapping("/signup") // api : /email/signup
    public int signup(@RequestBody Map<String, String> map){
        String email = map.get("email");
        String authKey = emailService.sendMail("signup", email);
        if(authKey != null){
            return 1;
        }
        return 0;
    }

    @PostMapping("/checkAuthKey")
    public int checkAuthKey(@RequestBody Map<String, Object> map){
        log.info("인증키 번호 : {}", map);
        return emailService.checkAuthKey(map);
    }
}



