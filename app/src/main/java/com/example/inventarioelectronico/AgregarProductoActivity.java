package com.example.inventarioelectronico;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.inventarioelectronico.dao.AppDatabase;
import com.example.inventarioelectronico.dao.Producto;
import com.example.inventarioelectronico.nube.SincronizadorNube;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AgregarProductoActivity extends AppCompatActivity {

    // Cambiado a String para dar soporte a los nuevos identificadores UUID
    private String productoId = null;
    private boolean esModoEdicion = false;

    private void subirANube(Producto producto) {
        SincronizadorNube.subirProducto(getApplicationContext(), producto);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_agregar_producto);

        TextView tvTituloFormulario = findViewById(R.id.tvTituloFormulario);
        Button botonGuardarProducto = findViewById(R.id.btnGuardarProducto);
        Button botonCancelar = findViewById(R.id.btnCancelar);
        EditText etNombre = findViewById(R.id.etNombre);
        EditText etPrecio = findViewById(R.id.etPrecio);
        EditText etCantidad = findViewById(R.id.etCantidad);
        EditText etProveedor = findViewById(R.id.etProveedor);
        EditText etDescripcion = findViewById(R.id.etDescripcion);

        botonCancelar.setOnClickListener(v -> finish());

        // DETECTAR SI ES MODO EDICIÓN
        if (getIntent().hasExtra("PRODUCTO_ID")) {
            esModoEdicion = true;
            // Recuperamos el ID como String de los extras del Intent
            productoId = getIntent().getStringExtra("PRODUCTO_ID");

            tvTituloFormulario.setText("Modificar producto");
            botonGuardarProducto.setText("Actualizar cambios");

            etNombre.setText(getIntent().getStringExtra("PRODUCTO_NOMBRE"));
            etDescripcion.setText(getIntent().getStringExtra("PRODUCTO_DESCRIPCION"));
            etCantidad.setText(String.valueOf(getIntent().getIntExtra("PRODUCTO_CANTIDAD", 0)));
            etPrecio.setText(String.valueOf(getIntent().getIntExtra("PRODUCTO_PRECIO", 0)));
            etProveedor.setText(getIntent().getStringExtra("PRODUCTO_PROVEEDOR"));
        }

        AppDatabase database = AppDatabase.getDatabase(this);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        botonGuardarProducto.setOnClickListener(v -> {
            String nombreProducto = etNombre.getText().toString();
            String precioStr = etPrecio.getText().toString();
            String cantidadStr = etCantidad.getText().toString();
            String proveedorProducto = etProveedor.getText().toString();
            String descripcionProducto = etDescripcion.getText().toString();

            if (nombreProducto.isEmpty() || precioStr.isEmpty() || cantidadStr.isEmpty()) {
                Toast.makeText(this, "Por favor llena los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            int precioProducto;
            int cantidadProducto;

            try {
                precioProducto = Integer.parseInt(precioStr.trim());
                cantidadProducto = Integer.parseInt(cantidadStr.trim());
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Ingresa una cantidad y un precio válidos", Toast.LENGTH_LONG).show();
                return;
            }

            if (precioProducto < 0 || cantidadProducto < 0) {
                Toast.makeText(this, "El precio y la cantidad no pueden ser negativos", Toast.LENGTH_LONG).show();
                return;
            }

            executor.execute(() -> {
                if (esModoEdicion) {
                    // En modo edición creamos el objeto y le reasignamos su UUID original de la BD
                    Producto producto = new Producto(
                            nombreProducto,
                            precioProducto,
                            cantidadProducto,
                            proveedorProducto,
                            descripcionProducto
                    );
                    producto.setId(productoId);

                    database.productoDao().actualizar(producto);
                    subirANube(producto);

                    mainHandler.post(() -> {
                        Toast.makeText(AgregarProductoActivity.this, "¡Modificación exitosa!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    // MODO CREACIÓN: El constructor genera el UUID aleatorio de inmediato
                    Producto producto = new Producto(
                            nombreProducto,
                            precioProducto,
                            cantidadProducto,
                            proveedorProducto,
                            descripcionProducto
                    );

                    // Insertamos de manera directa en SQLite (Room)
                    database.productoDao().insertar(producto);

                    // Delegamos la subida a SincronizadorNube (gestionará colas si está offline)
                    subirANube(producto);

                    mainHandler.post(() -> {
                        Toast.makeText(AgregarProductoActivity.this, "¡Guardado exitoso!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            });
        });
    }
}
