package com.harsh.projectflow.gateway.filter;

import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter,Ordered {
	@Value("${jwt.secret}")
	private String secret;
	
	private static final List<String> PUBLIC_PATHS = List.of("/auth/register","/auth/login");
	
	private SecretKey getSignKey() {
		return Keys.hmacShaKeyFor(secret.getBytes());
	}
	
	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String path = exchange.getRequest().getURI().getPath();
		
		if(PUBLIC_PATHS.contains(path)) {
			return chain.filter(exchange);
		}
		String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
		if(authHeader  == null || !authHeader.startsWith("Bearer ")) {
			return unauthorized(exchange);
		}
		String token = authHeader.substring(7);
		try {
			Claims claims = Jwts.parser()
					.verifyWith(getSignKey())
					.build()
					.parseSignedClaims(token)
					.getPayload();
			
			Long userId = claims.get("userId",Long.class);
			String role = claims.get("role", String.class);
			
			ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
					.header("X-User-Id", String.valueOf(userId))
					.header("X-User-Role", role)
					.build();
			
			return chain.filter(exchange.mutate().request(mutatedRequest).build());
					
		}catch(Exception e) {
			return unauthorized(exchange);
		}
		
	}
	
	private Mono<Void> unauthorized(ServerWebExchange exchange){
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(HttpStatus.UNAUTHORIZED);
		return response.setComplete();
	}
	
	@Override
	public int getOrder() {
		return -1;
	}
}
