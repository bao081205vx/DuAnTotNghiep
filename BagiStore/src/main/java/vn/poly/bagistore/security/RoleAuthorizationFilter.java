package vn.poly.bagistore.security;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import io.jsonwebtoken.Claims;

@Component
public class RoleAuthorizationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, jakarta.servlet.ServletException {
        try{
            String path = request.getRequestURI() == null ? "" : request.getRequestURI();
            // paths to protect with role checks
            boolean isEmployees = path.startsWith("/api/employees");
            boolean isDiscounts = path.startsWith("/api/discounts");
            boolean isStatistics = path.startsWith("/api/statistics") || path.startsWith("/api/stats");
            if(!(isEmployees || isDiscounts || isStatistics)){
                filterChain.doFilter(request, response);
                return;
            }

            // allow read-only stats endpoint to be publicly accessible (GET) so dashboard can fetch data
            if(isStatistics && "GET".equalsIgnoreCase(request.getMethod())){
                filterChain.doFilter(request, response);
                return;
            }

            // allow preflight CORS requests through without auth
            if("OPTIONS".equalsIgnoreCase(request.getMethod())){
                filterChain.doFilter(request, response);
                return;
            }

            // require Authorization header
            String auth = request.getHeader("Authorization");
            if(auth == null || !auth.startsWith("Bearer ")){
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"Authentication required\"}");
                return;
            }
            String token = auth.substring(7).trim();
            String secret = System.getenv().getOrDefault("BAGISTORE_JWT_SECRET", "ChangeThisSecretToAStrongOneAtLeast32Chars!");
            Claims claims = JwtUtil.parseClaims(token, secret);
            if(claims == null){
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"Invalid token\"}");
                return;
            }
            // Previously we denied certain endpoints for sales role on the server-side.
            // Change: do not block sales users from accessing employees/discounts/statistics endpoints
            // to allow the frontend to merely hide the sidebar buttons while data remains accessible.
        }catch(Exception ex){
            // on error, fail closed for protected paths
            String path = request.getRequestURI() == null ? "" : request.getRequestURI();
            if(path.startsWith("/api/employees") || path.startsWith("/api/discounts") || path.startsWith("/api/statistics") || path.startsWith("/api/stats")){
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"Access denied\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
