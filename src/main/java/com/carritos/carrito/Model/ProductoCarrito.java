package com.carritos.carrito.Model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductoCarrito {
    private String nombre;
    private int cantidad;
    private int precio;
}