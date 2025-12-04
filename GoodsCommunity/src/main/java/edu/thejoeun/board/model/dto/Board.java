package edu.thejoeun.board.model.dto;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Board {

    private int id;
    private String title;
    private String content;
    private String writer;
    private String boardMainImage;
    private String boardDetailImage;
    /*
    DB 저장 시 List를 문자열 형태로 저장할 것이다.
    ,로 구분지어서 하나의 문자열로 저장할 예정이다.
     */
    private int viewCount;
    private String createdAt;
    private String updatedAt;
    private Integer ranking;
    private String popularUpdateAt;
}










