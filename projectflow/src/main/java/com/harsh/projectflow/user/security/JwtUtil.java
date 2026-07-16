package com.harsh.projectflow.user.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.expiration}")
	private Long expirationMs;

	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(secret.getBytes()); //// convert it into HMAC -SHA2556 format. - cryptographic format.
	}

	public String generateToken(Long userId, String email, String role) {  // GENERATE A wrist band from this line of code
		Date now = new Date();
		Date expiry = new Date(now.getTime() + expirationMs);

		return  Jwts.builder()
					.subject(email)
					.claim("userId", userId)
					.claim("role", role)
					.issuedAt(now)
					.expiration(expiry)
					.signWith(getSigningKey())
					.compact();

	}

	public Claims extractClaims(String token) {			// check if the code is valid or not.
		return Jwts.parser()
				.verifyWith(getSigningKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	public boolean isTokenValid(String token) { // since JWT might throw a lot exceptions..
		try {
			extractClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
