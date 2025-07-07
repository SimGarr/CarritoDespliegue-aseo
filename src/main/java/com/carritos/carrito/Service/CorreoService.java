package com.carritos.carrito.Service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CorreoService {

    private final String API_KEY = "46d929d68b42c6ede555166d10c726b2";
    private final String API_SECRET = "b51d367bfd99999eb5772f02739f9e89";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Envía correo con el detalle de la compra en HTML.
     * 
     * @param destinatario Email del usuario
     * @param nombre Nombre del usuario
     * @param detalleCompraHTML Tabla HTML con los productos y totales
     */
    public void enviarCorreo(String destinatario, String nombre, String detalleCompraHTML) {
        String url = "https://api.mailjet.com/v3.1/send";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(API_KEY, API_SECRET);

        String body = """
        {
          "Messages":[
            {
              "From": {
                "Email": "sim.garrido2002@gmail.com",
                "Name": "AseoMayorista"
              },
              "To": [
                {
                  "Email": "%s",
                  "Name": "%s"
                }
              ],
              "Subject": "¡Gracias por tu compra!",
              "TextPart": "Tu compra ha sido realizada exitosamente.",
              "HTMLPart": "<h3>Gracias por tu compra, %s!</h3><p>Tu pedido está siendo procesado.</p>%s"
            }
          ]
        }
        """.formatted(destinatario, nombre, nombre, detalleCompraHTML);

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(url, request, String.class);
    }
}
