package com.airepair.auth;
import io.jsonwebtoken.Jwts; import io.jsonwebtoken.security.Keys; import javax.crypto.SecretKey; import java.nio.charset.StandardCharsets; import java.util.Date;
import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Component;
@Component public class JwtService {
 private final SecretKey key;
 public JwtService(@Value("${app.jwt-secret}") String secret){if(secret.getBytes(StandardCharsets.UTF_8).length<32) throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));}
 public String issue(long id){long now=System.currentTimeMillis();return Jwts.builder().subject(Long.toString(id)).issuedAt(new Date(now)).expiration(new Date(now+8*60*60*1000L)).signWith(key).compact();}
 public long parse(String token){return Long.parseLong(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject());}
}
