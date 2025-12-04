package edu.thejoeun.board.model.mapper;

import edu.thejoeun.board.model.dto.Board;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BoardMapper {

    // mapper.xml 에 작성한
    // id 와 메서드명칭은 일치
    List<Board> getAllBoard();

    /**
     * 게시물 클릭하면 상세 조회
     * @param id 에 해당하는
     * @return 게시물 데이터 반환
     */
    Board getBoardById(int id);

    /**
     * 작성한 게시물 저장
     *
     * @param board 게시물 데이터 가져오기
     */
    void insertBoard(Board board);

    /**
     * 게시물 사진 포함 업데이트
     * @param board id, mainImage, detailImage 활용할 것이다.
     *              int id, String board_main_image, String board_detail_image와 같은 형식으로도 매개변수 작성 가능하다.
     */
    void addBoardImage(Board board);

    /**
     * 게시물 데이터 제목, 내용, 저자 수정불가, 업데이트 일자 변경
     * @param board
     */
    void updateBoard(Board board);

    /**
     * 게시물 상세보기 선택했을 경우
     * 해당 게시물의 조회수 증가
     * @param id
     */
    void updateViewCount(int id);
}
