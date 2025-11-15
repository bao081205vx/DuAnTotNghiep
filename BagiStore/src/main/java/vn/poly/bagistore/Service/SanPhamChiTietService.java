package vn.poly.bagistore.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.poly.bagistore.dto.VariantUpdateDTO;
import vn.poly.bagistore.model.*;
import vn.poly.bagistore.repository.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SanPhamChiTietService {

    @Autowired
    private SanPhamChiTietRepository sanPhamChiTietRepository;

    @Autowired
    private MauSacRepository mauSacRepository;

    @Autowired
    private KichThuocRepository kichThuocRepository;

    @Autowired
    private TrongLuongRepository trongLuongRepository;

    @Autowired
    private AnhSanPhamRepository anhSanPhamRepository;

    @Transactional
    public Map<String, Object> updateVariant(Integer variantId, VariantUpdateDTO dto) {
        try {
            System.out.println("=== START UPDATING VARIANT ===");
            System.out.println("Variant ID: " + variantId);
            System.out.println("Update DTO: " + dto);

            // Tìm variant theo ID với fetch đầy đủ - sử dụng @EntityGraph để tránh LazyLoading
            SanPhamChiTiet variant = sanPhamChiTietRepository.findById(variantId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết sản phẩm với ID: " + variantId));

            System.out.println("Found variant - ID: " + variant.getId());
            // KHÔNG in toàn bộ object variant.toString() để tránh StackOverflow
            System.out.println("Variant basic info - Gia: " + variant.getGia() + ", SoLuong: " + variant.getSoLuong());

            // ... phần còn lại của method giữ nguyên
            // Cập nhật thông tin cơ bản
            if (dto.getSoLuong() != null) {
                variant.setSoLuong(dto.getSoLuong());
                System.out.println("Updated soLuong: " + dto.getSoLuong());
            }

            if (dto.getGia() != null) {
                variant.setGia(dto.getGia());
                System.out.println("Updated gia: " + dto.getGia());
            }

            if (dto.getTrangThai() != null) {
                variant.setTrangThai(dto.getTrangThai());
                System.out.println("Updated trangThai: " + dto.getTrangThai());
            }

            if (dto.getMoTa() != null) {
                variant.setMoTa(dto.getMoTa());
                System.out.println("Updated moTa length: " + dto.getMoTa().length());
            }

            // Cập nhật ngày sửa
            variant.setNgaySua(LocalDateTime.now());
            System.out.println("Set ngaySua: " + variant.getNgaySua());

            // Xử lý màu sắc - chỉ khi có giá trị
            if (dto.getTenMauSac() != null && !dto.getTenMauSac().isBlank()) {
                System.out.println("Processing color: " + dto.getTenMauSac());
                MauSac color = mauSacRepository.findByTenMauSac(dto.getTenMauSac());
                if (color == null) {
                    System.out.println("Creating new color...");
                    color = new MauSac();
                    color.setTenMauSac(dto.getTenMauSac());
                    color.setTrangThai(true);
                    color = mauSacRepository.save(color);
                    System.out.println("Created new color with ID: " + color.getId());
                } else {
                    System.out.println("Found existing color with ID: " + color.getId());
                }
                variant.setMauSac(color);
            } else {
                System.out.println("No color provided, setting to null");
                variant.setMauSac(null);
            }

            // Xử lý kích thước - chỉ khi có giá trị
            if (dto.getTenKichThuoc() != null && !dto.getTenKichThuoc().isBlank()) {
                System.out.println("Processing size: " + dto.getTenKichThuoc());
                KichThuoc size = kichThuocRepository.findByTenKichThuoc(dto.getTenKichThuoc());
                if (size == null) {
                    System.out.println("Creating new size...");
                    size = new KichThuoc();
                    size.setTenKichThuoc(dto.getTenKichThuoc());
                    size.setTrangThai(true);
                    size = kichThuocRepository.save(size);
                    System.out.println("Created new size with ID: " + size.getId());
                } else {
                    System.out.println("Found existing size with ID: " + size.getId());
                }
                variant.setKichThuoc(size);
            } else {
                System.out.println("No size provided, setting to null");
                variant.setKichThuoc(null);
            }

            // Xử lý trọng lượng - chỉ khi có giá trị
            if (dto.getTenTrongLuong() != null && !dto.getTenTrongLuong().isBlank()) {
                System.out.println("Processing weight: " + dto.getTenTrongLuong());
                TrongLuong weight = trongLuongRepository.findByTenTrongLuong(dto.getTenTrongLuong());
                if (weight == null) {
                    System.out.println("Creating new weight...");
                    weight = new TrongLuong();
                    weight.setTenTrongLuong(dto.getTenTrongLuong());
                    weight.setTrangThai(true);
                    weight = trongLuongRepository.save(weight);
                    System.out.println("Created new weight with ID: " + weight.getId());
                } else {
                    System.out.println("Found existing weight with ID: " + weight.getId());
                }
                variant.setTrongLuong(weight);
            } else {
                System.out.println("No weight provided, setting to null");
                variant.setTrongLuong(null);
            }

            // Xử lý ảnh - chỉ khi có giá trị
            if (dto.getAnhDuongDan() != null && !dto.getAnhDuongDan().isBlank()) {
                System.out.println("Processing image...");

                // Nếu variant đã có ảnh, cập nhật đường dẫn
                if (variant.getAnhSanPham() != null) {
                    AnhSanPham existingImage = variant.getAnhSanPham();
                    existingImage.setDuongDan(dto.getAnhDuongDan());
                    AnhSanPham savedImage = anhSanPhamRepository.save(existingImage);
                    // ensure relation explicitly set
                    variant.setAnhSanPham(savedImage);
                    System.out.println("Updated existing image with ID: " + savedImage.getId());
                } else {
                    // Tạo ảnh mới
                    AnhSanPham newImage = new AnhSanPham();
                    newImage.setDuongDan(dto.getAnhDuongDan());
                    newImage.setTrangThai(true);
                    AnhSanPham savedImage = anhSanPhamRepository.save(newImage);
                    variant.setAnhSanPham(savedImage);
                    System.out.println("Created new image with ID: " + savedImage.getId());
                }
            } else {
                System.out.println("No image provided");
                // Không làm gì nếu không có ảnh mới
            }

            System.out.println("Saving variant...");
            // Lưu variant
            SanPhamChiTiet updatedVariant = sanPhamChiTietRepository.save(variant);
            System.out.println("Variant saved successfully with ID: " + updatedVariant.getId());
            System.out.println("Linked image id after save: " + (updatedVariant.getAnhSanPham() != null ? updatedVariant.getAnhSanPham().getId() : "null"));

            // Tạo response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật chi tiết sản phẩm thành công");
            response.put("variantId", updatedVariant.getId());
            response.put("ngaySua", updatedVariant.getNgaySua());

            System.out.println("=== VARIANT UPDATE COMPLETED ===");
            return response;

        } catch (Exception e) {
            System.err.println("!!! ERROR UPDATING VARIANT !!!");
            System.err.println("Error type: " + e.getClass().getName());
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi cập nhật chi tiết sản phẩm: " + e.getMessage(), e);
        }
    }
    public void deleteVariant(Integer id) {
        try {
            // Tìm variant theo ID
            Optional<SanPhamChiTiet> variantOpt = sanPhamChiTietRepository.findById(id);

            if (variantOpt.isEmpty()) {
                throw new RuntimeException("Không tìm thấy chi tiết sản phẩm với ID: " + id);
            }

            SanPhamChiTiet variant = variantOpt.get();

            // Log thông tin trước khi xóa (không dùng maVariant)
            System.out.println("🗑️ Deleting variant ID: " + variant.getId());

            // Log chi tiết variant để debug
            if (variant.getSanPham() != null) {
                System.out.println("📦 Product: " + variant.getSanPham().getTenSanPham());
            }
            if (variant.getMauSac() != null) {
                System.out.println("🎨 Color: " + variant.getMauSac().getTenMauSac());
            }
            if (variant.getKichThuoc() != null) {
                System.out.println("📏 Size: " + variant.getKichThuoc().getTenKichThuoc());
            }
            System.out.println("💰 Price: " + variant.getGia());
            System.out.println("📊 Quantity: " + variant.getSoLuong());

            // Xóa variant
            sanPhamChiTietRepository.delete(variant);

            System.out.println("✅ Variant deleted successfully: " + id);

        } catch (Exception e) {
            System.err.println("❌ Error deleting variant: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi xóa chi tiết sản phẩm: " + e.getMessage(), e);
        }
    }
}