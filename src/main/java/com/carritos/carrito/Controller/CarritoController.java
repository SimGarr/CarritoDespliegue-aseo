package com.carritos.carrito.Controller;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.carritos.carrito.Model.TransaccionRequest;
import com.carritos.carrito.Service.CarritoService;
import com.carritos.carrito.Service.UsuarioCliente;
import com.carritos.carrito.Service.WebpayService;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/carrito")
public class CarritoController {

    private static final Logger logger = LoggerFactory.getLogger(CarritoController.class);

    @Autowired
    private CarritoService carritoService;

    @Autowired
    private WebpayService webpayService;

    @Autowired
    private UsuarioCliente usuarioRestClient;

    @Value("${webpay.return.url}")
    private String returnUrl;

    @Value("${frontend.url}")
    private String frontendUrl;

    record ApiResponse(String status, String message, Object data) {}

    @Operation(summary = "Iniciar el proceso de pago y crear transacción en Webpay")
    @PostMapping("/pagar")
    public ResponseEntity<ApiResponse> iniciarPago(@RequestBody Map<String, Long> body) {
        Long usuarioId = body.get("usuarioId");
        logger.info("Inicio de pago solicitado por usuarioId={}", usuarioId);

        if (usuarioId == null || usuarioId <= 0) {
            logger.warn("UsuarioId inválido recibido: {}", usuarioId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse("error", "UsuarioId inválido", null));
        }

        try {
            if (!carritoService.verificarStock(usuarioId)) {
                logger.warn("Stock insuficiente para usuarioId={}", usuarioId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse("error", "Stock insuficiente para uno o más productos", null));
            }

            int monto = carritoService.calcularTotalCompra(usuarioId);
            logger.info("Monto calculado para usuarioId {}: {}", usuarioId, monto);

            String buyOrder = UUID.randomUUID().toString().replace("-", "").substring(0, 26);

            TransaccionRequest request = new TransaccionRequest(
                    buyOrder, usuarioId.toString(), monto, returnUrl
            );

            String token = webpayService.crearTransaccion(request);
            String redirectUrl = "https://webpay3gint.transbank.cl/webpayserver/initTransaction?token_ws=" + token;

            logger.info("Transacción creada para usuarioId={}, token={}", usuarioId, token);

            Map<String, String> data = Map.of("token", token, "url", redirectUrl);
            return ResponseEntity.ok(new ApiResponse("success", "Transacción creada", data));

        } catch (Exception e) {
            logger.error("Error al iniciar el pago para usuarioId {}: {}", usuarioId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("error", "Error al iniciar el pago: " + e.getMessage(), null));
        }
    }

    @Operation(summary = "Recibir retorno de Webpay y procesar la confirmación del pago")
    @GetMapping("/retorno")
    public void recibirRetorno(@RequestParam("token_ws") String token, HttpServletResponse response) throws IOException {
        logger.info("Retorno de pago recibido con token_ws={}", token);

        if (token == null || token.isBlank()) {
            logger.warn("Token_ws inválido o vacío recibido");
            response.sendRedirect("http://10.15.67.191:5500/pagorechazado.html");
            return;
        }

        try {
            JsonNode transaccion = webpayService.confirmarTransaccion(token);
            int responseCode = transaccion.get("response_code").asInt();
            logger.info("Código de respuesta de Webpay: {}", responseCode);

            if (responseCode == 0) {
                Long usuarioId = Long.parseLong(transaccion.get("session_id").asText());
                int amount = transaccion.get("amount").asInt();

                logger.info("Pago aprobado para usuarioId={}, amount={}", usuarioId, amount);

                carritoService.descontarStock(usuarioId);

                var usuario = usuarioRestClient.obtenerUsuarioPorId(usuarioId);

                if (usuario != null && usuario.getEmail() != null) {
                    // Aquí enviamos el correo con detalle completo usando CarritoService
                    carritoService.enviarCorreoConfirmacionCompra(usuario.getEmail(), usuario.getNombre(), usuarioId);
                    logger.info("Correo de confirmación enviado a {}", usuario.getEmail());
                } else {
                    logger.warn("No se pudo obtener email para usuarioId={}", usuarioId);
                }

                carritoService.vaciarCarrito(usuarioId);

                response.sendRedirect("http://10.15.67.191:5500/pagoexitoso.html");
            } else {
                logger.info("Pago rechazado o cancelado, response_code={}", responseCode);
                response.sendRedirect("http://10.15.67.191:5500/pagorechazado.html");
            }

        } catch (Exception e) {
            logger.error("Error al confirmar el pago con token {}: {}", token, e.getMessage(), e);
            response.sendRedirect("http://10.15.67.191:5500/pagorechazado.html");
        }
    }
}
