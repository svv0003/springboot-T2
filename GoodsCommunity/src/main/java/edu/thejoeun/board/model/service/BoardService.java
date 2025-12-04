package edu.thejoeun.board.model.service;

import edu.thejoeun.board.model.dto.Board;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface BoardService {

    List<Board> getAllBoard();
    Board getBoardById(int id);

//    void createBoard(Board board, MultipartFile imageFile);
    void createBoard(Board board, MultipartFile mainImage, List<MultipartFile> detailImage) throws IOException;


}
