package com.carritos.carrito.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransaccionRequest {
    private String buy_order;
    private String session_id;
    private int amount;
    private String return_url;
}