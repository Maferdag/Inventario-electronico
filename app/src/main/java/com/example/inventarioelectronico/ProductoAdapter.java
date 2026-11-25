
package com.example.inventarioelectronico;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.example.inventarioelectronico.dao.Producto;

import java.util.ArrayList;
import java.util.List;

public class ProductoAdapter
        extends RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder> {

    private final List<Producto> listaProductos = new ArrayList<>();
    private final OnProductoActionListener actionListener;

    // Interfaz para editar y eliminar productos
    public interface OnProductoActionListener {

        void onEliminarClick(Producto producto);

        void onEditarClick(Producto producto);
    }

    // Constructor
    public ProductoAdapter(
            List<Producto> listaProductos,
            OnProductoActionListener actionListener) {

        if (listaProductos != null) {
            this.listaProductos.addAll(listaProductos);
        }

        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ProductoViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_producto, parent, false);

        return new ProductoViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ProductoViewHolder holder,
            int position) {

        Producto producto = listaProductos.get(position);

        // Mostrar datos del producto
        holder.tvNombre.setText(producto.getNombre());

        holder.tvDescripcion.setText(
                producto.getDescripcion());

        holder.tvPrecio.setText(
                "Precio: $" + producto.getPrecio());

        holder.tvCantidad.setText(
                "Cant: " + producto.getCantidad());

        holder.tvProveedor.setText(
                "Proveedor: " + producto.getProveedor());

        // Botón Editar
        holder.btnEditar.setOnClickListener(v -> {

            if (actionListener != null) {
                actionListener.onEditarClick(producto);
            }
        });

        // Botón Eliminar
        holder.btnEliminar.setOnClickListener(v -> {

            if (actionListener != null) {
                actionListener.onEliminarClick(producto);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaProductos.size();
    }

    // Actualizar lista para la búsqueda
    public void actualizarLista(List<Producto> nuevosProductos) {

        listaProductos.clear();

        if (nuevosProductos != null) {
            listaProductos.addAll(nuevosProductos);
        }

        notifyDataSetChanged();
    }

    // ViewHolder
    public static class ProductoViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvNombre;
        TextView tvDescripcion;
        TextView tvPrecio;
        TextView tvCantidad;
        TextView tvProveedor;



        MaterialButton btnEditar;
        MaterialButton btnEliminar;

        public ProductoViewHolder(@NonNull View itemView) {

            super(itemView);

            // Identificadores reales de item_producto.xml
            tvNombre = itemView.findViewById(
                    R.id.tvNombreItem);

            tvDescripcion = itemView.findViewById(
                    R.id.tvDescripcionItem);

            tvPrecio = itemView.findViewById(
                    R.id.tvPrecioItem);

            tvCantidad = itemView.findViewById(
                    R.id.tvCantidadItem);

            tvProveedor = itemView.findViewById(
                    R.id.tvProveedorItem);

            btnEditar = itemView.findViewById(
                    R.id.btnEditarItem);

            btnEliminar = itemView.findViewById(
                    R.id.btnEliminarItem);
        }
    }
}
