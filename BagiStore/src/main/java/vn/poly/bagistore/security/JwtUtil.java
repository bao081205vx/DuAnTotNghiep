package vn.poly.bagistore.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;

public class JwtUtil {
    private static Key getKey(String secret){
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(java.util.Arrays.copyOf(keyBytes, Math.max(keyBytes.length, 32)));
    }

    public static Claims parseClaims(String jwt, String secret){
        if(jwt == null || jwt.trim().isEmpty()) return null;
        try{
            Key key = getKey(secret);
            Jws<Claims> j = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwt);
            return j.getBody();
        }catch(Exception ex){
            return null;
        }
    }
}
