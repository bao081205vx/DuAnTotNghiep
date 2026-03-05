package vn.poly.bagistore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import vn.poly.bagistore.model.*;
import vn.poly.bagistore.repository.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lookups")
public class LookupController {

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;
    @Autowired
    private ChatLieuRepository chatLieuRepository;
    @Autowired
    private NhaSanXuatRepository nhaSanXuatRepository;
    @Autowired
    private XuatXuRepository xuatXuRepository;
    @Autowired
    private DungTichRepository dungTichRepository;
    @Autowired
    private MauSacRepository mauSacRepository;
    @Autowired
    private KichThuocRepository kichThuocRepository;
    @Autowired
    private TrongLuongRepository trongLuongRepository;
    @Autowired
    private SanPhamRepository sanPhamRepository; // Thêm repository này
    @Autowired
    private vn.poly.bagistore.repository.HoaDonRepository hoaDonRepository;

    // Thêm endpoint cho product names
    @GetMapping("/product-names")
    public List<String> getProductNames() {
        List<SanPham> products = sanPhamRepository.findAll();
        return products.stream()
                .map(SanPham::getTenSanPham)
                .filter(name -> name != null && !name.trim().isEmpty())
                .collect(Collectors.toList());
    }
    @PostMapping("/product-names")
    public String createProductName(@RequestBody Map<String, String> payload) {
        // Chỉ trả về tên sản phẩm, không lưu vào database
        return payload.get("tenSanPham") != null ? payload.get("tenSanPham") : payload.get("name");
    }

    // Brands
    @GetMapping("/brands")
    public List<ThuongHieu> getBrands() {
        return thuongHieuRepository.findAll();
    }

    @PostMapping("/brands")
    public ThuongHieu createBrand(@RequestBody ThuongHieu payload) {
        return thuongHieuRepository.save(payload);
    }

    // Materials
    @GetMapping("/materials")
    public List<ChatLieu> getMaterials() {
        return chatLieuRepository.findAll();
    }

    @PostMapping("/materials")
    public ChatLieu createMaterial(@RequestBody ChatLieu payload) { return chatLieuRepository.save(payload); }

    // Manufacturers
    @GetMapping("/manufacturers")
    public List<NhaSanXuat> getManufacturers() {
        return nhaSanXuatRepository.findAll();
    }

    @PostMapping("/manufacturers")
    public NhaSanXuat createManufacturer(@RequestBody NhaSanXuat payload) { return nhaSanXuatRepository.save(payload); }

    // Origins
    @GetMapping("/origins")
    public List<XuatXu> getOrigins() {
        return xuatXuRepository.findAll();
    }

    @PostMapping("/origins")
    public XuatXu createOrigin(@RequestBody XuatXu payload) { return xuatXuRepository.save(payload); }

    // Volumes
    @GetMapping("/volumes")
    public List<DungTich> getVolumes() {
        return dungTichRepository.findAll();
    }

    @PostMapping("/volumes")
    public DungTich createVolume(@RequestBody DungTich payload) { return dungTichRepository.save(payload); }

    // Colors
    @GetMapping("/colors")
    public List<MauSac> getColors() {
        return mauSacRepository.findAll();
    }

    @PostMapping("/colors")
    public MauSac createColor(@RequestBody MauSac payload) { return mauSacRepository.save(payload); }

    // Sizes
    @GetMapping("/sizes")
    public List<KichThuoc> getSizes() {
        return kichThuocRepository.findAll();
    }

    @PostMapping("/sizes")
    public KichThuoc createSize(@RequestBody KichThuoc payload) { return kichThuocRepository.save(payload); }

    // Weights
    @GetMapping("/weights")
    public List<TrongLuong> getWeights() {
        return trongLuongRepository.findAll();
    }

    @PostMapping("/weights")
    public TrongLuong createWeight(@RequestBody TrongLuong payload) { return trongLuongRepository.save(payload); }

    // Invoice types (loaiHoaDon) - distinct non-null values
    @GetMapping("/invoiceTypes")
    public List<String> getInvoiceTypes() {
        return hoaDonRepository.findDistinctLoaiHoaDon();
    }
}
