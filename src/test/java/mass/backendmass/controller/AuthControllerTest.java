package mass.backendmass.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import mass.backendmass.models.Cliente;
import mass.backendmass.repository.ClienteRepository;
import mass.backendmass.service.ClienteService;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    ClienteRepository clienteRepository;

    @Mock
    ClienteService clienteService;

    @InjectMocks
    AuthController authController;

    @Test
    public void registrar_duplicateEmail_returnsErrorMessage() {
        // Arrange
        Cliente input = new Cliente();
        input.setCorreo("test@example.com");
        when(clienteRepository.findByCorreo("test@example.com")).thenReturn(new Cliente());

        // Act
        ResponseEntity<?> resp = authController.registrar(input);

        // Assert - should return 200 OK with a structured error body (frontend easier to consume)
        assertEquals(200, resp.getStatusCodeValue());
        assertTrue(resp.getBody() instanceof Map);
        Map body = (Map) resp.getBody();
        assertEquals(false, body.get("success"));
        assertTrue(((String) body.get("mensaje")).toLowerCase().contains("correo"));
    }

    @Test
    public void registrar_newEmail_callsServiceAndReturnsCreated() throws Exception {
        // Arrange
        Cliente input = new Cliente();
        input.setCorreo("new@example.com");
        Cliente saved = new Cliente();
        saved.setId_cliente(5);
        saved.setCorreo("new@example.com");

        when(clienteRepository.findByCorreo("new@example.com")).thenReturn(null);
        when(clienteService.registrarCliente(input)).thenReturn(saved);

        // Act
        ResponseEntity<?> resp = authController.registrar(input);

        // Assert
        assertEquals(201, resp.getStatusCodeValue());
        assertTrue(resp.getBody() instanceof Map);
        Map body = (Map) resp.getBody();
        assertEquals(true, body.get("success"));
        assertEquals(5, body.get("id_cliente"));
    }
}
