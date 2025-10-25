package util;

import modelo.EstadoProceso;
import modelo.Proceso;

import java.util.HashMap;
import java.util.Map;

public class ConvertidorProceso {

    public static Map<String, Object> procesoAMap(Proceso p) {
        Map<String, Object> map = new HashMap<>();
        map.put("clienteId", p.getClienteId());
        map.put("nombre", p.getNombre());
        map.put("tiempoCPU", p.getTiempoCPU());
        map.put("estado", p.getEstado().name());
        map.put("tiempoLlegada", p.getTiempoLlegada());
        map.put("tiempoPeticion", p.getTiempoPeticion());
        map.put("tiempoInicio", p.getTiempoInicio());
        map.put("tiempoFin", p.getTiempoFin());

        // Métricas
        map.put("tiempoEspera", p.getTiempoEspera());
        map.put("tiempoFinalizacion", p.getTiempoFinalizacion());
        map.put("penalizacion", p.getPenalizacion());
        return map;
    }

    public static Proceso mapAProceso(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;

        Proceso p = new Proceso(
                (String) map.get("clienteId"),
                (String) map.get("nombre"),
                (int) map.get("tiempoCPU"),
                (int) map.get("tiempoLlegada"),
                (int) map.get("tiempoPeticion")
        );

        p.setEstado(EstadoProceso.valueOf((String) map.get("estado")));
        p.setTiempoInicio((int) map.get("tiempoInicio"));
        p.setTiempoFin((int) map.get("tiempoFin"));
        return p;
    }
}
