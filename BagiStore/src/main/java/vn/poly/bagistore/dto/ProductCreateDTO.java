package vn.poly.bagistore.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProductCreateDTO {
    private String tenSanPham;
    private String moTa; // Thêm trường mô tả
    private Boolean trangThai;
    private String tenThuongHieu;
    private String tenChatLieu;
    private String tenNhaSanXuat;
    private String tenXuatXu;
    private String tenDungTich;
    private List<VariantCreateDTO> variants;

}