package util;

import modelo.Proceso;
import modelo.EstadoProceso;
import java.util.HashMap;
import java.util.Map;

/**
 * ARCHIVO: util/ConvertidorProceso.java
 *
 * Utilidad para convertir entre objetos Proceso y Maps
 * (necesario para XML-RPC que no soporta objetos complejos)
 */
public class ConvertidorProceso {

    /**
     * Convierte un objeto Proceso a un Map compatible con XML-RPC
     */
    public static Map<String, Object> procesoAMap(Proceso proceso) {
        Map<String, Object> map = new HashMap<>();

        map.put(Constantes.KEY_NOMBRE, proceso.getNombre());
        map.put(Constantes.KEY_ESTADO, proceso.getEstado().toString());
        map.put(Constantes.KEY_TIEMPO_CPU, proceso.getTiempoCPU());
        map.put(Constantes.KEY_TIEMPO_ESPERA, proceso.getTiempoEspera());
        map.put(Constantes.KEY_TIEMPO_FINALIZACION, proceso.getTiempoFinalizacion());
        map.put(Constantes.KEY_PENALIZACION, proceso.getPenalizacion());
        map.put(Constantes.KEY_CLIENTE_ID, proceso.getClienteId());

        return map;
    }

    /**
     * Convierte un Map a un objeto Proceso
     * (útil si necesitas reconstruir el objeto en el cliente)
     */
    public static Proceso mapAProceso(Map<String, Object> map) {
        String nombre = (String) map.get(Constantes.KEY_NOMBRE);
        int tiempoCPU = (Integer) map.get(Constantes.KEY_TIEMPO_CPU);

        Proceso proceso = new Proceso(nombre, tiempoCPU, 0);

        // Establecer el estado
        String estadoStr = (String) map.get(Constantes.KEY_ESTADO);
        proceso.setEstado(EstadoProceso.valueOf(estadoStr));

        // Establecer cliente
        String clienteId = (String) map.get(Constantes.KEY_CLIENTE_ID);
        if (clienteId != null) {
            proceso.setClienteId(clienteId);
        }

        return proceso;
    }

    // Constructor privado
    private ConvertidorProceso() {}
}