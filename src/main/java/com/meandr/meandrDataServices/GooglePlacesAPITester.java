package com.meandr.meandrDataServices;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import com.google.maps.PlacesApi;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;

@WebServlet(urlPatterns = {"/hello", "/hi/*"})   // ← very important!
public class GooglePlacesAPITester extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        GeoApiContext context = new GeoApiContext.Builder().apiKey("YOUR_API_KEY").build();
        
        try {
            resp = (HttpServletResponse) PlacesApi.textSearchQuery(context, "restaurant in Paris").await();
        } catch (ApiException ex) {
            System.getLogger(GooglePlacesAPITester.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        } catch (InterruptedException ex) {
            System.getLogger(GooglePlacesAPITester.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        resp.setContentType("text/plain");
        resp.getWriter().println("Hello from classic Servlet! 🎉");
        resp.getWriter().println("Here's the output of the Google Places search 🎉" + resp);
        resp.getWriter().println("Path: " + req.getRequestURI());
    }
}
