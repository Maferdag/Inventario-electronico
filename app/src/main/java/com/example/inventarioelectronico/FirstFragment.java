package com.example.inventarioelectronico;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.inventarioelectronico.dao.AppDatabase;
import com.example.inventarioelectronico.dao.Producto;
import com.example.inventarioelectronico.databinding.FragmentFirstBinding;
import com.example.inventarioelectronico.nube.SincronizadorNube;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private RecyclerView recyclerView;
    private EditText etBuscarProducto;
    private AppDatabase database;
    private ProductoAdapter adaptador;

    // Variable global para controlar el filtro por categoría seleccionado
    private String categoriaActual = "Todos";

    private final List<Producto> todosLosProductos =
            new ArrayList<>();

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentFirstBinding.inflate(
                inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        database = AppDatabase.getDatabase(requireContext());

        // Usamos View Binding directamente en lugar de findViewById
        recyclerView = binding.recyclerViewProductos;

        recyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext()));

        etBuscarProducto = binding.etBuscarProducto;

        // Configurar lista de productos
        adaptador = new ProductoAdapter(
                new ArrayList<>(),
                new ProductoAdapter.OnProductoActionListener() {

                    @Override
                    public void onEliminarClick(Producto producto) {
                        confirmarYEliminarProducto(producto);
                    }

                    @Override
                    public void onEditarClick(Producto producto) {
                        abrirFormularioEdicion(producto);
                    }
                });

        recyclerView.setAdapter(adaptador);

        // Abrir formulario para agregar
        binding.btnAgregarProducto.setOnClickListener(v -> {
            Intent intent = new Intent(
                    requireContext(),
                    AgregarProductoActivity.class);

            startActivity(intent);
        });

        // Configurar menú emergente para el Botón de Filtro
        binding.btnFiltrarProducto.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), v);

            // Opciones del menú desplegable
            popup.getMenu().add(1, 1, 1, "Todos");
            popup.getMenu().add(1, 2, 2, "Nombre");
            popup.getMenu().add(1, 3, 3, "Proveedor");
            popup.getMenu().add(1, 4, 4, "Descripcion");

            popup.setOnMenuItemClickListener(item -> {
                categoriaActual = item.getTitle().toString();

                // Forzar el filtrado combinando el texto escrito y la categoría elegida
                filtrarProductos(etBuscarProducto.getText().toString());

                Toast.makeText(requireContext(), "Filtro: " + categoriaActual, Toast.LENGTH_SHORT).show();
                return true;
            });
            popup.show();
        });

        // Buscar productos mientras se escribe (Corregido)
        etBuscarProducto.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        filtrarProductos(s.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (binding != null) {
            cargarProductosDesdeBaseDeDatos();
        }
    }

    private void cargarProductosDesdeBaseDeDatos() {

        executor.execute(() -> {

            try {
                List<Producto> productos =
                        database.productoDao().obtenerTodos();

                List<Producto> copia = productos == null
                        ? new ArrayList<>()
                        : new ArrayList<>(productos);

                mainHandler.post(() -> {

                    if (binding == null) {
                        return;
                    }

                    todosLosProductos.clear();
                    todosLosProductos.addAll(copia);

                    filtrarProductos(
                            etBuscarProducto.getText().toString());
                });

            } catch (Exception e) {

                mainHandler.post(() -> {

                    if (binding != null) {
                        Toast.makeText(
                                requireContext(),
                                "Error al cargar productos",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
            }
        });
    }

    // Filtra la lista evaluando simultáneamente el texto ingresado y el botón selector
    // Filtra la lista evaluando el texto ingresado en múltiples campos y el filtro activo
    private void filtrarProductos(String texto) {
        if (adaptador == null) {
            return;
        }

        // Limpiamos el texto ingresado por el usuario
        String busqueda = texto.trim().toLowerCase(Locale.ROOT);
        List<Producto> filtrados = new ArrayList<>();

        for (Producto producto : todosLosProductos) {
            // Si el buscador está vacío, mostramos el producto directamente
            if (busqueda.isEmpty()) {
                filtrados.add(producto);
                continue;
            }

            String nombre = producto.getNombre() != null ? producto.getNombre().toLowerCase(Locale.ROOT) : "";
            String descripcion = producto.getDescripcion() != null ? producto.getDescripcion().toLowerCase(Locale.ROOT) : "";
            String proveedor = producto.getProveedor() != null ? producto.getProveedor().toLowerCase(Locale.ROOT) : "";

            boolean coincide = false;

            // Evaluamos según el tipo de filtro seleccionado en el botón
            switch (categoriaActual) {
                case "Nombre":
                    coincide = nombre.contains(busqueda);
                    break;
                case "Descripcion":
                    coincide = descripcion.contains(busqueda);
                    break;
                case "Proveedor":
                    coincide = proveedor.contains(busqueda);
                    break;
                case "Todos":
                default:
                    // Si es "Todos", busca en cualquiera de los tres campos
                    coincide = nombre.contains(busqueda) ||
                            descripcion.contains(busqueda) ||
                            proveedor.contains(busqueda);
                    break;
            }

            if (coincide) {
                filtrados.add(producto);
            }
        }

        // Actualizamos el RecyclerView con la lista filtrada
        adaptador.actualizarLista(filtrados);
    }

    private void abrirFormularioEdicion(Producto producto) {

        Intent intent = new Intent(
                requireContext(),
                AgregarProductoActivity.class);

        intent.putExtra(
                "PRODUCTO_ID", producto.getId());

        intent.putExtra(
                "PRODUCTO_NOMBRE", producto.getNombre());

        intent.putExtra(
                "PRODUCTO_DESCRIPCION",
                producto.getDescripcion());

        intent.putExtra(
                "PRODUCTO_CANTIDAD",
                producto.getCantidad());

        intent.putExtra(
                "PRODUCTO_PRECIO",
                producto.getPrecio());

        intent.putExtra(
                "PRODUCTO_PROVEEDOR",
                producto.getProveedor());

        startActivity(intent);
    }

    // Se corrigió el cierre del callback anónimo de FirestoreRepository
    private void confirmarYEliminarProducto(Producto producto) {

        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar producto")
                .setMessage("¿Deseas eliminar '" + producto.getNombre() + "'?")
                .setPositiveButton("Eliminar", (dialog, which) -> {

                    executor.execute(() -> {
                        try {
                            // 1. Eliminación local
                            database.productoDao().eliminar(producto);

                            // También borramos el producto en la nube
                            // (o queda pendiente si no hay conexión)
                            SincronizadorNube.eliminarProducto(
                                    requireContext().getApplicationContext(),
                                    producto.getId());

                            mainHandler.post(() -> {

                                if (binding != null) {

                                    Toast.makeText(
                                            requireContext(),
                                            "Producto eliminado",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    cargarProductosDesdeBaseDeDatos();
                                }
                            });

                        } catch (Exception e) {

                            mainHandler.post(() -> {

                                if (binding != null) {
                                    Toast.makeText(
                                            requireContext(),
                                            "Error al eliminar producto",
                                            Toast.LENGTH_LONG
                                    ).show();
                                }
                            });
                        }
                    });
                })
                .setNegativeButton(
                        "Cancelar",
                        (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        recyclerView = null;
        etBuscarProducto = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}