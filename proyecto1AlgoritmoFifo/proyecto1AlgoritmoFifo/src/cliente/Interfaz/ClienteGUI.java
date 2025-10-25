package cliente.Interfaz;

import cliente.ClienteFIFO;
import modelo.Proceso;
import servidor.ServidorFIFO;
import util.Constantes;
import util.ConvertidorProceso;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClienteGUI extends JFrame {

    private JTable tablaProcesos;
    private DefaultTableModel modeloProcesos;
    private JButton btnConnect, btnDisconnect, btnAddProcess, btnKillProcess;
    private JLabel statusLabel;

    private ClienteFIFO clienteFIFO;
    private Timer pollingTimer;
    private String clienteId;

    private Map<String, String> estadoLocalProcesos = new HashMap<>();



    public ClienteGUI() {
        setTitle("Cliente Planificador FIFO");
        setSize(1100, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5, 5));
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.clienteId = "Cliente-" + UUID.randomUUID().toString().substring(0, 8);

        add(crearPanelConexion(), BorderLayout.NORTH);
        add(crearPanelTabla(), BorderLayout.CENTER);
        add(crearPanelAcciones(), BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        agregarListeners();
        configurarPollingTimer();
    }

    private JPanel crearPanelConexion() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                "Conexión con el Servidor",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 13)
        ));

        btnConnect = new JButton("🔌 Conectar");
        btnDisconnect = new JButton("🔌 Desconectar");
        btnConnect.setFont(new Font("Arial", Font.BOLD, 12));
        btnDisconnect.setFont(new Font("Arial", Font.BOLD, 12));
        btnDisconnect.setEnabled(false);

        JLabel lblId = new JLabel("Mi ID: " + this.clienteId);
        lblId.setFont(new Font("Monospaced", Font.BOLD, 12));
        lblId.setForeground(Color.decode("#1976D2"));

        panel.add(btnConnect);
        panel.add(btnDisconnect);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(lblId);

        return panel;
    }

    private JScrollPane crearPanelTabla() {
        String[] columnas = {
                "ID Proceso", "Estado", "T. CPU (C)", "T. Petición (t)",
                "T. Llegada", "T. Espera", "T. Finalización", "Penalización"
        };

        modeloProcesos = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        tablaProcesos = new JTable(modeloProcesos);
        tablaProcesos.setRowHeight(28);
        tablaProcesos.setFont(new Font("Arial", Font.PLAIN, 13));
        tablaProcesos.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        tablaProcesos.getTableHeader().setBackground(Color.decode("#E3F2FD"));
        tablaProcesos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaProcesos.setGridColor(Color.LIGHT_GRAY);
        tablaProcesos.setSelectionBackground(Color.decode("#BBDEFB"));

        // Centrar contenido de las columnas
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < tablaProcesos.getColumnCount(); i++) {
            tablaProcesos.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Renderer personalizado para la columna de Estado
        tablaProcesos.getColumnModel().getColumn(1).setCellRenderer(new EstadoRenderer());

        JScrollPane scroll = new JScrollPane(tablaProcesos);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                "Procesos del Cliente",
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 13)
        ));

        return scroll;
    }

    private JPanel crearPanelAcciones() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        panelBotones.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                "Acciones de Proceso",
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 13)
        ));

        btnAddProcess = new JButton("➕ Agregar Nuevo Proceso");
        btnKillProcess = new JButton("❌ Eliminar Proceso Seleccionado");
        btnAddProcess.setFont(new Font("Arial", Font.BOLD, 12));
        btnKillProcess.setFont(new Font("Arial", Font.BOLD, 12));

        panelBotones.add(btnAddProcess);
        panelBotones.add(btnKillProcess);
        panel.add(panelBotones, BorderLayout.NORTH);

        statusLabel = new JLabel("  Estado: Desconectado  ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.decode("#E74C3C"));
        statusLabel.setForeground(Color.WHITE);
        panel.add(statusLabel, BorderLayout.SOUTH);

        return panel;
    }

    private void configurarPollingTimer() {
        pollingTimer = new Timer(2000, e -> sondearServidor());
        pollingTimer.setInitialDelay(0);
    }

    private void sondearServidor() {
        if (clienteFIFO == null || !clienteFIFO.isConectado()) {
            return;
        }

        new Thread(() -> {
            Object[] procesosRemotos = clienteFIFO.obtenerTodosLosProcesos();

            if (procesosRemotos == null) {
                SwingUtilities.invokeLater(() -> {
                    actualizarEstadoConexion("Error de red o servidor desconectado",
                            Color.decode("#FF9800"));
                    btnConnect.setEnabled(true);
                    btnDisconnect.setEnabled(false);
                    pollingTimer.stop();
                    estadoLocalProcesos.clear();
                });
                return;
            }

            for (Object obj : procesosRemotos) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                Proceso p = ConvertidorProceso.mapAProceso(map);

                if (p != null) {
                    agregarOActualizarProceso(
                            p.getNombre(),
                            p.getEstado().name(),
                            String.valueOf(p.getTiempoCPU()),
                            String.valueOf(p.getTiempoPeticion()),
                            (p.getTiempoLlegada() >= 0) ? String.valueOf(p.getTiempoLlegada()) : "N/A",
                            (p.getTiempoEspera() >= 0) ? String.valueOf(p.getTiempoEspera()) : "N/A",
                            (p.getTiempoFinalizacion() > 0) ? String.valueOf(p.getTiempoFinalizacion()) : "N/A",
                            (p.getPenalizacion() > 0) ? String.format("%.2f", p.getPenalizacion()) : "N/A"
                    );
                }
            }
        }).start();
    }

    private void agregarListeners() {

        btnConnect.addActionListener(e -> {
            String url = "http://localhost:" + Constantes.PUERTO_SERVIDOR;
            clienteFIFO = new ClienteFIFO(url, this.clienteId);

            if (clienteFIFO.conectar()) {
                actualizarEstadoConexion("Conectado", Color.decode("#2ECC71"));
                btnConnect.setEnabled(false);
                btnDisconnect.setEnabled(true);
                pollingTimer.start();
            } else {
                actualizarEstadoConexion("Error de Conexión", Color.decode("#FF9800"));
            }
        });

        btnDisconnect.addActionListener(e -> {
            if (clienteFIFO != null) clienteFIFO.desconectar();
            pollingTimer.stop();
            actualizarEstadoConexion("Desconectado", Color.decode("#E74C3C"));
            btnConnect.setEnabled(true);
            btnDisconnect.setEnabled(false);
            modeloProcesos.setRowCount(0);
            estadoLocalProcesos.clear();
        });

        btnAddProcess.addActionListener(e -> mostrarDialogoNuevoProceso());

        btnKillProcess.addActionListener(e -> {
            int fila = tablaProcesos.getSelectedRow();
            if (fila == -1) {
                JOptionPane.showMessageDialog(this,
                        "⚠ Selecciona un proceso de la tabla.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String idProceso = (String) modeloProcesos.getValueAt(fila, 0);

            if (clienteFIFO != null && clienteFIFO.eliminarProceso(idProceso)) {
                System.out.println("Solicitud de eliminación enviada para: " + idProceso);
            } else {
                JOptionPane.showMessageDialog(this,
                        "No se pudo eliminar el proceso\n(quizás ya está en ejecución o finalizado).",
                        "Error", JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    private void mostrarDialogoNuevoProceso() {
        JTextField txtId = new JTextField("P-" + (modeloProcesos.getRowCount() + 1));
        JTextField txtTiempoEjecucion = new JTextField("5");
        JTextField txtTiempoPeticion = new JTextField("0");

        JPanel panelDialog = new JPanel(new GridLayout(0, 2, 10, 10));
        panelDialog.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panelDialog.add(new JLabel("ID del Proceso:"));
        panelDialog.add(txtId);
        panelDialog.add(new JLabel("Tiempo de CPU (C):"));
        panelDialog.add(txtTiempoEjecucion);
        panelDialog.add(new JLabel("Tiempo de Petición (t):"));
        panelDialog.add(txtTiempoPeticion);

        int result = JOptionPane.showConfirmDialog(this, panelDialog,
                "Agregar Nuevo Proceso", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String id = txtId.getText().trim();
            String tEjecStr = txtTiempoEjecucion.getText().trim();
            String tPtcnStr = txtTiempoPeticion.getText().trim();

            try {
                int tEjec = Integer.parseInt(tEjecStr);
                int tPtcn = Integer.parseInt(tPtcnStr);

                if (id.isEmpty() || tEjec <= 0 || tPtcn < 0) {
                    throw new NumberFormatException();
                }

                if (clienteFIFO != null && clienteFIFO.agregarProceso(id, tEjec, tPtcn)) {
                    System.out.println("✓ Proceso agregado: " + id + " (t=" + tPtcn + ")");
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Error del servidor al agregar el proceso\n" +
                                    "(ID duplicado o servidor no disponible).",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Datos inválidos:\n" +
                                "- El ID no puede estar vacío\n" +
                                "- El tiempo de CPU debe ser > 0\n" +
                                "- El tiempo de petición debe ser ≥ 0",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void actualizarEstadoConexion(String texto, Color colorFondo) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("  Estado: " + texto + "  ");
            statusLabel.setBackground(colorFondo);
        });
    }

    public void agregarOActualizarProceso(String id, String estado, String tCPU,
                                          String tPeticion, String tLlegada, String tEspera,
                                          String tFin, String tPenal) {
        String hash = id + estado + tCPU + tPeticion + tLlegada + tEspera + tFin + tPenal;

        if(hash.equals(estadoLocalProcesos.get(id))) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            int fila = buscarFilaPorID(id);

            if (fila == -1) {
                Object[] nuevaFila = {id, estado, tCPU, tPeticion, tLlegada, tEspera, tFin, tPenal};
                modeloProcesos.addRow(nuevaFila);
            } else {
                modeloProcesos.setValueAt(estado, fila, 1);
                modeloProcesos.setValueAt(tCPU, fila, 2);
                modeloProcesos.setValueAt(tPeticion, fila, 3);
                modeloProcesos.setValueAt(tLlegada, fila, 4);
                modeloProcesos.setValueAt(tEspera, fila, 5);
                modeloProcesos.setValueAt(tFin, fila, 6);
                modeloProcesos.setValueAt(tPenal, fila, 7);
            }
            estadoLocalProcesos.put(id, hash);
        });
    }

    private int buscarFilaPorID(String id) {
        for (int i = 0; i < modeloProcesos.getRowCount(); i++) {
            if (id.equals(modeloProcesos.getValueAt(i, 0))) {
                return i;
            }
        }
        return -1;
    }

    // Renderer personalizado para colorear estados
    class EstadoRenderer extends DefaultTableCellRenderer {
        public EstadoRenderer() {
            setHorizontalAlignment(CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);

            if (!isSelected && value != null) {
                String estado = value.toString();
                switch (estado) {
                    case "NUEVO":
                        c.setBackground(Color.decode("#E3F2FD"));
                        c.setForeground(Color.decode("#1565C0"));
                        setFont(getFont().deriveFont(Font.BOLD));
                        break;
                    case "LISTO":
                        c.setBackground(Color.decode("#C8E6C9"));
                        c.setForeground(Color.BLACK);
                        break;
                    case "EJECUCION":
                        c.setBackground(Color.decode("#FFF59D"));
                        c.setForeground(Color.BLACK);
                        setFont(getFont().deriveFont(Font.BOLD));
                        break;
                    case "EN_ESPERA":
                        c.setBackground(Color.decode("#FFCCBC"));
                        c.setForeground(Color.BLACK);
                        break;
                    case "TERMINADO":
                        c.setBackground(Color.decode("#E1BEE7"));
                        c.setForeground(Color.BLACK);
                        break;
                    case "RECHAZADO":
                        c.setBackground(Color.decode("#FFCDD2"));
                        c.setForeground(Color.decode("#C62828"));
                        setFont(getFont().deriveFont(Font.BOLD));
                        break;
                    case "ELIMINADO":
                        c.setBackground(Color.decode("#CFD8DC"));
                        c.setForeground(Color.decode("#455A64"));
                        break;
                    default:
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                }
            }

            return c;
        }
    }
}