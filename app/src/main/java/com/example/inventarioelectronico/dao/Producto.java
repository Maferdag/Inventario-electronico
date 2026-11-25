package com.example.inventarioelectronico.dao;

import androidx.annotation.NonNull;
import androidx.room3.Entity;
import androidx.room3.PrimaryKey;
import java.util.Objects;
import java.util.UUID;

@Entity(tableName = "inventario")
public class Producto {

    @PrimaryKey
    @NonNull // Las llaves primarias en formato String no pueden ser nulas en Room
    private String id;

    private String nombre;
    private int cantidad;
    private String descripcion;
    private int precio;
    private String proveedor;

    // Constructor vacío requerido por Room
    public Producto() {
        this.id = "";
    }

    public Producto(String nombre, int precio, int cantidad, String proveedor, String descripcion) {
        // Generación automática del ID Único Universal
        this.id = UUID.randomUUID().toString();
        this.nombre = nombre;
        this.precio = precio;
        this.cantidad = cantidad;
        this.proveedor = proveedor;
        this.descripcion = descripcion;
    }

    // Métodos Getter y Setter actualizados a String
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getPrecio() { return precio; }
    public void setPrecio(int precio ) { this.precio = precio; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Producto producto = (Producto) o;
        return cantidad == producto.cantidad && precio == producto.precio && Objects.equals(id, producto.id) && Objects.equals(nombre, producto.nombre) && Objects.equals(descripcion, producto.descripcion) && Objects.equals(proveedor, producto.proveedor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nombre, cantidad, descripcion, precio, proveedor);
    }

    @Override
    public String toString() {
        return "Producto{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", cantidad=" + cantidad +
                ", descripcion='" + descripcion + '\'' +
                ", precio=" + precio +
                ", proveedor='" + proveedor + '\'' +
                '}';
    }
}
