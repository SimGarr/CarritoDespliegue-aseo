package com.carritos.carrito.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.carritos.carrito.Model.TransaccionRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WebpayService {

    private final RestTemplate restTemplate;

    @Value("${webpay.api.url}")
    private String webpayUrl;

    @Value("${webpay.commerce.code}")
    private String commerceCode;

    @Value("${webpay.api.key}")
    private String apiKey;

    public WebpayService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String crearTransaccion(TransaccionRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Tbk-Api-Key-Id", commerceCode.trim());
            headers.set("Tbk-Api-Key-Secret", apiKey.trim());

            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(request);

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    webpayUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode json = mapper.readTree(response.getBody());
            return json.get("token").asText();

        } catch (Exception e) {
            throw new RuntimeException("Error al crear transacción Webpay: " + e.getMessage(), e);
        }
    }

    public JsonNode confirmarTransaccion(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Tbk-Api-Key-Id", commerceCode.trim());
            headers.set("Tbk-Api-Key-Secret", apiKey.trim());
            headers.setAccept(List.of(MediaType.APPLICATION_JSON)); // 

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = webpayUrl + "/" + token;

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT, // 
                    entity,
                    String.class
            );

            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(response.getBody());

        } catch (Exception e) {
            throw new RuntimeException("Error al confirmar transacción Webpay: " + e.getMessage(), e);
        }
    }
}
