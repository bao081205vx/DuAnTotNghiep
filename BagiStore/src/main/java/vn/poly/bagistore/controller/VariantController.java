package vn.poly.bagistore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}
