package edu.thejoeun.board.model.service;


import edu.thejoeun.board.model.dto.Board;
import edu.thejoeun.board.model.mapper.BoardMapper;
import edu.thejoeun.common.util.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardServiceImpl  implements BoardService {

    //@Autowired
    // Autowired 보다 RequiredArgsConstructor 처리해주는 것이
    // 상수화하여 Mapper 를 사용할 수 있으므로 안전 -> 내부 메서드나 데이터 변경 불가
    private final BoardMapper boardMapper;
    private final FileUploadService fileUploadService;
    private final SimpMessagingTemplate messagingTemplate; // WebSocket 메세지 전송


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
//    public void createBoard(Board board, MultipartFile imageFile) {
    public void createBoard(Board board, MultipartFile mainImage, List<MultipartFile> detailImage) {
        /*
        1. try-catch 생성한다.
        2. 게시물 ID 생성을 위해 게시물 저장을 먼저 진행한다.
        + (클라이언트가 메인, 상세 이미지를 필수로 업로드하지 않기 때문에 이미지 유무에 따른 작업)
        3. 게시물 ID를 기반으로 이미지 폴더 경로 생성 및 업로드 처리한다.
        4. DB에 이미지 경로 저장한다. (updateBoardImages(board))
        5. websocket을 활용하여 실시간 알림 전송한다.
         */
        try {
            boardMapper.insertBoard(board);
            log.info("게시물 저장 완료 : {}", board.getId());
            boolean imageExist = false;
            if(mainImage != null || !mainImage.isEmpty()) {
                // TODO : uploadMainImage() 생성하여 업로드 관련 작업 진행하기
                String mainImagePath = uploadMainImage(board.getId(), mainImage);
                board.setBoardMainImage(mainImagePath);
                imageExist = true;
                /*
                board : DB와 상호작용할 변수명
                메인 이미지 저장 시 fileUploadService에서 폴더 저장 후 DB 저장
                 */
            }
            if(detailImage != null || !detailImage.isEmpty()) {
                // TODO : uploadDetailImage() 생성하여 업로드 관련 작업 진행하기
                String detailImagePath = uploadDetailImage(board.getId(), detailImage);
                board.setBoardDetailImage(detailImagePath);
                imageExist = true;
            }
            /*
            무조건 실행하는 것이 아닌 mainImage, detailImage 수정 작업 발생 시 업데이트한다.
             */
            if(imageExist) {
                boardMapper.addBoardImage(board);
                log.info("게시물 이미지 경로 DB 업데이트 완료");
            }
            sendBoardNotification(board);
        } catch (Exception e) {
            log.error("게시물 이미지 업로드 중 오류 발생 : {}", e.getMessage());
            throw new RuntimeException("게시물 이미지 업로드에 실패했습니다. : " + e.getMessage());
        }
    }

    /**
     * 게시물 메인 이미지 업로드
     * @param boardId       게시물 ID
     * @param mainImage     메인 이미지 파일
     * @return              업로드 이미지 경로
     * @throws IOException  파일 업로드 실패 시
     */
    private String uploadMainImage(int boardId, MultipartFile mainImage) throws IOException {
        String mainImagePath = fileUploadService.uploadBoardImages(mainImage, boardId, "main");
        log.info("메인 이미지 업로드 완료 : {}", mainImagePath);
        return mainImagePath;
    }

    /**
     * 게시물 상세 이미지 업로드 (최대 5장)
     * @param boardId       게시물 ID
     * @param detailImages   상세 이미지 파일
     * @return              업로드 이미지 경로
     * @throws IOException  파일 업로드 실패 시
     */
    private String uploadDetailImage(int boardId, List<MultipartFile> detailImages) throws IOException {
        List<String> detailImagesPaths = new ArrayList<>();
        int maxImages = Math.min(detailImages.size(), 5);
        for (int i = 0; i < maxImages; i++) {
            MultipartFile detailImageFile = detailImages.get(i);
            if(detailImageFile == null || detailImageFile.isEmpty()) {
                continue;
            }
            /*
            폴더에 이미지 저장 시 detail_번호 형태로 저장된다.
             */
            String imagePath = fileUploadService.uploadBoardImages(detailImageFile, boardId, "detail_" + (i + 1));
            detailImagesPaths.add(imagePath);
            log.info("상세 이미지 {} 업로드 완료 : {}", (i+1), imagePath);
        }
        String result = String.join(",", detailImagesPaths);
        log.info("총 {} 개의 상세 이미지 업로드 완료", detailImagesPaths.size());
        return result;
    }

    /**
     * 게시물 작성 알림 전송
     * @param board 작성된 게시물 정보
     */
    private void sendBoardNotification(Board board) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("msg", "새로운 게시글이 작성되었습니다.");
        notification.put("boardId", board.getId());
        log.info("boardId,{}", board.getId());
        notification.put("title", board.getTitle());
        notification.put("writer", board.getWriter());
        notification.put("timestamp", System.currentTimeMillis());

        // /topic/notifications 을 구독한 모든 클라이언트에게 전송
        messagingTemplate.convertAndSend("/topic/notifications", notification);
        log.info("새 게시글 작성 및 WebSocket 알림 전송 완료 : {}", board.getTitle()); // 개발자 회사 로그용
    }
}
