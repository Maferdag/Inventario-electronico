package com.example.inventarioelectronico.nube;

import com.example.inventarioelectronico.dao.Producto;

public class ProductoNube {
    private String id;
    private String nombre;
    private String descripcion;
    private int cantidad;
    private int precio;
    private String proveedor;

    // Obligatorio: Firestore lo usa para armar el objeto al leer datos
    public ProductoNube() { }

    public ProductoNube(String id, String nombre, String descripcion,
                        int cantidad, int precio, String proveedor) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precio = precio;
        this.proveedor = proveedor;
    }

    // Convierte un producto de Room (local) en uno para la nube.
    // Usamos el id de Room, convertido a texto, como id del documento.
    public static ProductoNube desdeRoom(Producto p) {
        return new ProductoNube(String.valueOf(p.getId()), p.getNombre(),
                p.getDescripcion(), p.getCantidad(), p.getPrecio(), p.getProveedor());
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public int getPrecio() { return precio; }
    public void setPrecio(int precio) { this.precio = precio; }
    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }
}