package com.KTU.KTUVotingapp.config;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class DeviceCookieFilter implements Filter {

    public static final String COOKIE_NAME = "voting_device_id";
    private static final int COOKIE_MAX_AGE = 60 * 60 * 24 * 365; // 1 year

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            Cookie[] cookies = httpRequest.getCookies();
            String deviceId = null;

            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (COOKIE_NAME.equals(cookie.getName())) {
                        deviceId = cookie.getValue();
                        break;
                    }
                }
            }

            if (deviceId == null || deviceId.isBlank()) {
                deviceId = UUID.randomUUID().toString();
                Cookie newCookie = new Cookie(COOKIE_NAME, deviceId);
                newCookie.setPath("/");
                newCookie.setHttpOnly(true); // Prevent client-side script access
                newCookie.setMaxAge(COOKIE_MAX_AGE);
                newCookie.setSecure(false); // Set to true when running under HTTPS
                // newCookie.setSecure(true); // Enable in production with HTTPS
                httpResponse.addCookie(newCookie);
            }
        }

        chain.doFilter(request, response);
    }
}
