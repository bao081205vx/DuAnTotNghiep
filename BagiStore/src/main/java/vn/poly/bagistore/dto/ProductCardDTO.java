package vn.poly.bagistore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductCardDTO {
    private Integer id;
    private String tenSanPham;
    private String image; // representative image URL
    private Double minPrice;
    private Double maxPrice;
}
