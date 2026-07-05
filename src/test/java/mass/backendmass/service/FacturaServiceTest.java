package mass.backendmass.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.io.ByteArrayInputStream;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import mass.backendmass.models.DetalleVenta;
import mass.backendmass.models.Factura;
import mass.backendmass.models.Venta;
import mass.backendmass.repository.ClienteRepository;
import mass.backendmass.repository.DetalleVentaRepository;
import mass.backendmass.repository.FacturaRepository;
import mass.backendmass.repository.ProductoRepository;
import mass.backendmass.repository.VentaRepository;

@ExtendWith(MockitoExtension.class)
public class FacturaServiceTest {

    @Mock
    FacturaRepository facturaRepository;

    @Mock
    VentaRepository ventaRepository;

    @Mock
    DetalleVentaRepository detalleVentaRepository;

    @Mock
    ClienteRepository clienteRepository;

    @Mock
    ProductoRepository productoRepository;

    @InjectMocks
    FacturaService facturaService;

    @Test
    public void generarPdfFactura_muestraProductoIGVyTotal() throws Exception {
        // Arrange
        Factura factura = new Factura();
        factura.setId_factura(1);
        factura.setId_venta(10);
        factura.setNumero_factura("F-0001");
        factura.setFecha_emision(new java.sql.Timestamp(System.currentTimeMillis()));
        factura.setTotal(BigDecimal.valueOf(118.00)); // total including IGV for example

        Venta venta = new Venta();
        venta.setId_venta(10);
        venta.setId_cliente(null);
        venta.setFecha_venta(new Timestamp(System.currentTimeMillis()));

        DetalleVenta d1 = new DetalleVenta();
        d1.setId_detalle(100);
        d1.setId_venta(10);
        d1.setId_producto(1);
        d1.setCantidad(1);
        d1.setPrecio_unitario(BigDecimal.valueOf(100.00));
        d1.setSubtotal(BigDecimal.valueOf(100.00));

        when(facturaRepository.findById(1)).thenReturn(Optional.of(factura));
        when(ventaRepository.findById(10)).thenReturn(Optional.of(venta));
        when(detalleVentaRepository.findByIdVenta(10)).thenReturn(List.of(d1));

        // Act
        byte[] pdfBytes = facturaService.generarPdfFactura(1);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        // Extract text from PDF using iText to reliably find labels
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdfBytes)));
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
            sb.append(PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i)));
        }
        pdfDoc.close();
        String pdfText = sb.toString();
        // Now the PDF should show the product lines, IGV and TOTAL only (no summary 'Subtotal' row)
        // PDF must contain Subtotal header and the per-line subtotal amount, but NOT the summary 'Subtotal:' row
        assertTrue(pdfText.contains("Subtotal"), "PDF debe contener la columna 'Subtotal' en la tabla");
        // Some PDF renderers may break lines; check amounts separately
        assertTrue(pdfText.contains("100.00"), "PDF debe contener el monto subtotal de la línea 100.00");
        // The summary 'Subtotal: S/ x' row should no longer appear below the table
        assertFalse(pdfText.contains("Subtotal: S/"), "PDF no debe contener la fila resumen 'Subtotal:' abajo de la tabla");
        assertTrue(pdfText.contains("IGV"), "PDF debe contener 'IGV'");
        assertTrue(pdfText.contains("18.00"), "PDF debe contener el monto IGV 18.00");
        assertTrue(pdfText.contains("TOTAL"), "PDF debe contener 'TOTAL'");
        assertTrue(pdfText.contains("118.00"), "PDF debe contener el monto total 118.00");
    }
}
