package vn.poly.bagistore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.poly.bagistore.Service.SanPhamChiTietService;
import vn.poly.bagistore.Service.SanPhamService;
import vn.poly.bagistore.dto.ProductVariantDTO;
import vn.poly.bagistore.dto.VariantUpdateDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@RestController
@RequestMapping("/api/variants")
public class SanPhamChiTietController {

    @Autowired
    private SanPhamChiTietService sanPhamChiTietService;

    @Autowired
    private SanPhamService sanPhamService;
    @Autowired
    private vn.poly.bagistore.Service.CloudinaryService cloudinaryService;
    @Autowired
    private vn.poly.bagistore.repository.SanPhamChiTietRepository sanPhamChiTietRepository;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createVariant(@RequestPart("variant") String variantJson,
                                           @RequestPart(name = "image", required = false) MultipartFile image,
                                           @RequestParam(name = "productId", required = false) Integer productId) {
        try {
            System.out.println("=== CREATING NEW VARIANT ===");
            System.out.println("Variant data: " + variantJson);
            System.out.println("Image provided: " + (image != null && !image.isEmpty()));

            // Parse the JSON into DTO
            ObjectMapper mapper = new ObjectMapper();
            ProductVariantDTO variantDTO = mapper.readValue(variantJson, ProductVariantDTO.class);

            // If the DTO doesn't contain productId, use request param (helps clients that send productId separately)
            if (variantDTO.getProductId() == null && productId != null) {
                variantDTO.setProductId(productId);
                System.out.println("Injected productId from request param: " + productId);
            }

            // Call service to save variant with image and get saved id
            Integer savedVariantId = sanPhamService.saveProductVariant(variantDTO, image);

            // Try to fetch the saved variant to get linked image id for debugging
            Integer linkedImageId = null;
            try {
                if (savedVariantId != null) {
                    var opt = sanPhamChiTietRepository.findById(savedVariantId);
                    if (opt.isPresent()) {
                        var v = opt.get();
                        if (v.getAnhSanPham() != null) linkedImageId = v.getAnhSanPham().getId();
                    }
                }
            } catch (Exception ex) {
                System.err.println("Warning: could not read saved variant for image id: " + ex.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Variant created successfully",
                    "variantId", savedVariantId,
                    "imageId", linkedImageId
            ));

        } catch (Exception e) {
            System.err.println("!!! CONTROLLER ERROR CREATING VARIANT !!!");
            System.err.println("Error: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Error creating product variant",
                    "message", e.getMessage(),
                    "errorType", e.getClass().getName()
            ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVariant(@PathVariable Integer id, @RequestBody VariantUpdateDTO dto) {
        try {
            System.out.println("=== UPDATING VARIANT ID: " + id + " ===");
            System.out.println("Update data received:");
            System.out.println("- maVariant: " + dto.getMaVariant());
            System.out.println("- tenMauSac: " + dto.getTenMauSac());
            System.out.println("- tenKichThuoc: " + dto.getTenKichThuoc());
            System.out.println("- tenTrongLuong: " + dto.getTenTrongLuong());
            System.out.println("- soLuong: " + dto.getSoLuong());
            System.out.println("- gia: " + dto.getGia());
            System.out.println("- trangThai: " + dto.getTrangThai());
            System.out.println("- moTa: " + (dto.getMoTa() != null ? "has value (length: " + dto.getMoTa().length() + ")" : "null"));
            System.out.println("- anhDuongDan: " + (dto.getAnhDuongDan() != null ? "has value (length: " + dto.getAnhDuongDan().length() + ")" : "null"));

            var result = sanPhamChiTietService.updateVariant(id, dto);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("!!! CONTROLLER ERROR UPDATING VARIANT !!!");
            System.err.println("Error: " + e.getClass().getName() + " - " + e.getMessage());

            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Lỗi khi cập nhật chi tiết sản phẩm",
                    "message", e.getMessage(),
                    "errorType", e.getClass().getName()
            ));
        }
    }

    // Support multipart PUT to update variant including an uploaded image file
    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateVariantWithImage(@PathVariable Integer id,
                                                    @RequestPart("variant") String variantJson,
                                                    @RequestPart(name = "image", required = false) MultipartFile image) {
        try {
            System.out.println("=== UPDATING VARIANT (multipart) ID: " + id + " ===");
            ObjectMapper mapper = new ObjectMapper();
            VariantUpdateDTO dto = mapper.readValue(variantJson, VariantUpdateDTO.class);

            if (image != null && !image.isEmpty()) {
                System.out.println("Received image file for update: " + image.getOriginalFilename());
                try {
                    String secure = cloudinaryService.upload(image);
                    dto.setAnhDuongDan(secure);
                    System.out.println("Uploaded image to Cloudinary, secure_url=" + secure);
                } catch (Exception e) {
                    System.err.println("Error uploading image to Cloudinary: " + e.getMessage());
                }
            }

            var result = sanPhamChiTietService.updateVariant(id, dto);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("!!! CONTROLLER ERROR UPDATING VARIANT (multipart) !!!");
            System.err.println("Error: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Lỗi khi cập nhật chi tiết sản phẩm (multipart)",
                    "message", e.getMessage(),
                    "errorType", e.getClass().getName()
            ));
        }
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVariant(@PathVariable Integer id) {
        try {
            System.out.println("=== DELETING VARIANT ID: " + id + " ===");

            // Gọi service để xóa variant
            sanPhamChiTietService.deleteVariant(id);

            System.out.println("✅ Successfully deleted variant ID: " + id);

            return ResponseEntity.ok(Map.of(
                    "message", "Đã xóa chi tiết sản phẩm thành công",
                    "deletedId", id
            ));

        } catch (Exception e) {
            System.err.println("!!! CONTROLLER ERROR DELETING VARIANT !!!");
            System.err.println("Error: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Lỗi khi xóa chi tiết sản phẩm",
                    "message", e.getMessage(),
                    "errorType", e.getClass().getName()
            ));
        }
    }
}