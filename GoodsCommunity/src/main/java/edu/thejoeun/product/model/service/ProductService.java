package edu.thejoeun.product.model.service;

import edu.thejoeun.product.model.dto.Product;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {

    // 전체 상품 조회
    List<Product> getAllProducts();

    //상품 상세 조회
    Product getProductById(int id);

    // 상품 코드로 조회
    Product getProductByCode(String productCode);

    // 카테고리별 조회
    List<Product> getProductsByCategory(String category);

    // 상품명으로 검색
    List<Product> searchProducts(String keyword);
    /*
    수정 삭제도 동일
    int  = 등록데이터가 0부터 ~ n 개 까지 저장 확인 가능 다수의 상품 등록 사용
    void = 등록데이터의 유무만 확인 가능 성공 / 실패
     */
    // 상품 등록
    void insertProduct(Product product, MultipartFile imageFile);

    // 상품 수정
    void updateProduct(Product product);

    // 상품 삭제
    void deleteProduct(int id);

    // 재고 업데이트
    void updateStock(int id, int quantity);

}
