package vn.poly.bagistore.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import vn.poly.bagistore.Service.SanPhamService;
import vn.poly.bagistore.dto.PaginationResponseDTO;
import vn.poly.bagistore.model.SanPham;
import vn.poly.bagistore.dto.ProductSummaryDTO;
import vn.poly.bagistore.dto.ProductVariantDTO;
import vn.poly.bagistore.dto.ProductCreateDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


@RestController
@RequestMapping("/api/products")
public class SanPhamController {

    @Autowired
    private SanPhamService sanPhamService;



    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping
    public List<ProductSummaryDTO> getAllSanPhams() {
        return sanPhamService.getProductSummaries();
    }

    @GetMapping("/{id}/variants")
    public List<ProductVariantDTO> getVariants(@PathVariable Integer id) {
        return sanPhamService.getVariantsByProductId(id);
    }

    // THÊM METHOD POST ĐỂ TẠO SẢN PHẨM MỚI
    @PostMapping
    public SanPham createProduct(@RequestBody ProductCreateDTO dto) {
        System.out.println("=== RECEIVED PRODUCT CREATE REQUEST ===");
        System.out.println("Product name: " + dto.getTenSanPham());
        System.out.println("Variants count: " + (dto.getVariants() != null ? dto.getVariants().size() : 0));
        try {
            SanPham result = sanPhamService.createProductWithVariants(dto);
            System.out.println("Product created successfully with ID: " + result.getId());
            // Build response with QR path (service already generates QR files under uploads/)
            String productQr = "uploads/qrcodes/products/product_" + result.getId() + ".png";
            Map<String, Object> resp = new HashMap<>();
            resp.put("product", result);
            resp.put("productQr", productQr);
            return result;
        } catch (Exception e) {
            System.err.println("Error creating product: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }


    /**
     * Endpoint to create product with variant images uploaded from local machine.
     * Expects multipart/form-data with a part named 'product' containing JSON ProductCreateDTO
     * and multiple files under 'variantImages'. Filenames should match the variant's expected image path
     * (e.g. 'variant1.jpg' or 'CTSP123.jpg') or the client can send a mapping in the DTO anhDuongDan field.
     */
    @PostMapping(path = "/upload-with-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProductWithImages(@RequestPart("product") String productJson,
                                                     @RequestPart(name = "variantImages", required = false) MultipartFile[] variantImages) {
        try {
            ProductCreateDTO dto = objectMapper.readValue(productJson, ProductCreateDTO.class);

            // Build a map filename -> MultipartFile for easy lookup
            Map<String, MultipartFile> fileMap = new HashMap<>();
            if (variantImages != null) {
                for (MultipartFile f : variantImages) {
                    fileMap.put(f.getOriginalFilename(), f);
                }
            }
            System.out.println("=== UPLOAD-WITH-IMAGES: Received product payload ===");
            System.out.println("tenSanPham=" + dto.getTenSanPham());
            System.out.println("tenThuongHieu=" + dto.getTenThuongHieu());
            System.out.println("tenChatLieu=" + dto.getTenChatLieu());
            System.out.println("tenNhaSanXuat=" + dto.getTenNhaSanXuat());
            System.out.println("tenXuatXu=" + dto.getTenXuatXu());
            System.out.println("tenDungTich=" + dto.getTenDungTich());
            System.out.println("Variants count=" + (dto.getVariants() == null ? 0 : dto.getVariants().size()));
            System.out.println("Uploaded files: " + fileMap.keySet());

            if (dto.getVariants() != null) {
                System.out.println("Listing incoming variants (index -> anhDuongDan):");
                int vi = 0;
                for (var v : dto.getVariants()) {
                    System.out.println("  [" + (vi++) + "] anhDuongDan=" + v.getAnhDuongDan() + " | tenMauSac=" + v.getTenMauSac() + " | tenKichThuoc=" + v.getTenKichThuoc());
                }
            }

            var result = sanPhamService.createProductWithVariantsAndFiles(dto, fileMap, variantImages);
            // include product QR and variant QRs are generated on the service side and stored under uploads/
            Map<String, Object> out = new HashMap<>();
            out.put("product", result);
            out.put("productQr", "uploads/qrcodes/products/product_" + result.getId() + ".png");
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Debug endpoint: upload a single image and save as AnhSanPham, return id and duongDan
    @PostMapping(path = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImageOnly(@RequestPart("image") MultipartFile image) {
        try {
            System.out.println("=== DEBUG: uploadImageOnly ===");
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No image file provided"));
            }
            // Avoid saving large base64 strings into the database from this debug endpoint.
            // Instead return an instructional response that the server-side Cloudinary upload
            // endpoint should be used: POST /api/images/upload. If you want to persist a URL,
            // send the URL to the variant create/update endpoints (they will persist it).
            return ResponseEntity.status(400).body(Map.of(
                    "error", "Avoid saving base64 via this endpoint. Use /api/images/upload to let server upload to Cloudinary, then send the returned secure_url to the variant endpoint.",
                    "hint", "If you intended to persist a URL, call PUT /api/variants/{id} with anhDuongDan set to a public image URL."
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/paginated")
    public ResponseEntity<PaginationResponseDTO<ProductSummaryDTO>> getProductsPaginated(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        try {
            PaginationResponseDTO<ProductSummaryDTO> response = sanPhamService.getProductsWithPagination(page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/names")
    public ResponseEntity<List<String>> getDistinctProductNames() {
        try {
            List<String> productNames = sanPhamService.getDistinctProductNames();
            return ResponseEntity.ok(productNames);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // API tìm kiếm tên sản phẩm
    @GetMapping("/names/search")
    public ResponseEntity<List<String>> searchProductNames(
            @RequestParam(required = false) String keyword) {
        try {
            List<String> productNames = sanPhamService.searchProductNames(keyword);
            return ResponseEntity.ok(productNames);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Resolve a scanned QR payload to product and its variants.
     * Accepts either a POST with JSON { "payload": "product:123" }
     * or a GET /resolve-qr?payload=product:123
     */
    @RequestMapping(path = "/resolve-qr", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> resolveQr(@RequestParam(required = false) String payload, @RequestBody(required = false) Map<String, Object> body) {
        try {
            // First, allow structured bodies that directly include variant/product identifiers or codes
            if (body != null && !body.isEmpty()) {
                // If body contains explicit numeric id fields
                Object vid = body.get("variantId");
                if (vid == null) vid = body.get("id");
                if (vid != null) {
                    Integer idVal = null;
                    if (vid instanceof Number) idVal = ((Number) vid).intValue();
                    else {
                        try { idVal = Integer.parseInt(vid.toString()); } catch (Exception ignored) { idVal = null; }
                    }
                    if (idVal != null) {
                        // find variant by numeric id
                        List<ProductVariantDTO> all = sanPhamService.getAllVariants();
                        ProductVariantDTO found = null;
                        for (ProductVariantDTO v : all) if (v.getId().equals(idVal)) { found = v; break; }
                        if (found == null) return ResponseEntity.status(404).body(Map.of("error", "Variant not found"));
                        Map<String, Object> out = new HashMap<>();
                        out.put("variant", found);
                        out.put("variantQr", "uploads/qrcodes/variants/variant_" + idVal + ".png");
                        return ResponseEntity.ok(out);
                    }
                }

                // If body contains a code like maVariant / code / variantCode, try to resolve by code
                String[] codeKeys = new String[]{"maVariant", "code", "variantCode", "ma"};
                for (String k : codeKeys) {
                    Object cv = body.get(k);
                    if (cv != null) {
                        String code = cv.toString().trim();
                        if (!code.isEmpty()) {
                            List<ProductVariantDTO> all = sanPhamService.getAllVariants();
                            ProductVariantDTO found = null;
                            for (ProductVariantDTO v : all) {
                                if (v.getMaVariant() != null && code.equalsIgnoreCase(v.getMaVariant())) {
                                    found = v; break;
                                }
                                // also try matching numeric id encoded as string
                                try { if (Integer.parseInt(code) == v.getId()) { found = v; break; } } catch (Exception ignored) {}
                            }
                            if (found == null) return ResponseEntity.status(404).body(Map.of("error", "Variant not found by code"));
                            Map<String, Object> out = new HashMap<>();
                            out.put("variant", found);
                            out.put("variantQr", "uploads/qrcodes/variants/variant_" + found.getId() + ".png");
                            return ResponseEntity.ok(out);
                        }
                    }
                }

                // If body contains a raw 'payload' field, fall back to the string payload handling below
                Object p = body.get("payload");
                if (p != null) payload = p.toString();
            }

            if ((payload == null || payload.isBlank())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Missing payload"));
            }

            // If payload looks like a URL, extract last path segment and try to resolve
            String trimmed = payload.trim();
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                try {
                    java.net.URL u = new java.net.URL(trimmed);
                    String[] segs = u.getPath().split("/");
                    String last = segs.length > 0 ? segs[segs.length - 1] : "";
                    if (last != null && !last.isBlank()) {
                        // try numeric id first
                        try { Integer id = Integer.parseInt(last);
                            // try variant id
                            List<ProductVariantDTO> all = sanPhamService.getAllVariants();
                            for (ProductVariantDTO v : all) if (v.getId().equals(id)) {
                                Map<String, Object> out = new HashMap<>(); out.put("variant", v); out.put("variantQr", "uploads/qrcodes/variants/variant_" + id + ".png"); return ResponseEntity.ok(out);
                            }
                        } catch (Exception ignored) {}
                        // treat last segment as code
                        String code = last;
                        List<ProductVariantDTO> all = sanPhamService.getAllVariants();
                        for (ProductVariantDTO v : all) if (v.getMaVariant() != null && code.equalsIgnoreCase(v.getMaVariant())) {
                            Map<String, Object> out = new HashMap<>(); out.put("variant", v); out.put("variantQr", "uploads/qrcodes/variants/variant_" + v.getId() + ".png"); return ResponseEntity.ok(out);
                        }
                    }
                } catch (Exception ignored) {}
            }

            // If payload is a plain code or numeric id (no colon), try to resolve directly
            if (!trimmed.contains(":")) {
                // try numeric id first
                try {
                    Integer idCandidate = Integer.parseInt(trimmed);
                    List<ProductVariantDTO> all = sanPhamService.getAllVariants();
                    for (ProductVariantDTO v : all) if (v.getId().equals(idCandidate)) {
                        Map<String, Object> out = new HashMap<>(); out.put("variant", v); out.put("variantQr", "uploads/qrcodes/variants/variant_" + idCandidate + ".png"); return ResponseEntity.ok(out);
                    }
                } catch (Exception ignored) {}

                // try as variant code
                List<ProductVariantDTO> all = sanPhamService.getAllVariants();
                for (ProductVariantDTO v : all) {
                    if (v.getMaVariant() != null && trimmed.equalsIgnoreCase(v.getMaVariant())) {
                        Map<String, Object> out = new HashMap<>(); out.put("variant", v); out.put("variantQr", "uploads/qrcodes/variants/variant_" + v.getId() + ".png"); return ResponseEntity.ok(out);
                    }
                }

                // not resolved — fall through to try typed payload parsing (below) or return not found
            }

            // Expect payload format product:{id} or variant:{id}
            String[] parts = trimmed.split(":");
            if (parts.length != 2) return ResponseEntity.badRequest().body(Map.of("error", "Invalid payload format"));
            String type = parts[0];
            Integer id = null;
            try { id = Integer.parseInt(parts[1]); } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", "Invalid id in payload")); }

            if ("product".equalsIgnoreCase(type)) {
                // fetch product summary + variants
                List<ProductSummaryDTO> summaries = sanPhamService.getProductSummaries();
                ProductSummaryDTO found = null;
                for (ProductSummaryDTO s : summaries) if (s.getId().equals(id)) found = s;
                if (found == null) return ResponseEntity.status(404).body(Map.of("error", "Product not found"));
                List<ProductVariantDTO> variants = sanPhamService.getVariantsByProductId(id);
                Map<String, Object> out = new HashMap<>();
                out.put("product", found);
                out.put("variants", variants);
                out.put("productQr", "uploads/qrcodes/products/product_" + id + ".png");
                return ResponseEntity.ok(out);
            } else if ("variant".equalsIgnoreCase(type)) {
                List<ProductVariantDTO> all = sanPhamService.getAllVariants();
                ProductVariantDTO found = null;
                for (ProductVariantDTO v : all) if (v.getId().equals(id)) found = v;
                if (found == null) return ResponseEntity.status(404).body(Map.of("error", "Variant not found"));
                Map<String, Object> out = new HashMap<>();
                out.put("variant", found);
                out.put("variantQr", "uploads/qrcodes/variants/variant_" + id + ".png");
                return ResponseEntity.ok(out);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Unknown type in payload"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}