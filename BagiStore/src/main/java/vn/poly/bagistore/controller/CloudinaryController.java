package vn.poly.bagistore.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/images")
public class CloudinaryController {

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    private final RestTemplate rest = new RestTemplate();

    @PostMapping(path = "/upload")
    public ResponseEntity<?> uploadToCloudinary(@RequestParam("file") MultipartFile file) {
        try {
            if (cloudName == null || cloudName.isBlank() || apiKey == null || apiKey.isBlank() || apiSecret == null || apiSecret.isBlank()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Cloudinary not configured on server"));
            }

            String uploadUrl = String.format("https://api.cloudinary.com/v1_1/%s/image/upload", cloudName);

            long timestamp = Instant.now().getEpochSecond();
            String toSign = "timestamp=" + timestamp;
            String signature = sha1Hex(toSign + apiSecret);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });
            body.add("api_key", apiKey);
            body.add("timestamp", String.valueOf(timestamp));
            body.add("signature", signature);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            @SuppressWarnings({"rawtypes"})
            ResponseEntity<Map> respEntity = rest.postForEntity(new URI(uploadUrl), requestEntity, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = respEntity.getBody();
            if (resp == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Empty response from Cloudinary"));
            }
            Object secureObj = resp.get("secure_url");
            if (secureObj == null) secureObj = resp.get("url");
            if (secureObj == null) {
                // return the full response as fallback but indicate missing secure_url
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Cloudinary response missing secure_url", "raw", resp));
            }
            String secureUrl = String.valueOf(secureObj);
            System.out.println("Cloudinary upload successful, secure_url=" + secureUrl);
            return ResponseEntity.ok(Map.of("secure_url", secureUrl));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    private static String sha1Hex(String input) throws Exception {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
}
