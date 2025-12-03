package edu.thejoeun.product.model.service;


import edu.thejoeun.common.util.FileUploadService;
import edu.thejoeun.product.model.dto.Product;
import edu.thejoeun.product.model.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final FileUploadService fileUploadService;


    /*
     public List<Product> getAllProducts() {
     log.info("전체 상품 조회 : {}".);
     return productMapper.getAllProducts();
     }
     getAllProducts 자료형이 List<Product> 이기 때문에
     변수의 자료형 타입 또한 List<Product> 설정
     변수명칭은 개발자가 원하는 명칭으로 설정
     */
    @Override
    public List<Product> getAllProducts() {
        List<Product> p = productMapper.getAllProducts();
        log.info("전체 상품 조회 : {}", p);
        return p;
    }

    /*
     public Product getProductById(int id) {
        return productMapper.getProductById(id);
    }
    productMapper.getProductById(id) 자료형에 맞는 변수명칭을 생성
    변수명칭과 productMapper.getProductById(id)와 자료형을 동일하게 설정
     */
    @Override
    public Product getProductById(int id) {
        Product p = productMapper.getProductById(id);
        if(p == null) {
            log.warn("상품을 조회할 수 없습니다. ID : {}", id);
            throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        }
        return p;
    }
    /*
    *   public Product getProductByCode(String productCode) {
            return productMapper.getProductByCode(productCode);
        }
    *
    * */
    @Override
    public Product getProductByCode(String productCode) {
        Product p = productMapper.getProductByCode(productCode);
        log.info("상품 코드로 조회 - Code : {}", productCode);
        // exception throw 이용해서 상품이 없을 경우에 대한 예외 처리
        return p;
    }

    @Override
    public List<Product> getProductsByCategory(String category) {
        List<Product> p = productMapper.getProductsByCategory(category);
        log.info("상품 카테고리 조회 - category : {}", p);
        return p;
    }

    @Override
    public List<Product> searchProducts(String keyword) {
        log.info("상품 검색  - Keyword: {}", keyword);
        if(keyword == null || keyword.trim().isEmpty()) {
            log.warn("검색어가 비어있습니다.");
            return null;
        }
        return productMapper.searchProducts(keyword.trim()); //공백제거하여 키워드 검색
    }

    // 상품에 대한 정보 저장 @Transactional 어노테이션이 필수로 붙어야 함
    @Override
    @Transactional
    public void insertProduct(Product product, MultipartFile imageFile) {
        log.info("상품 등록 시작 - {}", product.getProductName());
        // 유효성 검사
        // void validateProduct(product);
        // 메서드를 만들어, 데이터를 저장하기 전에 백엔드에서 한 번 더 유효성 검사 진행

        // 상품 코드 중복 체크
        Product existingProduct = productMapper.getProductByCode(product.getProductCode());
        if(existingProduct != null) {
            log.warn("상품 코드 중복 - Code: {}", product.getProductCode());
            throw new IllegalArgumentException("이미 존재하는 상품입니다.");
        }
        if(imageFile != null && !imageFile.isEmpty()) {
            /**
             * 이미지 파일이 있다면 함께 저장한다.
             */
            try {
                /**
                 * 상품 먼저 등록해서 productId 생성한다.
                 */
                int result = productMapper.insertProduct(product);
                /**
                 * DB에서 rows 행 추가 결과가 1개 이상이라면 insert문 성공했다는 뜻이다.
                 */
                if(result > 0) {
                    String imageUrl = fileUploadService.uploadProductImage(imageFile, product.getId(), "main");
                    /**
                     * 이미지 URL을 product에 설정하고 업데이트한다.
                     */
                    product.setImageUrl(imageUrl);
                    productMapper.updateProduct(product);
                    log.info("상품 등록 완료 - ID : {}, Name : {}, ImageUrl : {}",
                            product.getId(), product.getProductName(), imageUrl);
                } else {
                    log.error("상품 등록 실패 - {}", product.getProductName());
                    throw new RuntimeException("상품 등록에 실패했습니다.");
                }

            }catch(Exception e) {
                log.error("파일 업로드 실패 : ",e);
                throw new RuntimeException("이미지 업로드에 실패했습니다.");
            }
        } else {
            /**
             * 이미지가 없는 경우 상품 정보만 저장한다.
             */
            int result = productMapper.insertProduct(product);
            if(result > 0) {
                log.info("상품 등록 완료 - ID : {}, Name : {}",product.getId(), product.getProductName());
            } else {
                log.error("상품 등록 실패 - {}", product.getProductName());
                throw  new RuntimeException("상품 등록에 실패했습니다.");
            }
        }
    }

    @Override
    @Transactional
    public void updateProduct(Product product, MultipartFile imageFile) {
        log.info("상품 수정 시작 - {}", product.getId());
        // 상품이 존재하는지 확인
        Product existingProduct = productMapper.getProductById(product.getId());
        if(existingProduct == null) {
            log.warn("수정할 상품을 찾을 수 없습니다. : {}", product.getId());
            throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        }
        // 유효성 검사
        // void validateProduct(product);
        // 메서드를 만들어, 데이터를 저장하기 전에 백엔드에서 한 번 더 유효성 검사 진행
        try{
            if(imageFile != null && !imageFile.isEmpty()) {
                // 만약 기존 이미지 파일이 null일 수도 있기 때문에 사용한다.
                if(existingProduct.getImageUrl() != null && !existingProduct.getImageUrl().isEmpty()) {
                    boolean deleteExistingImageResult = fileUploadService.deleteFile(existingProduct.getImageUrl());
                    if(deleteExistingImageResult) log.info("기존 이미지 삭제 성공 : {}", existingProduct.getImageUrl());
                    else log.warn("기존 이미지 삭제 실패 : {}", existingProduct.getImageUrl());
                }
                String imageUrl = fileUploadService.uploadProductImage(imageFile, product.getId(), "main");
                product.setImageUrl(imageUrl);
            } else{
                // 이미지를 수정하지 않은 경우 기본 이미지 유지
                product.setImageUrl(existingProduct.getImageUrl());
            }
            int result = productMapper.updateProduct(product);
            if(result > 0) {
                log.info("상품 수정 완료 - ID : {}",product.getId());
            } else {
                log.error("상품 수정 실패 ID : {}",product.getId());
                throw  new RuntimeException("상품 수정에 실패했습니다.");
            }
        } catch (Exception e) {
            log.error("상품 수정 실패 ID : {}",e.getMessage());
            throw  new RuntimeException("상품 수정에 실패했습니다.");
        }
    }

    @Override
    @Transactional
    public void deleteProduct(int id) {
        log.info("상품 삭제 시작 - {}", id);

        // 상품이 존재하는지 확인
        Product existingProduct = productMapper.getProductById(id);
        if(existingProduct == null) {
            log.warn("삭제할 상품을 찾을 수 없습니다. : {}", id);
            throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        }
        // 유효성 검사
        // void validateProduct(product);
        // 메서드를 만들어, 데이터를 저장하기 전에 백엔드에서 한 번 더 유효성 검사 진행

        int result = productMapper.deleteProduct(id);
        if(result > 0) {
            log.info("상품 삭제 완료 - ID : {}",id);
        } else {
            log.error("상품 삭제 실패 ID : {}",id);
            throw  new RuntimeException("상품 삭제에 실패했습니다.");
        }
    }

    @Override
    @Transactional
    public void updateStock(int id, int quantity) {
        log.info("재고 업데이트 -ID : {}, Quantity : {}", id, quantity);

        // 상품이 존재하는지 확인
        Product existingProduct = productMapper.getProductById(id);
        if(existingProduct == null) {
            log.warn("상품을 찾을 수 없습니다. : {}", id);
            throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        }
        // 상품재고가 음수가 될 수 없도록 설정
        int newStock = existingProduct.getStockQuantity() + quantity;
        if(newStock < 0) {
            log.warn("재고는 음수가 될 수 없습니다. Current : {}, Change : {}",
                    existingProduct.getStockQuantity(), quantity);
        }

        int result = productMapper.updateStock(id,quantity);
        if(result > 0) {
            log.info("재고 업데이트 완료 - ID : {}, new Stock : {}",id,quantity);
        } else {
            log.error("재고 업데이트 실패  - ID : {}", id);
            throw  new RuntimeException("재고 업데이트에 실패했습니다.");
        }
    }
}
