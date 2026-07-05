package mass.backendmass.controller;

import mass.backendmass.models.Categoria;
import mass.backendmass.models.Producto;
import mass.backendmass.repository.CategoriaRepository;
import mass.backendmass.repository.DetalleVentaRepository;
import mass.backendmass.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private CategoriaRepository categoriaRepository;
    
    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @GetMapping
    public List<Map<String, Object>> listarProductos(
            @RequestParam(required = false) Integer categoria,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax
    ) {
        List<Producto> productos = productoService.listarProductos(categoria, precioMin, precioMax);
        return productos.stream().map(producto -> {
            Map<String, Object> productoConVentas = new HashMap<>();
            productoConVentas.put("id_producto", producto.getId_producto());
            productoConVentas.put("codigo_barras", producto.getCodigo_barras());
            productoConVentas.put("nombre", producto.getNombre());
            productoConVentas.put("descripcion", producto.getDescripcion());
            productoConVentas.put("precio", producto.getPrecio());
            productoConVentas.put("stock", producto.getStock());
            productoConVentas.put("id_categoria", producto.getId_categoria());
            productoConVentas.put("id_proveedor", producto.getId_proveedor());
            productoConVentas.put("fecha_ingreso", producto.getFecha_ingreso());
            productoConVentas.put("fecha_vencimiento", producto.getFecha_vencimiento());
            productoConVentas.put("lote", producto.getLote());
            productoConVentas.put("categoria", producto.getCategoria());
            productoConVentas.put("imagen", producto.getImagen());
            productoConVentas.put("activo", producto.isActivo());
            productoConVentas.put("fecha_registro", producto.getFecha_registro());
            
            // Agregar número de ventas
            Long ventas = detalleVentaRepository.countVentasByProductoId(producto.getId_producto());
            productoConVentas.put("ventas", ventas != null ? ventas : 0);
            
            return productoConVentas;
        }).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerProductoPorId(@PathVariable int id) {
        Optional<Producto> producto = productoService.obtenerPorId(id);
        return producto.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crearProducto(@RequestBody Producto producto) {
        try {
            // Asegurar que el producto esté activo por defecto
            producto.setActivo(true);
            
            // Si id_proveedor viene como 0, asignar proveedor por defecto (1)
            if (producto.getId_proveedor() == 0) {
                producto.setId_proveedor(1);
            }
            
            // Si tiene id_categoria, buscar el nombre de la categoría
            if (producto.getId_categoria() > 0) {
                Optional<Categoria> categoria = categoriaRepository.findById(producto.getId_categoria());
                if (categoria.isPresent()) {
                    producto.setCategoria(categoria.get().getNombre_categoria());
                } else {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("mensaje", "Categoría con ID " + producto.getId_categoria() + " no encontrada");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }
            
            Producto nuevoProducto = productoService.guardarProducto(producto);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("producto", nuevoProducto);
            response.put("mensaje", "Producto creado correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("mensaje", "Error al crear producto: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarProducto(
            @PathVariable int id, 
            @RequestBody Producto producto) {
        try {
            // Verificar que el producto existe y está activo
            Optional<Producto> productoExistente = productoService.obtenerPorId(id);
            if (!productoExistente.isPresent()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("mensaje", "Producto no encontrado");
                return ResponseEntity.status(404).body(errorResponse);
            }
            
            // Si id_proveedor viene como 0 o null, usar el del producto existente
            if (producto.getId_proveedor() == 0) {
                producto.setId_proveedor(productoExistente.get().getId_proveedor());
            }
            
            // Si tiene id_categoria, buscar el nombre de la categoría
            if (producto.getId_categoria() > 0) {
                Optional<Categoria> categoria = categoriaRepository.findById(producto.getId_categoria());
                if (categoria.isPresent()) {
                    producto.setCategoria(categoria.get().getNombre_categoria());
                } else {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("mensaje", "Categoría con ID " + producto.getId_categoria() + " no encontrada");
                    return ResponseEntity.badRequest().body(errorResponse);
                }
            }
            
            // Mantener el estado activo del producto existente
            producto.setActivo(productoExistente.get().isActivo());

            // Merge partial update into existing product to avoid setting non-included fields to null
            Producto productoParaActualizar = productoExistente.get();

            // Strings - keep existing if incoming is null or empty
            if (producto.getCodigo_barras() != null && !producto.getCodigo_barras().isEmpty())
                productoParaActualizar.setCodigo_barras(producto.getCodigo_barras());
            if (producto.getNombre() != null && !producto.getNombre().isEmpty())
                productoParaActualizar.setNombre(producto.getNombre());
            if (producto.getDescripcion() != null && !producto.getDescripcion().isEmpty())
                productoParaActualizar.setDescripcion(producto.getDescripcion());
            if (producto.getCategoria() != null && !producto.getCategoria().isEmpty())
                productoParaActualizar.setCategoria(producto.getCategoria());
            if (producto.getLote() != null && !producto.getLote().isEmpty())
                productoParaActualizar.setLote(producto.getLote());
            if (producto.getImagen() != null && !producto.getImagen().isEmpty())
                productoParaActualizar.setImagen(producto.getImagen());

            // Primitives - only update if different from default 0
            if (producto.getPrecio() != 0.0)
                productoParaActualizar.setPrecio(producto.getPrecio());
            if (producto.getStock() != 0)
                productoParaActualizar.setStock(producto.getStock());
            if (producto.getId_categoria() > 0)
                productoParaActualizar.setId_categoria(producto.getId_categoria());
            if (producto.getId_proveedor() != 0)
                productoParaActualizar.setId_proveedor(producto.getId_proveedor());

            // Date-like fields: only update if provided (not null)
            if (producto.getFecha_ingreso() != null)
                productoParaActualizar.setFecha_ingreso(producto.getFecha_ingreso());
            if (producto.getFecha_vencimiento() != null)
                productoParaActualizar.setFecha_vencimiento(producto.getFecha_vencimiento());

            Producto actualizado = productoService.guardarProducto(productoParaActualizar);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("producto", actualizado);
            response.put("mensaje", "Producto actualizado correctamente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("mensaje", "Error al actualizar producto: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProducto(@PathVariable int id) {
        boolean eliminado = productoService.eliminarProducto(id);
        Map<String, Object> response = new HashMap<>();
        if (eliminado) {
            response.put("success", true);
            response.put("mensaje", "Producto eliminado correctamente");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("mensaje", "Producto no encontrado");
            return ResponseEntity.status(404).body(response);
        }
    }
    
    @PutMapping("/ajustar-stock")
    public ResponseEntity<?> ajustarStockMasivo(@RequestParam int maxStock) {
        try {
            List<Producto> productos = productoService.listarProductos(null, null, null);
            int actualizados = 0;
            
            for (Producto producto : productos) {
                if (producto.getStock() > maxStock) {
                    producto.setStock(maxStock);
                    productoService.guardarProducto(producto);
                    actualizados++;
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("mensaje", "Stock ajustado correctamente");
            response.put("productosActualizados", actualizados);
            response.put("stockMaximo", maxStock);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("mensaje", "Error al ajustar stock: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
