package servidor.interfaz;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class procesos extends JFrame {
    private DefaultTableModel modelo;
    private DefaultTableModel modeloProcesos;
    private final Map<String, Color> coloresTareas;
    private final int rangoPlanificador;

    // --- NUEVAS VARIABLES PARA SIMULACIÓN ---
    private Timer simuladorClock;
    private int tiempoSimulacionActual = 0;
    private final List<TareaEnEjecucion> tareasPendientes = new ArrayList<>();
    private final Random rand = new Random();

    //--- VENTANA CON LAS TABLAS ---
    private tirmpos panelTiempos;
    private ColaVentana panelColas;

    /**
     * Clase interna para guardar los datos de una tarea pendiente
     */
    private static class TareaEnEjecucion {
        String nombre;
        int inicio;
        int fin;
        int fila;
        int tiempoLlegada;

        TareaEnEjecucion(String nombre, int inicio, int fin, int fila,  int tiempoLlegada) {
            this.nombre = nombre;
            this.inicio = inicio;
            this.fin = fin;
            this.fila = fila;
            this.tiempoLlegada = tiempoLlegada;
        }
    }
    // --- FIN DE NUEVAS VARIABLES ---

    public procesos(int rangoPlanificador) {
        this.rangoPlanificador = rangoPlanificador;
        setTitle("Proyecto 1 - Rango: " + rangoPlanificador); // Título dinámico
        setSize(1200, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Correcto
        setLayout(new BorderLayout(10, 10));

        coloresTareas = new HashMap<>();

        // Panel superior
        add(crearPanelSuperior(), BorderLayout.NORTH);

        // Panel izquierdo - Tabla de procesos
        JPanel panelIzquierdo = crearPanelProcesos();
        add(panelIzquierdo, BorderLayout.WEST);

        // Panel central - Gantt
        JPanel panelCentral = crearPanelGantt();
        add(panelCentral, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        // setVisible(true); // Se quita de aquí, se hace visible en InterfazServidor
    }

    /**
     * Asigna la ventana de estadísticas a la que esta ventana debe reportar.
     */
    public void setPanelTiempos(tirmpos panel) {
        this.panelTiempos = panel;
    }

    public void setPanelColas(ColaVentana panelColas) {
        this.panelColas = panelColas;
    }



    private JPanel crearPanelSuperior() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.decode("#E8F4F8"));

        JLabel titulo = new JLabel("DIAGRAMA DE GANTT - GESTIÓN DE PROYECTOS");
        titulo.setFont(new Font("Arial", Font.BOLD, 18));
        titulo.setForeground(Color.decode("#2C3E50"));
        panel.add(titulo);

        return panel;
    }

    private JPanel crearPanelGantt() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Procesos x Tiempo"));

        // ... (Toda la configuración de la tabla y el modelo sigue igual) ...
        String[] columnas = new String[rangoPlanificador + 1];
        columnas[0] = "Tarea";
        for (int i = 1; i <= rangoPlanificador; i++) {
            columnas[i] = String.valueOf(i);
        }
        modelo = new DefaultTableModel(columnas, 0) { /* ... isCellEditable ... */ };
        JTable tabla = new JTable(modelo);
        // ... (configuración de tabla, anchos, renderer ...)
        tabla.setRowHeight(35);
        tabla.setGridColor(Color.LIGHT_GRAY);
        tabla.setFont(new Font("Arial", Font.PLAIN, 12));
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(120);
        for (int i = 1; i <= rangoPlanificador; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(35);
        }
        procesos.GanttRenderer renderer = new procesos.GanttRenderer();
        for (int i = 1; i < tabla.getColumnCount(); i++) {
            tabla.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }


        JScrollPane scrollPane = new JScrollPane(tabla);
        panel.add(scrollPane, BorderLayout.CENTER);

        // --- Panel de Botones (MODIFICADO) ---
        JPanel panelBotones = new JPanel(new FlowLayout()); // Usamos FlowLayout

        JButton btnIniciar = new JButton("Iniciar/Reiniciar Simulación");
        btnIniciar.addActionListener(e -> iniciarSimulacion());
        panelBotones.add(btnIniciar);

        // --- ¡BOTÓN NUEVO! ---
        JButton btnAgregar = new JButton("Agregar Tarea Aleatoria");
        btnAgregar.addActionListener(e -> agregarTareaAleatoria());
        panelBotones.add(btnAgregar);
        // --- FIN DE BOTÓN NUEVO ---

        panel.add(panelBotones, BorderLayout.SOUTH);

        return  panel;
    }


    private JPanel crearPanelProcesos() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Datos de Procesos"));
        panel.setPreferredSize(new Dimension(250, 0));

        String[] columnas = {"Proceso", "C", "t"};
        modeloProcesos = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // No editable
            }
        };

        // --- MODIFICADO ---

        JTable tablaProcesos = new JTable(modeloProcesos);
        tablaProcesos.setRowHeight(30);
        tablaProcesos.getColumnModel().getColumn(0).setPreferredWidth(80);
        tablaProcesos.getColumnModel().getColumn(1).setPreferredWidth(50);
        tablaProcesos.getColumnModel().getColumn(2).setPreferredWidth(50);

        // Centrar contenido (Correcto)
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < tablaProcesos.getColumnCount(); i++) {
            tablaProcesos.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scroll = new JScrollPane(tablaProcesos);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Limpia solo los bloques de ejecución '█' de la tabla para reiniciar la simulación.
     */
    private void limpiarEjecucion() {
        for (int r = 0; r < modelo.getRowCount(); r++) {
            for (int c = 1; c <= rangoPlanificador; c++) {
                if (String.valueOf(modelo.getValueAt(r, c)).equals("█")) {
                    modelo.setValueAt("", r, c);
                }
            }
        }
    }

    /**
     * Inicia el Timer (reloj) de la simulación.
     */
    private void iniciarSimulacion() {
        if (simuladorClock != null && simuladorClock.isRunning()) {
            simuladorClock.stop();
        }
        limpiarEjecucion();
        tiempoSimulacionActual = 0;
        int delay = 300;

        ActionListener accionDeReloj = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tiempoSimulacionActual++;

                if (tiempoSimulacionActual > rangoPlanificador) {
                    ((Timer) e.getSource()).stop();
                    System.out.println("Simulación terminada.");
                    return;
                }

                for (TareaEnEjecucion tarea : tareasPendientes) {

                    // --- LÓGICA DE MOVIMIENTO DE COLAS (MODIFICADA) ---
                    if (panelColas != null) {

                        // Si el tiempo es IGUAL a la llegada, mover a "Listo"
                        if (tiempoSimulacionActual == tarea.tiempoLlegada) {
                            panelColas.moverA_Listo(tarea.nombre);
                        }
                        // Si el tiempo es IGUAL al inicio, mover a "Ejecución"
                        else if (tiempoSimulacionActual == tarea.inicio) {
                            panelColas.moverA_Ejecucion(tarea.nombre);
                        }
                        // Si el tiempo es IGUAL al fin, mover a "Terminado"
                        else if (tiempoSimulacionActual == tarea.fin) {
                            panelColas.moverA_Terminado(tarea.nombre);
                        }
                    }
                    // --- FIN ---

                    // Pintar el Gantt (lógica existente, no cambia)
                    if (tiempoSimulacionActual >= tarea.inicio && tiempoSimulacionActual <= tarea.fin) {
                        modelo.setValueAt("█", tarea.fila, tiempoSimulacionActual);
                    }
                }
            }
        };
        simuladorClock = new Timer(delay, accionDeReloj);
        simuladorClock.start();
    }


    // --- RENDERER (MODIFICADO) ---
    // Tu clase interna, ahora sabe cómo dibujar "L"
    class GanttRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);

            // Accede directamente a los campos de la clase externa
            String nombreTarea = (String) modelo.getValueAt(row, 0);
            Color colorTarea = coloresTareas.get(nombreTarea);

            String sValue = (value == null) ? "" : value.toString();

            if (sValue.equals("█")) {
                // Bloque de ejecución
                c.setBackground(colorTarea != null ? colorTarea : Color.GRAY);
                c.setForeground(colorTarea != null ? colorTarea : Color.GRAY);
                setText("");
            } else if (sValue.equals("L")) {
                // Marca de Llegada
                c.setBackground(Color.WHITE);
                c.setForeground(Color.BLACK);
                setText(" L"); // Mostrar 'L'
                setHorizontalAlignment(CENTER);
            } else {
                // Celda vacía
                c.setBackground(Color.WHITE);
                c.setForeground(Color.BLACK);
                setText("");
            }

            if (isSelected) {
                c.setBackground(Color.LIGHT_GRAY);
            }

            return c;
        }
    }

    private void agregarTareaAleatoria() {
        if (panelTiempos == null) {
            JOptionPane.showMessageDialog(this, "Error: No hay un panel de estadísticas asignado.");
            return;
        }
        // 1. Generar datos aleatorios
        String nombre = "P-Rand-" + (modelo.getRowCount() + 1);

        // Llegada en la primera mitad del rango
        int tiempoLlegada = rand.nextInt(rangoPlanificador / 2) + 1;

        // Inicio 1-3 ticks después de llegar
        int inicio = tiempoLlegada + rand.nextInt(3) + 1;
        if (inicio >= rangoPlanificador) inicio = rangoPlanificador - 2; // Evitar desborde

        // Fin 2-8 ticks después de iniciar
        int fin = inicio + rand.nextInt(7) + 2;
        if (fin > rangoPlanificador) fin = rangoPlanificador; // Capar al final del rango
        if (inicio >= fin) inicio = fin - 1; // Asegurar fin > inicio

        // 2. Llamar al método que ya existe
        registrarTareaCompleta(nombre, tiempoLlegada, inicio, fin);
    }

    public void registrarTareaCompleta(String nombre, int tiempoLlegada, int inicio, int fin) {
        // ... (cálculo de color, filas, métricas, etc., no cambia) ...
        Color color = new Color(rand.nextInt(200) + 55, rand.nextInt(200) + 55, rand.nextInt(200) + 55);
        coloresTareas.put(nombre, color);
        Object[] filaGantt = new Object[rangoPlanificador + 1];
        filaGantt[0] = nombre;
        for (int i = 1; i <= rangoPlanificador; i++) filaGantt[i] = "";
        if (tiempoLlegada > 0 && tiempoLlegada <= rangoPlanificador) filaGantt[tiempoLlegada] = "L";
        int duracion = (fin - inicio) + 1;
        Object[] filaProceso = new Object[]{nombre, duracion, tiempoLlegada};
        int tiempoFinalizacion = fin;
        int tiempoEspera = inicio - tiempoLlegada;
        double tiempoRetorno = (double) fin - tiempoLlegada;
        double tiempoServicio = (double) duracion;
        if (tiempoServicio <= 0) tiempoServicio = 1;
        double penalizacion = tiempoRetorno / tiempoServicio;

        SwingUtilities.invokeLater(() -> {
            // ... (Añadir a modelo, modeloProcesos y panelTiempos no cambia) ...
            modelo.addRow(filaGantt);
            modeloProcesos.addRow(filaProceso);
            if (panelTiempos != null) {
                panelTiempos.agregarProceso(nombre);
                panelTiempos.actualizarTiempoFinalizacion(nombre, tiempoFinalizacion);
                panelTiempos.actualizarTiempoEspera(nombre, tiempoEspera);
                panelTiempos.actualizarPenalizacion(nombre, String.format("%.2f", penalizacion));
                panelTiempos.calcularYActualizarMedias();
            }

            // --- CAMBIO: AÑADIR A LA COLA DE "NUEVO" ---
            if (panelColas != null) {
                panelColas.agregarNuevo(nombre); // <-- Ya no va a "Listos"
            }
            // --- FIN ---

            int filaIndex = modelo.getRowCount() - 1;
            // --- CAMBIO: PASAR tiempoLlegada AL CONSTRUCTOR ---
            tareasPendientes.add(new TareaEnEjecucion(nombre, inicio, fin, filaIndex, tiempoLlegada));
        });
    }

}