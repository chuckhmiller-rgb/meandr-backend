package com.meandr.meandrDataServices;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = {"/hello", "/hi/*"})   // ← very important!
public class HelloServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        resp.setContentType("text/plain");
        resp.getWriter().println("Hello from classic Servlet! 🎉");
        resp.getWriter().println("Path: " + req.getRequestURI());
    }
}