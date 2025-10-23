package cliente.Interfaz;

import cliente.ClienteFIFO;
import util.Constantes;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class ClienteGUI extends JFrame {

    private JTable tablaProcesos;
    private DefaultTableModel modeloProcesos;
    private JButton btnConnect, btnDisconnect, btnAddProcess, btnKillProcess, btnRefresh;
    private JLabel statusLabel;

    // ⭐ NUEVO: Cliente XML-RPC
    private ClienteFIFO clienteRPC;
    private String clienteId;

    // ⭐ NUEVO: Hilo para actualización automática
    private Timer timerActualizacion;

    public ClienteGUI() {
        // Generar ID único para este cliente
        clienteId = "Cliente-" + System.currentTimeMillis();

        // Crear el cliente RPC (aún no conectado)
        clienteRPC = new ClienteFIFO(Constantes.SERVIDOR_URL, clienteId);

        setTitle("Cliente Planificador - " + clienteId);
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));

        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Paneles
        add(crearPanelConexion(), BorderLayout.NORTH);
        add(crearPanelTabla(), BorderLayout.CENTER);
        add(crearPanelAcciones(), BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        agregarListeners();
    }

    /**
     * Panel de conexión
     */
    private JPanel crearPanelConexion() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Conexión con el Servidor"));

        // Mostrar URL del servidor
        JLabel lblServidor = new JLabel("Servidor: " + Constantes.SERVIDOR_URL);
        lblServidor.setFont(new Font("Arial", Font.PLAIN, 12));

        btnConnect = new JButton("Conectar");
        btnDisconnect = new JButton("Desconectar");
        btnDisconnect.setEnabled(false);

        panel.add(lblServidor);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(btnConnect);
        panel.add(btnDisconnect);

        return panel;
    }

    /**
     * Panel con la tabla de procesos
     */
    private JScrollPane crearPanelTabla() {
        String[] columnas = {
                "ID Proceso", "Estado", "T. CPU (t)",
                "T. Espera (E)", "T. Finalización (F)", "Penalización (P)"
        };

        modeloProcesos = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaProcesos = new JTable(modeloProcesos);
        tablaProcesos.setRowHeight(25);
        tablaProcesos.setFont(new Font("Arial", Font.PLAIN, 14));
        tablaProcesos.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        tablaProcesos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        return new JScrollPane(tablaProcesos);
    }

    /**
     * Panel de acciones
     */
    private JPanel crearPanelAcciones() {
        JPanel panel = new JPanel(new BorderLayout());

        // Sub-panel para botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelBotones.setBorder(BorderFactory.createTitledBorder("Acciones de Proceso"));

        btnAddProcess = new JButton("Agregar Nuevo Proceso");
        btnKillProcess = new JButton("Eliminar Proceso Seleccionado");
        btnRefresh = new JButton("Actualizar Estado");

        // Inicialmente deshabilitados
        btnAddProcess.setEnabled(false);
        btnKillProcess.setEnabled(false);
        btnRefresh.setEnabled(false);

        panelBotones.add(btnAddProcess);
        panelBotones.add(btnKillProcess);
        panelBotones.add(btnRefresh);

        panel.add(panelBotones, BorderLayout.NORTH);

        // Barra de estado
        statusLabel = new JLabel("Estado: Desconectado", SwingConstants.LEFT);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statusLabel.setBorder(BorderFactory.createEtchedBorder());
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.decode("#E74C3C"));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                statusLabel.getBorder(),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        panel.add(statusLabel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Agregar listeners a los botones
     */
    private void agregarListeners() {

        // ============ CONECTAR ============
        btnConnect.addActionListener(e -> conectarAlServidor());

        // ============ DESCONECTAR ============
        btnDisconnect.addActionListener(e -> desconectarDelServidor());

        // ============ AGREGAR PROCESO ============
        btnAddProcess.addActionListener(e -> mostrarDialogoNuevoProceso());

        // ============ ELIMINAR PROCESO ============
        btnKillProcess.addActionListener(e -> eliminarProcesoSeleccionado());

        // ============ ACTUALIZAR (REFRESH) ============
        btnRefresh.addActionListener(e -> actualizarTablaProcesos());
    }

    // ═══════════════════════════════════════════════════════════
    // MÉTODOS DE CONEXIÓN
    // ═══════════════════════════════════════════════════════════

    /**
     * Conectar al servidor
     */
    private void conectarAlServidor() {
        btnConnect.setEnabled(false);
        actualizarEstadoConexion("Conectando...", Color.ORANGE);

        // Ejecutar la conexión en un hilo separado para no bloquear la GUI
        new Thread(() -> {
            boolean conectado = clienteRPC.conectar();

            SwingUtilities.invokeLater(() -> {
                if (conectado) {
                    actualizarEstadoConexion("Conectado", Color.decode("#2ECC71"));
                    btnConnect.setEnabled(false);
                    btnDisconnect.setEnabled(true);
                    btnAddProcess.setEnabled(true);
                    btnKillProcess.setEnabled(true);
                    btnRefresh.setEnabled(true);

                    // Iniciar actualización automática cada segundo
                    iniciarActualizacionAutomatica();

                    JOptionPane.showMessageDialog(this,
                            "Conectado exitosamente al servidor",
                            "Conexión Exitosa",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    actualizarEstadoConexion("Error de Conexión", Color.decode("#E74C3C"));
                    btnConnect.setEnabled(true);

                    JOptionPane.showMessageDialog(this,
                            "No se pudo conectar al servidor.\n" +
                                    "Verifica que el servidor esté ejecutándose en:\n" +
                                    Constantes.SERVIDOR_URL,
                            "Error de Conexión",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }

    /**
     * Desconectar del servidor
     */
    private void desconectarDelServidor() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Estás seguro de que quieres desconectar?\n" +
                        "Todos los procesos en cola serán eliminados.",
                "Confirmar Desconexión",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // Detener la actualización automática
            detenerActualizacionAutomatica();

            new Thread(() -> {
                boolean desconectado = clienteRPC.desconectar();

                SwingUtilities.invokeLater(() -> {
                    actualizarEstadoConexion("Desconectado", Color.decode("#E74C3C"));
                    btnConnect.setEnabled(true);
                    btnDisconnect.setEnabled(false);
                    btnAddProcess.setEnabled(false);
                    btnKillProcess.setEnabled(false);
                    btnRefresh.setEnabled(false);

                    // Limpiar la tabla
                    modeloProcesos.setRowCount(0);

                    if (desconectado) {
                        JOptionPane.showMessageDialog(this,
                                "Desconectado exitosamente",
                                "Desconexión",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                });
            }).start();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // MÉTODOS DE PROCESOS
    // ═══════════════════════════════════════════════════════════

    /**
     * Mostrar diálogo para agregar nuevo proceso
     */
    private void mostrarDialogoNuevoProceso() {
        JTextField txtNombre = new JTextField(15);
        JTextField txtTiempoCPU = new JTextField(5);

        JPanel panelDialog = new JPanel(new GridLayout(0, 2, 5, 5));
        panelDialog.add(new JLabel("Nombre del Proceso:"));
        panelDialog.add(txtNombre);
        panelDialog.add(new JLabel("Tiempo de CPU (t):"));
        panelDialog.add(txtTiempoCPU);

        int result = JOptionPane.showConfirmDialog(this, panelDialog,
                "Agregar Nuevo Proceso", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String nombre = txtNombre.getText().trim();
            String tiempoCPUStr = txtTiempoCPU.getText().trim();

            // Validaciones
            if (nombre.isEmpty() || tiempoCPUStr.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Ambos campos son obligatorios.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int tiempoCPU = Integer.parseInt(tiempoCPUStr);

                if (tiempoCPU <= 0) {
                    JOptionPane.showMessageDialog(this,
                            "El tiempo de CPU debe ser mayor a 0.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Agregar el proceso en un hilo separado
                agregarProcesoAlServidor(nombre, tiempoCPU);

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                        "El tiempo de CPU debe ser un número válido.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Agregar proceso al servidor
     */
    private void agregarProcesoAlServidor(String nombre, int tiempoCPU) {
        new Thread(() -> {
            boolean agregado = clienteRPC.agregarProceso(nombre, tiempoCPU);

            SwingUtilities.invokeLater(() -> {
                if (agregado) {
                    JOptionPane.showMessageDialog(this,
                            "Proceso '" + nombre + "' agregado exitosamente",
                            "Éxito",
                            JOptionPane.INFORMATION_MESSAGE);

                    // Actualizar inmediatamente la tabla
                    actualizarTablaProcesos();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Error al agregar el proceso.\n" +
                                    "Puede que ya exista un proceso con ese nombre.",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }

    /**
     * Eliminar proceso seleccionado
     */
    private void eliminarProcesoSeleccionado() {
        int filaSeleccionada = tablaProcesos.getSelectedRow();

        if (filaSeleccionada == -1) {
            JOptionPane.showMessageDialog(this,
                    "Por favor, selecciona un proceso de la tabla para eliminar.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String nombreProceso = (String) modeloProcesos.getValueAt(filaSeleccionada, 0);
        String estado = (String) modeloProcesos.getValueAt(filaSeleccionada, 1);

        // Advertencia si está en ejecución
        if (estado.equals("EJECUTANDO")) {
            JOptionPane.showMessageDialog(this,
                    "No se puede eliminar un proceso en ejecución.",
                    "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Estás seguro de que quieres eliminar el proceso '" + nombreProceso + "'?",
                "Confirmar Eliminación", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                boolean eliminado = clienteRPC.eliminarProceso(nombreProceso);

                SwingUtilities.invokeLater(() -> {
                    if (eliminado) {
                        JOptionPane.showMessageDialog(this,
                                "Proceso eliminado exitosamente",
                                "Éxito",
                                JOptionPane.INFORMATION_MESSAGE);

                        // Actualizar la tabla
                        actualizarTablaProcesos();
                    } else {
                        JOptionPane.showMessageDialog(this,
                                "No se pudo eliminar el proceso.\n" +
                                        "Puede estar en ejecución o ya completado.",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
            }).start();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // MÉTODOS DE ACTUALIZACIÓN
    // ═══════════════════════════════════════════════════════════

    /**
     * Actualizar la tabla con los procesos del servidor
     */
    private void actualizarTablaProcesos() {
        if (!clienteRPC.isConectado()) {
            return;
        }

        new Thread(() -> {
            try {
                Object[] procesos = clienteRPC.obtenerTodosLosProcesos();

                SwingUtilities.invokeLater(() -> {
                    // Limpiar la tabla
                    modeloProcesos.setRowCount(0);

                    // Agregar cada proceso
                    for (Object obj : procesos) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> proceso = (Map<String, Object>) obj;

                        String nombre = (String) proceso.get(Constantes.KEY_NOMBRE);
                        String estado = (String) proceso.get(Constantes.KEY_ESTADO);
                        int tiempoCPU = (Integer) proceso.get(Constantes.KEY_TIEMPO_CPU);
                        int tiempoEspera = (Integer) proceso.get(Constantes.KEY_TIEMPO_ESPERA);
                        int tiempoFin = (Integer) proceso.get(Constantes.KEY_TIEMPO_FINALIZACION);
                        double penalizacion = (Double) proceso.get(Constantes.KEY_PENALIZACION);

                        // Formatear los valores para mostrar
                        String tiempoFinStr = (tiempoFin >= 0) ? String.valueOf(tiempoFin) : "N/A";
                        String penalizacionStr = (penalizacion > 0) ? String.format("%.2f", penalizacion) : "N/A";

                        Object[] fila = {
                                nombre,
                                estado,
                                tiempoCPU,
                                tiempoEspera,
                                tiempoFinStr,
                                penalizacionStr
                        };

                        modeloProcesos.addRow(fila);
                    }
                });

            } catch (Exception e) {
                System.err.println("Error al actualizar tabla: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Iniciar actualización automática cada segundo
     */
    private void iniciarActualizacionAutomatica() {
        timerActualizacion = new Timer(Constantes.INTERVALO_ACTUALIZACION, e -> {
            actualizarTablaProcesos();
        });
        timerActualizacion.start();
        System.out.println("✓ Actualización automática iniciada");
    }

    /**
     * Detener actualización automática
     */
    private void detenerActualizacionAutomatica() {
        if (timerActualizacion != null && timerActualizacion.isRunning()) {
            timerActualizacion.stop();
            System.out.println("✓ Actualización automática detenida");
        }
    }

    /**
     * Actualizar la barra de estado (Thread-Safe)
     */
    public void actualizarEstadoConexion(String texto, Color colorFondo) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Estado: " + texto);
            statusLabel.setBackground(colorFondo);
        });
    }

    // ═══════════════════════════════════════════════════════════
    // MAIN - PUNTO DE ENTRADA
    // ═══════════════════════════════════════════════════════════

    public static void main(String[] args) {
        // Configurar Look and Feel del sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            ClienteGUI cliente = new ClienteGUI();
            cliente.setVisible(true);
        });
    }
}