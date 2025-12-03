package edu.thejoeun.board.model.service;


import edu.thejoeun.board.model.dto.Board;
import edu.thejoeun.board.model.mapper.BoardMapper;
import edu.thejoeun.common.util.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardServiceImpl  implements BoardService {

    //@Autowired
    // Autowired 보다 RequiredArgsConstructor 처리해주는 것이
    // 상수화하여 Mapper 를 사용할 수 있으므로 안전 -> 내부 메서드나 데이터 변경 불가
    private final BoardMapper boardMapper;
    private final FileUploadService fileUploadService;


    @Override
    public List<Board> getAllBoard() {
        return boardMapper.getAllBoard();
    }

    @Override
    public Board getBoardById(int id) {
        // 게시물 상세조회를 선택했을 때 해당 게시물의 조회수 증가
        boardMapper.updateViewCount(id);

        Board b = boardMapper.getBoardById(id);
        // 게시물 상세조회를 위해 id를 입력하고, 입력한 id 에 해당하는 게시물이
        // 존재할 경우에는 조회된 데이터 전달
        // 존재하지 않을 경우에는 null 전달
        return b != null ? b : null;
    }


    @Override
    public void createBoard(Board board, MultipartFile imageFile) {
        if(imageFile != null && !imageFile.isEmpty()) {
            try {
                int result = boardMapper.insertBoard(board);
                if(result > 0) {
                    String imageUrl = fileUploadService.uploadBoardImage(imageFile, board.getId(), "main");
                    board.setBoardImage(imageUrl);
                    boardMapper.addBoardImage(board);
                    log.info("게시물 등록 완료 - ID : {}, Title : {}, imageUrl : {}",
                            board.getId(), board.getTitle(), imageUrl);
                } else {
                    log.error("게시물 등록 실패 - {}", board.getTitle());
                    throw new RuntimeException("상품 등록에 실패했습니다.");
                }
            }catch(Exception e) {
                log.error("게시물 업로드 실패 : ", e);
                throw new RuntimeException("이미지 업로드에 실패했습니다.");
            }
        } else {
            int result = boardMapper.insertBoard(board);
            if(result > 0) {
                log.info("상품 등록 완료 - ID : {}, Title : {}", board.getId(), board.getTitle());
            } else {
                log.error("상품 등록 실패 - {}", board.getTitle());
                throw  new RuntimeException("상품 등록에 실패했습니다.");
            }
        }
    }
}
