package vn.poly.bagistore.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.poly.bagistore.dto.*;
import vn.poly.bagistore.model.*;
import vn.poly.bagistore.repository.*;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

@Service
public class SanPhamService {
    @Autowired
    SanPhamRepository sanPhamRepository;
    @Autowired
    SanPhamChiTietRepository sanPhamChiTietRepository;
    @Autowired
    MauSacRepository mauSacRepository;
    @Autowired
    KichThuocRepository kichThuocRepository;
    @Autowired
    ChatLieuRepository chatLieuRepository;
    @Autowired
    ThuongHieuRepository thuongHieuRepository;
    @Autowired
    TrongLuongRepository trongLuongRepository;
    @Autowired
    AnhSanPhamRepository anhSanPhamRepository;
    @Autowired
    CloudinaryService cloudinaryService;
    @Autowired
    NhaSanXuatRepository nhaSanXuatRepository;
    @Autowired
    XuatXuRepository xuatXuRepository;
    @Autowired
    DungTichRepository dungTichRepository;

    @Transactional
    public SanPham createProductWithVariants(ProductCreateDTO dto) {
        try {
            System.out.println("=== START CREATING PRODUCT ===");
            System.out.println("Product name: " + dto.getTenSanPham());
            System.out.println("Common description for all variants: " + dto.getMoTa());

            // Tạo sản phẩm mới
            SanPham p = new SanPham();
            p.setTenSanPham(dto.getTenSanPham());
            p.setTrangThai(dto.getTrangThai() != null ? dto.getTrangThai() : true);
            p.setNgayTao(LocalDateTime.now());

            // Xử lý thương hiệu
            if (dto.getTenThuongHieu() != null && !dto.getTenThuongHieu().isBlank()) {
                ThuongHieu brand = thuongHieuRepository.findByTenThuongHieu(dto.getTenThuongHieu());
                if (brand == null) {
                    brand = new ThuongHieu();
                    brand.setTenThuongHieu(dto.getTenThuongHieu());
                    brand.setTrangThai(true);
                    brand = thuongHieuRepository.save(brand);
                    System.out.println("Created new brand: " + brand.getTenThuongHieu());
                }
                p.setThuongHieu(brand);
            }

            // Xử lý chất liệu
            if (dto.getTenChatLieu() != null && !dto.getTenChatLieu().isBlank()) {
                ChatLieu material = chatLieuRepository.findByTenChatLieu(dto.getTenChatLieu());
                if (material == null) {
                    material = new ChatLieu();
                    material.setTenChatLieu(dto.getTenChatLieu());
                    material.setTrangThai(true);
                    material = chatLieuRepository.save(material);
                    System.out.println("Created new material: " + material.getTenChatLieu());
                }
                p.setChatLieu(material);
            }

            // Xử lý nhà sản xuất
            if (dto.getTenNhaSanXuat() != null && !dto.getTenNhaSanXuat().isBlank()) {
                NhaSanXuat manufacturer = nhaSanXuatRepository.findByTenNhaSanXuat(dto.getTenNhaSanXuat());
                if (manufacturer == null) {
                    manufacturer = new NhaSanXuat();
                    manufacturer.setTenNhaSanXuat(dto.getTenNhaSanXuat());
                    manufacturer.setTrangThai(true);
                    manufacturer = nhaSanXuatRepository.save(manufacturer);
                    System.out.println("Created new manufacturer: " + manufacturer.getTenNhaSanXuat());
                }
                p.setNhaSanXuat(manufacturer);
            }

            // Xử lý xuất xứ
            if (dto.getTenXuatXu() != null && !dto.getTenXuatXu().isBlank()) {
                XuatXu origin = xuatXuRepository.findByTenXuatXu(dto.getTenXuatXu());
                if (origin == null) {
                    origin = new XuatXu();
                    origin.setTenXuatXu(dto.getTenXuatXu());
                    origin.setTrangThai(true);
                    origin = xuatXuRepository.save(origin);
                    System.out.println("Created new origin: " + origin.getTenXuatXu());
                }
                p.setXuatXu(origin);
            }

            // Xử lý dung tích
            if (dto.getTenDungTich() != null && !dto.getTenDungTich().isBlank()) {
                DungTich volume = dungTichRepository.findByDungTich(dto.getTenDungTich());
                if (volume == null) {
                    volume = new DungTich();
                    volume.setTenDungTich(dto.getTenDungTich());
                    volume.setTrangThai(true);
                    volume = dungTichRepository.save(volume);
                    System.out.println("Created new volume: " + volume.getTenDungTich());
                }
                p.setDungTich(volume);
            }

            // Tạo mã sản phẩm
            p.setMaSanPham("SP" + System.currentTimeMillis());

            System.out.println("Saving product...");
            SanPham savedProduct = sanPhamRepository.save(p);
            System.out.println("Product saved with ID: " + savedProduct.getId() + " at " + savedProduct.getNgayTao());

            // Generate QR for product
            try {
                String productQrPath = generateAndSaveProductQr(savedProduct);
                System.out.println("Generated product QR: " + productQrPath);
            } catch (Exception ex) {
                System.err.println("Error generating product QR: " + ex.getMessage());
            }

            if (dto.getVariants() != null && !dto.getVariants().isEmpty()) {
                for (VariantCreateDTO variantDTO : dto.getVariants()) {
                    System.out.println("Processing variant: " + variantDTO.getTenMauSac() + " - " + variantDTO.getTenKichThuoc());

                    SanPhamChiTiet variant = new SanPhamChiTiet();
                    variant.setSanPham(savedProduct);
                    variant.setNgayTao(LocalDateTime.now());


                    // SET MÔ TẢ CHUNG CHO TẤT CẢ BIẾN THỂ
                    if (dto.getMoTa() != null && !dto.getMoTa().isBlank()) {
                        variant.setMoTa(dto.getMoTa());
                        System.out.println("✓ Set description for variant: " + dto.getMoTa().substring(0, Math.min(50, dto.getMoTa().length())) + "...");
                    } else {
                        variant.setMoTa(null);
                        System.out.println("✗ No description for variants");
                    }

                    // Xử lý màu sắc
                    if (variantDTO.getTenMauSac() != null && !variantDTO.getTenMauSac().isBlank()) {
                        MauSac color = mauSacRepository.findByTenMauSac(variantDTO.getTenMauSac());
                        if (color == null) {
                            color = new MauSac();
                            color.setTenMauSac(variantDTO.getTenMauSac());
                            color.setTrangThai(true);
                            color = mauSacRepository.save(color);
                            System.out.println("Created new color: " + color.getTenMauSac());
                        }
                        variant.setMauSac(color);
                    }

                    // Xử lý kích thước
                    if (variantDTO.getTenKichThuoc() != null && !variantDTO.getTenKichThuoc().isBlank()) {
                        KichThuoc size = kichThuocRepository.findByTenKichThuoc(variantDTO.getTenKichThuoc());
                        if (size == null) {
                            size = new KichThuoc();
                            size.setTenKichThuoc(variantDTO.getTenKichThuoc());
                            size.setTrangThai(true);
                            size = kichThuocRepository.save(size);
                            System.out.println("Created new size: " + size.getTenKichThuoc());
                        }
                        variant.setKichThuoc(size);
                    }

                    // Xử lý trọng lượng
                    if (variantDTO.getTenTrongLuong() != null && !variantDTO.getTenTrongLuong().isBlank()) {
                        TrongLuong weight = trongLuongRepository.findByTenTrongLuong(variantDTO.getTenTrongLuong());
                        if (weight == null) {
                            weight = new TrongLuong();
                            weight.setTenTrongLuong(variantDTO.getTenTrongLuong());
                            weight.setTrangThai(true);
                            weight = trongLuongRepository.save(weight);
                            System.out.println("Created new weight: " + weight.getTenTrongLuong());
                        }
                        variant.setTrongLuong(weight);
                    }
                    if (variantDTO.getAnhDuongDan() != null && !variantDTO.getAnhDuongDan().isBlank()) {
                        System.out.println("Processing image for variant: " + variantDTO.getAnhDuongDan());
                        // If the DTO already contains a Cloudinary URL, store it directly
                        String maybeUrl = variantDTO.getAnhDuongDan();
                        AnhSanPham anh = new AnhSanPham();
                        anh.setDuongDan(maybeUrl);
                        anh.setTrangThai(true);
                        AnhSanPham savedAnh = anhSanPhamRepository.save(anh);
                        System.out.println("✓ Image saved with ID: " + savedAnh.getId());
                        variant.setAnhSanPham(savedAnh);
                    } else {
                        System.out.println("No image for this variant");
                        variant.setAnhSanPham(null);
                    }

                    variant.setGia(variantDTO.getGia() != null ? variantDTO.getGia() : 0.0);
                    variant.setSoLuong(variantDTO.getSoLuong() != null ? variantDTO.getSoLuong() : 0);
                    variant.setTrangThai(variantDTO.getTrangThai() != null ? variantDTO.getTrangThai() : true);

                    System.out.println("Saving variant with image reference...");
                    SanPhamChiTiet savedVariant = sanPhamChiTietRepository.save(variant);
                    System.out.println("✓ Variant saved with ID: " + savedVariant.getId());
                    // Generate QR for variant
                    try {
                        String variantQrPath = generateAndSaveVariantQr(savedVariant);
                        System.out.println("Generated variant QR: " + variantQrPath);
                    } catch (Exception ex) {
                        System.err.println("Error generating variant QR: " + ex.getMessage());
                    }
                    if (dto.getVariants() != null && !dto.getVariants().isEmpty()) {
                        if (variantDTO.getAnhDuongDan() != null && !variantDTO.getAnhDuongDan().isBlank()) {
                            String imagePath = variantDTO.getAnhDuongDan();

                            // If imagePath refers to a local file, try to upload it to Cloudinary and store secure_url
                            if (imagePath.startsWith("file://") || imagePath.startsWith("C:\\") || imagePath.startsWith("/")) {
                                try {
                                    // upload local file to Cloudinary via server-side service
                                    java.io.File f = new java.io.File(imagePath.replaceFirst("^file://", ""));
                                    if (f.exists() && f.isFile()) {
                                        // wrap as MultipartFile-like via org.springframework.mock.web.MockMultipartFile isn't ideal here; instead skip local-to-cloud for now
                                        String baseUrl = convertImageToBase64(imagePath); // keep previous behavior as fallback
                                        AnhSanPham anh = new AnhSanPham();
                                        anh.setDuongDan(baseUrl);
                                        anh.setTrangThai(true);
                                        AnhSanPham savedAnh = anhSanPhamRepository.save(anh);
                                        variant.setAnhSanPham(savedAnh);
                                    }
                                } catch (Exception e) {
                                    System.err.println("Error converting image to base64: " + e.getMessage());
                                }
                            } else {
                                // Treat given string as a URL (Cloudinary secure_url or remote URL) and store it directly
                                AnhSanPham anh = new AnhSanPham();
                                anh.setDuongDan(imagePath);
                                anh.setTrangThai(true);
                                AnhSanPham savedAnh = anhSanPhamRepository.save(anh);
                                variant.setAnhSanPham(savedAnh);
                            }
                        }
                    }
                }
            }

            System.out.println("=== PRODUCT CREATION COMPLETED ===");
            return savedProduct;

        } catch (Exception e) {
            System.err.println("!!! ERROR CREATING PRODUCT !!!");
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi tạo sản phẩm: " + e.getMessage(), e);
        }
    }

    /**
     * New helper that accepts a map of filename -> MultipartFile. It will match variant.anhDuongDan
     * values against filenames and convert matched files to base64 to store in AnhSanPham.
     */
    public SanPham createProductWithVariantsAndFiles(ProductCreateDTO dto, Map<String, org.springframework.web.multipart.MultipartFile> fileMap, org.springframework.web.multipart.MultipartFile[] uploadedFiles) {
        try {
            // Use existing create flow but when encountering a variant.anhDuongDan matching a filename,
            // convert the MultipartFile to base64 and save as AnhSanPham.

            SanPham p = new SanPham();
            p.setTenSanPham(dto.getTenSanPham());
            p.setTrangThai(dto.getTrangThai() != null ? dto.getTrangThai() : true);
            p.setNgayTao(LocalDateTime.now());
            p.setMaSanPham(generateProductCode());

            // Xử lý thương hiệu
            if (dto.getTenThuongHieu() != null && !dto.getTenThuongHieu().isBlank()) {
                ThuongHieu brand = thuongHieuRepository.findByTenThuongHieu(dto.getTenThuongHieu());
                if (brand == null) {
                    brand = new ThuongHieu();
                    brand.setTenThuongHieu(dto.getTenThuongHieu());
                    brand.setTrangThai(true);
                    brand = thuongHieuRepository.save(brand);
                }
                p.setThuongHieu(brand);
            }

            // Xử lý chất liệu
            if (dto.getTenChatLieu() != null && !dto.getTenChatLieu().isBlank()) {
                ChatLieu material = chatLieuRepository.findByTenChatLieu(dto.getTenChatLieu());
                if (material == null) {
                    material = new ChatLieu();
                    material.setTenChatLieu(dto.getTenChatLieu());
                    material.setTrangThai(true);
                    material = chatLieuRepository.save(material);
                }
                p.setChatLieu(material);
            }

            // Xử lý nhà sản xuất
            if (dto.getTenNhaSanXuat() != null && !dto.getTenNhaSanXuat().isBlank()) {
                NhaSanXuat manufacturer = nhaSanXuatRepository.findByTenNhaSanXuat(dto.getTenNhaSanXuat());
                if (manufacturer == null) {
                    manufacturer = new NhaSanXuat();
                    manufacturer.setTenNhaSanXuat(dto.getTenNhaSanXuat());
                    manufacturer.setTrangThai(true);
                    manufacturer = nhaSanXuatRepository.save(manufacturer);
                }
                p.setNhaSanXuat(manufacturer);
            }

            // Xử lý xuất xứ
            if (dto.getTenXuatXu() != null && !dto.getTenXuatXu().isBlank()) {
                XuatXu origin = xuatXuRepository.findByTenXuatXu(dto.getTenXuatXu());
                if (origin == null) {
                    origin = new XuatXu();
                    origin.setTenXuatXu(dto.getTenXuatXu());
                    origin.setTrangThai(true);
                    origin = xuatXuRepository.save(origin);
                }
                p.setXuatXu(origin);
            }

            // Xử lý dung tích
            if (dto.getTenDungTich() != null && !dto.getTenDungTich().isBlank()) {
                DungTich volume = dungTichRepository.findByDungTich(dto.getTenDungTich());
                if (volume == null) {
                    volume = new DungTich();
                    volume.setTenDungTich(dto.getTenDungTich());
                    volume.setTrangThai(true);
                    volume = dungTichRepository.save(volume);
                }
                p.setDungTich(volume);
            }

            SanPham savedProduct = sanPhamRepository.save(p);
            // generate product QR
            try {
                String productQrPath = generateAndSaveProductQr(savedProduct);
                System.out.println("Generated product QR: " + productQrPath);
            } catch (Exception ex) {
                System.err.println("Error generating product QR: " + ex.getMessage());
            }
            if (dto.getVariants() != null && !dto.getVariants().isEmpty()) {
                int variantIndex = 0;
                for (VariantCreateDTO variantDTO : dto.getVariants()) {
                    SanPhamChiTiet variant = new SanPhamChiTiet();
                    variant.setSanPham(savedProduct);
                    variant.setNgayTao(LocalDateTime.now());
                    variant.setMoTa(dto.getMoTa());

                    // set other relations same as existing method (reuse logic)
                    if (variantDTO.getTenMauSac() != null && !variantDTO.getTenMauSac().isBlank()) {
                        MauSac color = mauSacRepository.findByTenMauSac(variantDTO.getTenMauSac());
                        if (color == null) {
                            color = new MauSac();
                            color.setTenMauSac(variantDTO.getTenMauSac());
                            color.setTrangThai(true);
                            color = mauSacRepository.save(color);
                        }
                        variant.setMauSac(color);
                    }
                    if (variantDTO.getTenKichThuoc() != null && !variantDTO.getTenKichThuoc().isBlank()) {
                        KichThuoc size = kichThuocRepository.findByTenKichThuoc(variantDTO.getTenKichThuoc());
                        if (size == null) {
                            size = new KichThuoc();
                            size.setTenKichThuoc(variantDTO.getTenKichThuoc());
                            size.setTrangThai(true);
                            size = kichThuocRepository.save(size);
                        }
                        variant.setKichThuoc(size);
                    }
                    if (variantDTO.getTenTrongLuong() != null && !variantDTO.getTenTrongLuong().isBlank()) {
                        TrongLuong weight = trongLuongRepository.findByTenTrongLuong(variantDTO.getTenTrongLuong());
                        if (weight == null) {
                            weight = new TrongLuong();
                            weight.setTenTrongLuong(variantDTO.getTenTrongLuong());
                            weight.setTrangThai(true);
                            weight = trongLuongRepository.save(weight);
                        }
                        variant.setTrongLuong(weight);
                    }

                    // Image handling: if anhDuongDan references a filename present in fileMap, convert file to base64
                    if (variantDTO.getAnhDuongDan() != null && !variantDTO.getAnhDuongDan().isBlank()) {
                        String path = variantDTO.getAnhDuongDan();
                        org.springframework.web.multipart.MultipartFile mpf = fileMap.get(path);
                        // fallback: try case-insensitive lookup
                        if (mpf == null) {
                            for (String k : fileMap.keySet()) {
                                if (k.equalsIgnoreCase(path)) {
                                    mpf = fileMap.get(k);
                                    break;
                                }
                            }
                        }
                        // fallback: compare basenames (in case client sent full path or different separators)
                        if (mpf == null) {
                            String expectedBase = path;
                            int idx = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
                            if (idx >= 0) expectedBase = path.substring(idx + 1);
                            for (String k : fileMap.keySet()) {
                                String base = k;
                                int idx2 = Math.max(k.lastIndexOf('/'), k.lastIndexOf('\\'));
                                if (idx2 >= 0) base = k.substring(idx2 + 1);
                                if (base.equalsIgnoreCase(expectedBase)) {
                                    mpf = fileMap.get(k);
                                    System.out.println("Matched by basename fallback: variantPath='" + path + "' -> key='" + k + "'");
                                    break;
                                }
                            }
                        }

                        if (mpf != null && !mpf.isEmpty()) {
                            try {
                                // Upload to Cloudinary and persist secure_url
                                String secure = cloudinaryService.upload(mpf);
                                AnhSanPham anh = new AnhSanPham();
                                anh.setDuongDan(secure);
                                anh.setTrangThai(true);
                                AnhSanPham savedAnh = anhSanPhamRepository.save(anh);
                                System.out.println("Saved image from upload (Cloudinary): filename=" + path + " id=" + savedAnh.getId() + " url=" + secure);
                                variant.setAnhSanPham(savedAnh);
                            } catch (Exception e) {
                                System.err.println("Error uploading multipart file to Cloudinary: " + e.getMessage());
                            }
                        } else {
                            // If no filename match, try mapping by index when counts align
                            if ((uploadedFiles != null) && (uploadedFiles.length == dto.getVariants().size())) {
                                try {
                                    org.springframework.web.multipart.MultipartFile candidate = uploadedFiles[variantIndex];
                                    if (candidate != null && !candidate.isEmpty()) {
                                        String secure = cloudinaryService.upload(candidate);
                                        AnhSanPham anh = new AnhSanPham();
                                        anh.setDuongDan(secure);
                                        anh.setTrangThai(true);
                                        AnhSanPham savedAnh = anhSanPhamRepository.save(anh);
                                        System.out.println("Saved image by index fallback (Cloudinary): variantIndex=" + variantIndex + " id=" + savedAnh.getId() + " url=" + secure);
                                        variant.setAnhSanPham(savedAnh);
                                    }
                                } catch (Exception e) {
                                    System.err.println("Error uploading index-fallback image to Cloudinary: " + e.getMessage());
                                }
                            } else {
                                System.out.println("No uploaded file matched for variant anhDuongDan='" + path + "' - checking if it's an http(s) URL");
                                // Only persist if path looks like an http(s) URL. Avoid saving raw base64 data into DB.
                                if (path.startsWith("http://") || path.startsWith("https://")) {
                                    AnhSanPham anh = new AnhSanPham();
                                    anh.setDuongDan(path);
                                    anh.setTrangThai(true);
                                    AnhSanPham savedAnh = anhSanPhamRepository.save(anh);
                                    System.out.println("Saved image from URL: " + path + " -> id=" + savedAnh.getId());
                                    variant.setAnhSanPham(savedAnh);
                                } else {
                                    System.out.println("Skipping saving unknown image path (not http): " + path);
                                }
                            }
                        }

                        variantIndex++;
                    }

                    variant.setGia(variantDTO.getGia() != null ? variantDTO.getGia() : 0.0);
                    variant.setSoLuong(variantDTO.getSoLuong() != null ? variantDTO.getSoLuong() : 0);
                    variant.setTrangThai(variantDTO.getTrangThai() != null ? variantDTO.getTrangThai() : true);

                    SanPhamChiTiet savedVar = sanPhamChiTietRepository.save(variant);
                    System.out.println("Saved variant id=" + (savedVar != null ? savedVar.getId() : "null") +
                            " | linkedImageId=" + (savedVar != null && savedVar.getAnhSanPham() != null ? savedVar.getAnhSanPham().getId() : "null"));
                    // generate QR for variant
                    try {
                        String variantQrPath = generateAndSaveVariantQr(savedVar);
                        System.out.println("Generated variant QR: " + variantQrPath);
                    } catch (Exception ex) {
                        System.err.println("Error generating variant QR: " + ex.getMessage());
                    }
                }
            }

            return savedProduct;
        } catch (Exception e) {
            throw new RuntimeException("Error creating product with files: " + e.getMessage(), e);
        }
    }

    private String generateProductCode() {
        // Tạo mã sản phẩm tự động (SP + timestamp)
        return "SP" + System.currentTimeMillis();
    }

    // Các method khác giữ nguyên...
    public List<SanPham> findAll() {
        return sanPhamRepository.findAll();
    }

    public List<ProductSummaryDTO> getProductSummaries() {
        // Implementation giữ nguyên
        List<SanPham> products = sanPhamRepository.findAll();
        List<SanPhamChiTiet> details = sanPhamChiTietRepository.findAll();

        Map<Integer, Integer> qtyByProduct = details.stream()
                .filter(d -> d.getSanPham() != null && d.getSoLuong() != null)
                .collect(Collectors.groupingBy(d -> d.getSanPham().getId(), Collectors.summingInt(SanPhamChiTiet::getSoLuong)));

        List<ProductSummaryDTO> summaries = new ArrayList<>();
        for (SanPham p : products) {
            Integer total = qtyByProduct.getOrDefault(p.getId(), 0);
            String thuongHieu = p.getThuongHieu() != null ? p.getThuongHieu().getTenThuongHieu() : null;
            String chatLieu = p.getChatLieu() != null ? p.getChatLieu().getTenChatLieu() : null;
            Boolean status = total > 0;
            ProductSummaryDTO dto = new ProductSummaryDTO(p.getId(), p.getMaSanPham(), p.getTenSanPham(), thuongHieu, chatLieu, total, status);
            summaries.add(dto);
        }
        return summaries;
    }

    public List<ProductVariantDTO> getVariantsByProductId(Integer productId) {
        System.out.println("=== DEBUG: GETTING VARIANTS FOR PRODUCT " + productId + " ===");

        List<SanPhamChiTiet> details = sanPhamChiTietRepository.findBySanPhamId(productId);
        System.out.println("Found " + details.size() + " variants in database");

        List<ProductVariantDTO> out = new ArrayList<>();

        for (SanPhamChiTiet d : details) {
            System.out.println("--- Processing Variant ID: " + d.getId() + " ---");
            System.out.println("  Image relation: " + (d.getAnhSanPham() != null ? "FOUND (ID: " + d.getAnhSanPham().getId() + ")" : "NULL"));

            String code = "CTSP" + (d.getId() != null ? d.getId() : "N/A");
            String name = d.getSanPham() != null ? d.getSanPham().getTenSanPham() : "N/A";

            // LẤY VÀ PHÂN TÍCH MÀU SẮC
            String colorName = "N/A";
            String colorCode = "#CCCCCC";
            if (d.getMauSac() != null) {
                colorName = d.getMauSac().getTenMauSac();
                colorCode = parseColorCodeFromName(colorName);
            }

            String size = d.getKichThuoc() != null ? d.getKichThuoc().getTenKichThuoc() : "N/A";

            // LẤY TRỌNG LƯỢNG
            String weight = null;
            if (d.getTrongLuong() != null) {
                weight = d.getTrongLuong().getTenTrongLuong();
            }

            Integer qty = d.getSoLuong() != null ? d.getSoLuong() : 0;
            Double price = d.getGia() != null ? d.getGia() : 0.0;
            Boolean st = d.getTrangThai() != null ? d.getTrangThai() : (qty > 0);
            String description = d.getMoTa();

            String imageUrl = null;
            if (d.getAnhSanPham() != null && d.getAnhSanPham().getDuongDan() != null) {
                imageUrl = d.getAnhSanPham().getDuongDan();
                System.out.println("  Found image URL for variant " + d.getId() + ": " +
                        (imageUrl.length() > 50 ? imageUrl.substring(0, 47) + "..." : imageUrl));
            } else {
                System.out.println("  No image found for variant " + d.getId());
            }

            // Tạo DTO
            ProductVariantDTO dto = new ProductVariantDTO(
                    d.getId(),       // id
                    code,           // maVariant
                    name,           // tenSanPham
                    colorName,      // mauSac
                    size,           // kichThuoc
                    qty,            // soLuong
                    st,             // trangThai
                    description,    // moTa
                    imageUrl,       // anhUrl
                    price,          // gia
                    weight          // tenTrongLuong - có thể là null
            );

            dto.setMaMau(colorCode);

            out.add(dto);
        }

        System.out.println("=== RETURNING " + out.size() + " VARIANTS ===");
        return out;
    }

    // Return all product variants across all products, newest variants first
    public List<ProductVariantDTO> getAllVariants() {
        List<SanPhamChiTiet> details = sanPhamChiTietRepository.findAll(Sort.by(Sort.Direction.DESC, "ngayTao"));
        List<ProductVariantDTO> out = new ArrayList<>();
        for (SanPhamChiTiet d : details) {
            String code = "CTSP" + (d.getId() != null ? d.getId() : "N/A");
            String name = d.getSanPham() != null ? d.getSanPham().getTenSanPham() : "N/A";
            String colorName = d.getMauSac() != null ? d.getMauSac().getTenMauSac() : "N/A";
            String size = d.getKichThuoc() != null ? d.getKichThuoc().getTenKichThuoc() : "N/A";
            Integer qty = d.getSoLuong() != null ? d.getSoLuong() : 0;
            Double price = d.getGia() != null ? d.getGia() : 0.0;
            Boolean st = d.getTrangThai() != null ? d.getTrangThai() : (qty > 0);
            String description = d.getMoTa();
            String imageUrl = d.getAnhSanPham() != null ? d.getAnhSanPham().getDuongDan() : null;

            ProductVariantDTO dto = new ProductVariantDTO(d.getId(), d.getSanPham() != null ? d.getSanPham().getId() : null, code, name, colorName, size, qty, st, description, imageUrl, price, (d.getTrongLuong() != null ? d.getTrongLuong().getTenTrongLuong() : null));
            String colorCode = null;
            if (d.getMauSac() != null && d.getMauSac().getTenMauSac() != null) {
                colorCode = parseColorCodeFromName(d.getMauSac().getTenMauSac());
            }
            dto.setMaMau(colorCode);
            out.add(dto);
        }
        return out;
    }

    /**
     * Resolve QR payloads of form "product:{id}" or "variant:{id}" and return a map containing product or variant info.
     */
    public Map<String, Object> resolveQrPayload(String payload) {
        Map<String, Object> out = new HashMap<>();
        if (payload == null) {
            out.put("error", "Payload is empty");
            return out;
        }
        payload = payload.trim();
        try {
            if (payload.toLowerCase().startsWith("product:")) {
                String idStr = payload.substring(payload.indexOf(':') + 1).trim();
                Integer id = Integer.parseInt(idStr);
                Optional<SanPham> maybe = sanPhamRepository.findById(id);
                if (maybe.isPresent()) {
                    SanPham p = maybe.get();
                    Map<String, Object> pd = new HashMap<>();
                    pd.put("id", p.getId());
                    pd.put("maSanPham", p.getMaSanPham());
                    pd.put("tenSanPham", p.getTenSanPham());
                    // variant count
                    int variantCount = sanPhamChiTietRepository.findBySanPhamId(p.getId()).size();
                    pd.put("variantCount", variantCount);
                    // try to find a representative image from first variant
                    List<SanPhamChiTiet> details = sanPhamChiTietRepository.findBySanPhamId(p.getId());
                    String imageUrl = null;
                    if (!details.isEmpty() && details.get(0).getAnhSanPham() != null) imageUrl = details.get(0).getAnhSanPham().getDuongDan();
                    pd.put("imageUrl", imageUrl);
                    out.put("product", pd);
                    return out;
                } else {
                    out.put("error", "Sản phẩm không tồn tại");
                    return out;
                }
            } else if (payload.toLowerCase().startsWith("variant:")) {
                String idStr = payload.substring(payload.indexOf(':') + 1).trim();
                Integer id = Integer.parseInt(idStr);
                Optional<SanPhamChiTiet> maybe = sanPhamChiTietRepository.findById(id);
                if (maybe.isPresent()) {
                    SanPhamChiTiet v = maybe.get();
                    ProductVariantDTO dto = new ProductVariantDTO(
                            v.getId(),
                            v.getSanPham() != null ? v.getSanPham().getId() : null,
                            "CTSP" + (v.getId() != null ? v.getId() : "N/A"),
                            v.getSanPham() != null ? v.getSanPham().getTenSanPham() : null,
                            v.getMauSac() != null ? v.getMauSac().getTenMauSac() : null,
                            v.getKichThuoc() != null ? v.getKichThuoc().getTenKichThuoc() : null,
                            v.getSoLuong() != null ? v.getSoLuong() : 0,
                            v.getTrangThai() != null ? v.getTrangThai() : false,
                            v.getMoTa(),
                            v.getAnhSanPham() != null ? v.getAnhSanPham().getDuongDan() : null,
                            v.getGia() != null ? v.getGia() : 0.0,
                            v.getTrongLuong() != null ? v.getTrongLuong().getTenTrongLuong() : null
                    );
                    out.put("variant", dto);
                    return out;
                } else {
                    out.put("error", "Biến thể không tồn tại");
                    return out;
                }
            } else {
                out.put("error", "Unknown QR payload format");
                return out;
            }
        } catch (Exception e) {
            out.put("error", "Lỗi khi xử lý payload: " + e.getMessage());
            return out;
        }
    }

    // Hàm phân tích tên màu để lấy mã màu
    private String parseColorCodeFromName(String colorName) {
        if (colorName == null || colorName.trim().isEmpty()) {
            return "#CCCCCC";
        }

        String lowerColor = colorName.toLowerCase().trim();

        // Kiểm tra nếu tên màu đã là mã hex (bắt đầu bằng #)
        if (lowerColor.startsWith("#")) {
            // Validate hex code
            if (lowerColor.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
                return lowerColor;
            }
        }

        // Kiểm tra nếu tên màu là mã RGB/RGBA
        if (lowerColor.startsWith("rgb(") || lowerColor.startsWith("rgba(")) {
            return lowerColor;
        }

        // Map các tên màu tiếng Việt và tiếng Anh phổ biến
        Map<String, String> colorMap = new HashMap<>();

        // Màu cơ bản - Tiếng Việt
        colorMap.put("đỏ", "#FF0000");
        colorMap.put("đỏ đậm", "#8B0000");
        colorMap.put("đỏ nhạt", "#FF6B6B");
        colorMap.put("xanh dương", "#0000FF");
        colorMap.put("xanh da trời", "#1E90FF");
        colorMap.put("xanh navy", "#000080");
        colorMap.put("xanh lá", "#00FF00");
        colorMap.put("xanh lá cây", "#32CD32");
        colorMap.put("xanh lá đậm", "#006400");
        colorMap.put("xanh rêu", "#6B8E23");
        colorMap.put("vàng", "#FFFF00");
        colorMap.put("vàng cam", "#FFA500");
        colorMap.put("vàng nhạt", "#FFFACD");
        colorMap.put("cam", "#FFA500");
        colorMap.put("cam đậm", "#FF8C00");
        colorMap.put("tím", "#800080");
        colorMap.put("tím nhạt", "#DDA0DD");
        colorMap.put("tím đậm", "#4B0082");
        colorMap.put("hồng", "#FFC0CB");
        colorMap.put("hồng đậm", "#FF1493");
        colorMap.put("nâu", "#A52A2A");
        colorMap.put("nâu nhạt", "#D2B48C");
        colorMap.put("nâu đậm", "#8B4513");
        colorMap.put("đen", "#000000");
        colorMap.put("trắng", "#FFFFFF");
        colorMap.put("xám", "#808080");
        colorMap.put("xám nhạt", "#D3D3D3");
        colorMap.put("xám đậm", "#696969");
        colorMap.put("bạc", "#C0C0C0");
        colorMap.put("be", "#F5F5DC");
        colorMap.put("kem", "#FFFDD0");

        // Màu cơ bản - Tiếng Anh
        colorMap.put("red", "#FF0000");
        colorMap.put("dark red", "#8B0000");
        colorMap.put("light red", "#FF6B6B");
        colorMap.put("blue", "#0000FF");
        colorMap.put("light blue", "#ADD8E6");
        colorMap.put("dark blue", "#00008B");
        colorMap.put("navy blue", "#000080");
        colorMap.put("green", "#00FF00");
        colorMap.put("light green", "#90EE90");
        colorMap.put("dark green", "#006400");
        colorMap.put("yellow", "#FFFF00");
        colorMap.put("light yellow", "#FFFACD");
        colorMap.put("orange", "#FFA500");
        colorMap.put("purple", "#800080");
        colorMap.put("light purple", "#DDA0DD");
        colorMap.put("pink", "#FFC0CB");
        colorMap.put("hot pink", "#FF69B4");
        colorMap.put("brown", "#A52A2A");
        colorMap.put("light brown", "#D2B48C");
        colorMap.put("black", "#000000");
        colorMap.put("white", "#FFFFFF");
        colorMap.put("gray", "#808080");
        colorMap.put("light gray", "#D3D3D3");
        colorMap.put("dark gray", "#696969");
        colorMap.put("silver", "#C0C0C0");
        colorMap.put("beige", "#F5F5DC");
        colorMap.put("cream", "#FFFDD0");

        // Kiểm tra trong map
        if (colorMap.containsKey(lowerColor)) {
            return colorMap.get(lowerColor);
        }

        // Kiểm tra từ khóa trong tên màu
        for (Map.Entry<String, String> entry : colorMap.entrySet()) {
            if (lowerColor.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Nếu không tìm thấy, tạo mã màu ngẫu nhiên từ hash của tên màu
        return generateColorFromString(lowerColor);
    }

    // Tạo màu từ string (fallback)
    private String generateColorFromString(String str) {
        int hash = str.hashCode();
        // Tạo RGB từ hash
        int r = (hash & 0xFF0000) >> 16;
        int g = (hash & 0x00FF00) >> 8;
        int b = hash & 0x0000FF;

        // Đảm bảo màu không quá tối (tối thiểu 100)
        r = Math.max(r & 0xFF, 100);
        g = Math.max(g & 0xFF, 100);
        b = Math.max(b & 0xFF, 100);

        // Đảm bảo màu không quá sáng (tối đa 240)
        r = Math.min(r, 240);
        g = Math.min(g, 240);
        b = Math.min(b, 240);

        return String.format("#%02X%02X%02X", r, g, b);
    }
    private String convertImageToBase64(String filePath) {
        try {
            // Xử lý đường dẫn file
            Path path;
            if (filePath.startsWith("file://")) {
                path = Paths.get(new URI(filePath));
            } else {
                path = Paths.get(filePath);
            }

            byte[] imageBytes = Files.readAllBytes(path);
            String base64 = Base64.getEncoder().encodeToString(imageBytes);

            // Xác định MIME type
            String mimeType = Files.probeContentType(path);
            if (mimeType == null) {
                mimeType = "image/jpeg"; // fallback
            }

            return "data:" + mimeType + ";base64," + base64;
        } catch (Exception e) {
            System.err.println("Error reading image file: " + e.getMessage());
            return null;
        }
    }

    // QR generation helpers
    private String generateAndSaveProductQr(SanPham product) throws Exception {
        if (product == null || product.getId() == null) throw new IllegalArgumentException("Product must be persisted before generating QR");
        String text = "product:" + product.getId();
        String dir = "uploads/qrcodes/products";
        String filename = "product_" + product.getId() + ".png";
        return writeQrToFile(text, dir, filename, 300, 300);
    }

    private String generateAndSaveVariantQr(SanPhamChiTiet variant) throws Exception {
        if (variant == null || variant.getId() == null) throw new IllegalArgumentException("Variant must be persisted before generating QR");
        String text = "variant:" + variant.getId();
        String dir = "uploads/qrcodes/variants";
        String filename = "variant_" + variant.getId() + ".png";
        return writeQrToFile(text, dir, filename, 300, 300);
    }

    private String writeQrToFile(String text, String dirPath, String filename, int width, int height) throws Exception {
        try {
            Path dir = Paths.get(dirPath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            QRCodeWriter qrWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            Path outPath = dir.resolve(filename);
            try (java.io.OutputStream os = Files.newOutputStream(outPath)) {
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", os);
            }
            return outPath.toString().replace("\\", "/");
        } catch (WriterException we) {
            throw new RuntimeException("Failed to write QR code: " + we.getMessage(), we);
        }
    }

    @Transactional
    public Integer saveProductVariant(ProductVariantDTO variantDTO, MultipartFile image) {
        try {
            System.out.println("=== START SAVING PRODUCT VARIANT ===");
            System.out.println("Variant data:");
            System.out.println("- ID: " + variantDTO.getId());
            System.out.println("- Product ID: " + variantDTO.getProductId());
            System.out.println("- Image provided: " + (image != null && !image.isEmpty()));

            // First ensure we have a valid product ID. If missing, try to infer by product name as a fallback.
            SanPham parentProduct = null;
            if (variantDTO.getProductId() != null) {
                parentProduct = sanPhamRepository.findById(variantDTO.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found with ID: " + variantDTO.getProductId()));
            } else {
                // Fallback: try to match by product name (exact match). This helps when frontend forgets to include productId.
                if (variantDTO.getTenSanPham() != null && !variantDTO.getTenSanPham().isBlank()) {
                    List<SanPham> matches = sanPhamRepository.findAll().stream()
                            .filter(sp -> variantDTO.getTenSanPham().equalsIgnoreCase(sp.getTenSanPham()))
                            .collect(Collectors.toList());
                    if (matches.size() == 1) {
                        parentProduct = matches.get(0);
                        variantDTO.setProductId(parentProduct.getId());
                        System.out.println("Fallback: matched product by name -> id=" + parentProduct.getId());
                    } else if (matches.size() > 1) {
                        System.out.println("Warning: multiple products found with name '" + variantDTO.getTenSanPham() + "'. Please provide productId explicitly.");
                    }
                }
                if (parentProduct == null) {
                    throw new RuntimeException("Product ID is required to save variant (and fallback by name failed)");
                }
            }


            // Create or get existing variant
            SanPhamChiTiet entity = new SanPhamChiTiet();
            boolean isNewVariant = true;
            if (variantDTO.getId() != null) {
                Optional<SanPhamChiTiet> maybe = sanPhamChiTietRepository.findById(variantDTO.getId());
                if (maybe.isPresent()) {
                    entity = maybe.get();
                    isNewVariant = false;
                    System.out.println("Found existing variant with ID: " + entity.getId());
                } else {
                    System.out.println("No existing variant found with provided id, creating new one");
                }
            }

            // Set parent product reference
            entity.setSanPham(parentProduct);
            if (isNewVariant && entity.getNgayTao() == null) {
                entity.setNgayTao(LocalDateTime.now());
            }

            // Map basic properties with validation
            entity.setGia(variantDTO.getGia() != null ? variantDTO.getGia() : 0.0);
            entity.setSoLuong(variantDTO.getSoLuong() != null ? variantDTO.getSoLuong() : 0);
            entity.setTrangThai(variantDTO.getTrangThai() != null ? variantDTO.getTrangThai() : true);
            entity.setMoTa(variantDTO.getMoTa());

            // Map relationships with validation
            if (variantDTO.getMauSac() != null && !variantDTO.getMauSac().isBlank()) {
                MauSac color = mauSacRepository.findByTenMauSac(variantDTO.getMauSac());
                if (color == null) {
                    color = new MauSac();
                    color.setTenMauSac(variantDTO.getMauSac());
                    color.setTrangThai(true);
                    color = mauSacRepository.save(color);
                    System.out.println("✓ Created new color: " + color.getTenMauSac());
                }
                entity.setMauSac(color);
            }

            if (variantDTO.getKichThuoc() != null && !variantDTO.getKichThuoc().isBlank()) {
                KichThuoc size = kichThuocRepository.findByTenKichThuoc(variantDTO.getKichThuoc());
                if (size == null) {
                    size = new KichThuoc();
                    size.setTenKichThuoc(variantDTO.getKichThuoc());
                    size.setTrangThai(true);
                    size = kichThuocRepository.save(size);
                    System.out.println("✓ Created new size: " + size.getTenKichThuoc());
                }
                entity.setKichThuoc(size);
            }

            if (variantDTO.getTenTrongLuong() != null && !variantDTO.getTenTrongLuong().isBlank()) {
                TrongLuong weight = trongLuongRepository.findByTenTrongLuong(variantDTO.getTenTrongLuong());
                if (weight == null) {
                    weight = new TrongLuong();
                    weight.setTenTrongLuong(variantDTO.getTenTrongLuong());
                    weight.setTrangThai(true);
                    weight = trongLuongRepository.save(weight);
                    System.out.println("✓ Created new weight: " + weight.getTenTrongLuong());
                }
                entity.setTrongLuong(weight);
            }

            // Image processing with improved logging and error handling
            if (image != null && !image.isEmpty()) {
                System.out.println("Processing new image...");
                System.out.println("- Original filename: " + image.getOriginalFilename());
                System.out.println("- Content type: " + image.getContentType());
                System.out.println("- Size: " + image.getSize() + " bytes");

                try {
                    // Create or update AnhSanPham by uploading image to Cloudinary
                    AnhSanPham anhSanPham = new AnhSanPham();
                    try {
                        String secure = cloudinaryService.upload(image);
                        anhSanPham.setDuongDan(secure);
                        anhSanPham.setTrangThai(true);
                    } catch (Exception ex) {
                        System.err.println("Warning: Cloudinary upload failed, falling back to base64: " + ex.getMessage());
                        byte[] bytes = image.getBytes();
                        String base64Image = java.util.Base64.getEncoder().encodeToString(bytes);
                        String contentType = image.getContentType() != null ? image.getContentType() : "image/jpeg";
                        String imageUrl = "data:" + contentType + ";base64," + base64Image;
                        anhSanPham.setDuongDan(imageUrl);
                        anhSanPham.setTrangThai(true);
                    }

                    // Save image first (persist separately so we have the id)
                    AnhSanPham savedImage = anhSanPhamRepository.save(anhSanPham);
                    System.out.println("✓ Saved image with ID: " + savedImage.getId());

                    // If updating existing variant with existing image, clean up old image
                    if (entity.getAnhSanPham() != null && entity.getAnhSanPham().getId() != null) {
                        try {
                            // Only delete if it's a different image
                            if (!entity.getAnhSanPham().getId().equals(savedImage.getId())) {
                                anhSanPhamRepository.deleteById(entity.getAnhSanPham().getId());
                                System.out.println("✓ Deleted old image with ID: " + entity.getAnhSanPham().getId());
                            }
                        } catch (Exception e) {
                            System.err.println("Warning: Could not delete old image: " + e.getMessage());
                        }
                    }

                    // Link new image to variant
                    entity.setAnhSanPham(savedImage);
                    System.out.println("✓ Linked new image to variant (anh id=" + savedImage.getId() + ")");

                } catch (Exception e) {
                    System.err.println("❌ Error processing image:");
                    System.err.println("- Message: " + e.getMessage());
                    e.printStackTrace();
                    // Don't throw - continue with save even if image processing fails
                }
            } else if (variantDTO.getAnhUrl() != null && !variantDTO.getAnhUrl().isBlank()) {
                System.out.println("Using provided anhUrl without new image upload");
                // If anhUrl is provided but no new image uploaded, use the URL
                AnhSanPham anhSanPham = new AnhSanPham();
                anhSanPham.setDuongDan(variantDTO.getAnhUrl());
                anhSanPham.setTrangThai(true);
                AnhSanPham savedImage = anhSanPhamRepository.save(anhSanPham);
                entity.setAnhSanPham(savedImage);
                System.out.println("✓ Saved and linked image from URL with ID: " + savedImage.getId());
            } else {
                System.out.println("No new image or URL provided");
                // For updates, keep existing image if no new one provided
                if (entity.getId() != null && entity.getAnhSanPham() != null) {
                    System.out.println("→ Keeping existing image with ID: " + entity.getAnhSanPham().getId());
                }
            }

            // Save variant
            entity = sanPhamChiTietRepository.save(entity);
            System.out.println("=== VARIANT SAVED SUCCESSFULLY ===");
            System.out.println("- Variant ID: " + entity.getId());
            System.out.println("- Image status: " + (entity.getAnhSanPham() != null ? "SET (ID: " + entity.getAnhSanPham().getId() + ")" : "NULL"));

            // Return saved variant id
            return entity.getId();

        } catch (Exception e) {
            System.err.println("❌ CRITICAL ERROR SAVING VARIANT:");
            System.err.println("- Message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save product variant: " + e.getMessage(), e);
        }
    }

    public PaginationResponseDTO<ProductSummaryDTO> getProductsWithPagination(int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by("ngayTao").descending());
            Page<SanPham> productPage = sanPhamRepository.findAll(pageable);

            // Chuyển đổi sang DTO
            List<ProductSummaryDTO> productDTOs = convertToProductSummaryDTOs(productPage.getContent());

            return new PaginationResponseDTO<>(
                    productDTOs,
                    page,
                    size,
                    productPage.getTotalElements()
            );
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách sản phẩm phân trang: " + e.getMessage());
        }
    }

    // Phân trang với tìm kiếm
    public List<String> getDistinctProductNames() {
        try {
            return sanPhamRepository.findDistinctProductNames();
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách tên sản phẩm: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Tìm kiếm tên sản phẩm theo keyword
    public List<String> searchProductNames(String keyword) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return getDistinctProductNames();
            }
            return sanPhamRepository.findDistinctProductNamesByKeyword(keyword.trim());
        } catch (Exception e) {
            System.err.println("Lỗi khi tìm kiếm tên sản phẩm: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Chuyển đổi từ SanPham sang ProductSummaryDTO
    private List<ProductSummaryDTO> convertToProductSummaryDTOs(List<SanPham> products) {
        List<SanPhamChiTiet> details = sanPhamChiTietRepository.findAll();

        Map<Integer, Integer> qtyByProduct = details.stream()
                .filter(d -> d.getSanPham() != null && d.getSoLuong() != null)
                .collect(Collectors.groupingBy(d -> d.getSanPham().getId(), Collectors.summingInt(SanPhamChiTiet::getSoLuong)));

        List<ProductSummaryDTO> summaries = new ArrayList<>();
        for (SanPham p : products) {
            Integer total = qtyByProduct.getOrDefault(p.getId(), 0);
            String thuongHieu = p.getThuongHieu() != null ? p.getThuongHieu().getTenThuongHieu() : null;
            String chatLieu = p.getChatLieu() != null ? p.getChatLieu().getTenChatLieu() : null;
            Boolean status = total > 0;

            ProductSummaryDTO dto = new ProductSummaryDTO(
                    p.getId(), p.getMaSanPham(), p.getTenSanPham(),
                    thuongHieu, chatLieu, total, status
            );
            summaries.add(dto);
        }
        return summaries;
    }
}
