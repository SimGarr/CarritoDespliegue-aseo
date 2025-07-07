package com.carritos.carrito.Service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.carritos.carrito.Model.CarritoItem;
import com.carritos.carrito.Model.ProductoCompletoDTO;
import com.carritos.carrito.Repository.CarritoItemRepository;

@Service
public class CarritoService {

    private static final Logger logger = LoggerFactory.getLogger(CarritoService.class);

    @Autowired
    private CarritoItemRepository carritoItemRepo;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CorreoService correoService;

    private final String PRODUCTOS_URL = "http://localhost:8082/api/productos/";

    public void agregarProducto(Long usuarioId, Long productoId, int cantidad) {
        logger.info("Agregando producto al carrito: usuarioId={}, productoId={}, cantidad={}", usuarioId, productoId, cantidad);

        if (productoId == null || productoId <= 0) {
            logger.error("Intento de agregar producto con productoId inválido: {}", productoId);
            throw new IllegalArgumentException("productoId inválido");
        }
        if (cantidad <= 0) {
            logger.error("Intento de agregar producto con cantidad inválida: {}", cantidad);
            throw new IllegalArgumentException("Cantidad debe ser mayor a cero");
        }

        CarritoItem item = new CarritoItem(null, usuarioId, productoId, cantidad);
        carritoItemRepo.save(item);
        logger.info("Producto agregado al carrito correctamente.");
    }

    public List<CarritoItem> obtenerCarritoPorUsuario(Long usuarioId) {
        logger.debug("Obteniendo items del carrito para usuarioId={}", usuarioId);
        return carritoItemRepo.findByUsuarioId(usuarioId);
    }

    public int calcularTotalCompra(Long usuarioId) {
        logger.debug("Calculando total compra para usuarioId={}", usuarioId);
        List<CarritoItem> items = obtenerCarritoPorUsuario(usuarioId);
        int total = 0;
        for (CarritoItem item : items) {
            try {
                ProductoCompletoDTO producto = obtenerProductoCompleto(item.getProductoId());
                if (producto == null) {
                    logger.warn("Producto no encontrado para productoId={}", item.getProductoId());
                    continue;
                }
                total += producto.getPrecio() * item.getCantidad();
            } catch (RestClientException e) {
                logger.error("Error al obtener producto {}: {}", item.getProductoId(), e.getMessage());
            }
        }
        logger.info("Total calculado para usuarioId {}: {}", usuarioId, total);
        return total;
    }

    public boolean verificarStock(Long usuarioId) {
        logger.debug("Verificando stock para usuarioId={}", usuarioId);
        List<CarritoItem> items = obtenerCarritoPorUsuario(usuarioId);
        for (CarritoItem item : items) {
            Long prodId = item.getProductoId();
            if (prodId == null || prodId <= 0) {
                logger.warn("Producto inválido en carrito: productoId={}", prodId);
                return false;
            }
            try {
                ProductoCompletoDTO producto = obtenerProductoCompleto(prodId);
                if (producto == null) {
                    logger.warn("Producto no encontrado para productoId={}", prodId);
                    return false;
                }
                if (producto.getStock() < item.getCantidad()) {
                    logger.warn("Stock insuficiente para productoId={}, stock={}, requerido={}", prodId, producto.getStock(), item.getCantidad());
                    return false;
                }
            } catch (RestClientException e) {
                logger.error("Error al obtener producto {}: {}", prodId, e.getMessage());
                return false;
            }
        }
        logger.info("Stock verificado OK para usuarioId={}", usuarioId);
        return true;
    }

    @Transactional
    public void descontarStock(Long usuarioId) {
        logger.info("Descontando stock para usuarioId={}", usuarioId);
        List<CarritoItem> items = obtenerCarritoPorUsuario(usuarioId);
        for (CarritoItem item : items) {
            Long prodId = item.getProductoId();
            if (prodId == null || prodId <= 0) {
                logger.warn("Producto inválido en carrito durante descuento de stock: productoId={}", prodId);
                continue;
            }
            try {
                ProductoCompletoDTO producto = obtenerProductoCompleto(prodId);
                if (producto == null) {
                    logger.warn("Producto no encontrado para productoId={} al descontar stock", prodId);
                    continue;
                }
                int nuevoStock = producto.getStock() - item.getCantidad();
                if (nuevoStock < 0) {
                    logger.warn("Nuevo stock sería negativo para productoId={} (actual: {}, restando: {})", prodId, producto.getStock(), item.getCantidad());
                    continue;
                }
                logger.debug("Actualizando stock productoId={} a nuevoStock={}", prodId, nuevoStock);
                restTemplate.put(PRODUCTOS_URL + prodId + "/stock?cantidad=" + nuevoStock, null);
            } catch (RestClientException e) {
                logger.error("Error al descontar stock para productoId {}: {}", prodId, e.getMessage());
            }
        }
        logger.info("Stock descontado correctamente para usuarioId={}", usuarioId);
    }

    @Transactional
    public void vaciarCarrito(Long usuarioId) {
        logger.info("Vaciando carrito para usuarioId={}", usuarioId);
        carritoItemRepo.deleteByUsuarioId(usuarioId);
        logger.info("Carrito vaciado para usuarioId={}", usuarioId);
    }

    private ProductoCompletoDTO obtenerProductoCompleto(Long productoId) {
        try {
            logger.debug("Consultando producto completo con id={}", productoId);
            return restTemplate.getForObject(PRODUCTOS_URL + productoId, ProductoCompletoDTO.class);
        } catch (RestClientException e) {
            logger.error("Error al obtener producto con id {}: {}", productoId, e.getMessage());
            return null;
        }
    }

    /**
     * Envía correo de confirmación con detalle de compra.
     * 
     * @param emailEmail del usuario comprador
     * @param nombreUsuario Nombre del usuario comprador
     * @param usuarioId ID del usuario (para obtener items)
     */
    public void enviarCorreoConfirmacionCompra(String emailEmail, String nombreUsuario, Long usuarioId) {
        List<CarritoItem> items = obtenerCarritoPorUsuario(usuarioId);

        // Obtener productos con detalles
        List<ProductoCompletoDTO> productos = items.stream()
                .map(item -> obtenerProductoCompleto(item.getProductoId()))
                .collect(Collectors.toList());

        // Construir tabla HTML con productos, cantidades y subtotal
        StringBuilder tablaHTML = new StringBuilder();
        tablaHTML.append("<table border='1' style='border-collapse: collapse; width: 100%;'>");
        tablaHTML.append("<thead style='background-color:#f2f2f2;'>");
        tablaHTML.append("<tr>");
        tablaHTML.append("<th>Producto</th><th>Cantidad</th><th>Precio Unitario</th><th>Subtotal</th>");
        tablaHTML.append("</tr></thead><tbody>");

        double total = 0;

        for (int i = 0; i < items.size(); i++) {
            CarritoItem item = items.get(i);
            ProductoCompletoDTO producto = productos.get(i);

            if (producto == null) continue;

            double subtotal = producto.getPrecio() * item.getCantidad();
            total += subtotal;

            tablaHTML.append("<tr>")
                     .append("<td>").append(producto.getNombre()).append("</td>")
                     .append("<td style='text-align:center;'>").append(item.getCantidad()).append("</td>")
                     .append("<td style='text-align:right;'>$").append(producto.getPrecio()).append("</td>")
                     .append("<td style='text-align:right;'>$").append(subtotal).append("</td>")
                     .append("</tr>");
        }

        tablaHTML.append("</tbody>");
        tablaHTML.append("<tfoot>");
        tablaHTML.append("<tr style='font-weight:bold;'>")
                 .append("<td colspan='3' style='text-align:right;'>Total pagado:</td>")
                 .append("<td style='text-align:right;'>$").append(total).append("</td>")
                 .append("</tr>");
        tablaHTML.append("</tfoot>");
        tablaHTML.append("</table>");

        // Llamar al servicio de correo con el contenido HTML generado
        correoService.enviarCorreo(emailEmail, nombreUsuario, tablaHTML.toString());
    }
}
