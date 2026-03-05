package vn.poly.bagistore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import vn.poly.bagistore.Service.SanPhamService;
import vn.poly.bagistore.dto.ProductVariantDTO;

import java.util.List;

@RestController
@RequestMapping("/api/variants")
public class VariantController {

    @Autowired
    private SanPhamService sanPhamService;

    @GetMapping
    public List<ProductVariantDTO> getAllVariants() {
        return sanPhamService.getAllVariants();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductVariantDTO> getVariantById(@PathVariable Integer id) {
        try {
            List<ProductVariantDTO> all = sanPhamService.getAllVariants();
            if (all == null) return ResponseEntity.notFound().build();
            for (ProductVariantDTO v : all) {
                if (v != null && v.getId() != null && v.getId().equals(id)) {
                    return ResponseEntity.ok(v);
                }
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
