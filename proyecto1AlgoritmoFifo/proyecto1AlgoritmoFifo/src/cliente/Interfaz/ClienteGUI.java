package cliente.Interfaz;

import cliente.ClienteFIFO;
import modelo.Proceso;
import util.Constantes;
import util.ConvertidorProceso;

import javax.swing.*;
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

    // Usamos un HashMap para no repintar filas que no han cambiado
    private Map<String, String> estadoLocalProcesos = new HashMap<>();

    public ClienteGUI() {
        setTitle("Cliente Planificador");
        setSize(900, 600);
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
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Conexión con el Servidor"));

        btnConnect = new JButton("Conectar");
        btnDisconnect = new JButton("Desconectar");
        btnDisconnect.setEnabled(false);

        panel.add(btnConnect);
        panel.add(btnDisconnect);
        panel.add(new JLabel("Mi ID: " + this.clienteId));
        return panel;
    }

    private JScrollPane crearPanelTabla() {
        String[] columnas = {
                "ID Proceso", "Estado", "T. Ejecución (C)",
                "T. Espera", "T. Finalización", "T. Penalización"
        };

        modeloProcesos = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        tablaProcesos = new JTable(modeloProcesos);
        tablaProcesos.setRowHeight(25);
        tablaProcesos.setFont(new Font("Arial", Font.PLAIN, 14));
        tablaProcesos.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        tablaProcesos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        return new JScrollPane(tablaProcesos);
    }

    private JPanel crearPanelAcciones() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelBotones.setBorder(BorderFactory.createTitledBorder("Acciones de Proceso"));

        btnAddProcess = new JButton("Agregar Nuevo Proceso");
        btnKillProcess = new JButton("Eliminar Proceso Seleccionado");

        panelBotones.add(btnAddProcess);
        panelBotones.add(btnKillProcess);
        panel.add(panelBotones, BorderLayout.NORTH);

        statusLabel = new JLabel("Estado: Desconectado", SwingConstants.LEFT);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statusLabel.setBorder(BorderFactory.createEtchedBorder());
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

        // Ejecutar en un hilo separado para no congelar la GUI si la red es lenta
        new Thread(() -> {
            Object[] procesosRemotos = clienteFIFO.obtenerTodosLosProcesos();

            // Si es null, la conexión se perdió
            if (procesosRemotos == null) {
                SwingUtilities.invokeLater(() -> {
                    actualizarEstadoConexion("Error de red o servidor desconectado", Color.ORANGE);
                    btnConnect.setEnabled(true);
                    btnDisconnect.setEnabled(false);
                    pollingTimer.stop();
                    estadoLocalProcesos.clear();
                });
                return;
            }

            // Actualizar la tabla con los datos frescos
            for (Object obj : procesosRemotos) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                Proceso p = ConvertidorProceso.mapAProceso(map);

                if (p != null) {
                    agregarOActualizarProceso(
                            p.getNombre(),
                            p.getEstado().name(),
                            String.valueOf(p.getTiempoCPU()),
                            String.valueOf(p.getTiempoEspera()),
                            String.valueOf(p.getTiempoPeticion()),
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
                actualizarEstadoConexion("Error de Conexión", Color.ORANGE);
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
                JOptionPane.showMessageDialog(this, "Selecciona un proceso de la tabla.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String idProceso = (String) modeloProcesos.getValueAt(fila, 0);

            if (clienteFIFO != null && clienteFIFO.eliminarProceso(idProceso)) {
                System.out.println("Solicitud de eliminación enviada para: " + idProceso);
                agregarOActualizarProceso(idProceso, "ELIMINADO", "-","-", "-", "-", "-");
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo eliminar el proceso (quizás ya está en ejecución o finalizado).", "Error", JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    private void mostrarDialogoNuevoProceso() {
        JTextField txtId = new JTextField("P-" + (modeloProcesos.getRowCount() + 1));
        JTextField txtTiempoEjecucion = new JTextField(5);
        JTextField txtTiempoPeticion = new JTextField(5);
        JPanel panelDialog = new JPanel(new GridLayout(0, 2, 5, 5));
        panelDialog.add(new JLabel("ID del Proceso:"));
        panelDialog.add(txtId);
        panelDialog.add(new JLabel("Tiempo de Ejecución (C):"));
        panelDialog.add(txtTiempoEjecucion);
        panelDialog.add(new JLabel("Tiempo de Peticion (t):"));
        panelDialog.add(txtTiempoPeticion);


        int result = JOptionPane.showConfirmDialog(this, panelDialog, "Agregar Nuevo Proceso", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String id = txtId.getText().trim();
            String tEjecStr = txtTiempoEjecucion.getText().trim();
            String tPtcnStr = txtTiempoPeticion.getText().trim();

            try {
                int tEjec = Integer.parseInt(tEjecStr);
                int tPtcn = Integer.parseInt(tPtcnStr);
                if (id.isEmpty() || tEjec <= 0 || tPtcn<=0) throw new NumberFormatException();

                if (clienteFIFO != null && clienteFIFO.agregarProceso(id, tEjec, tPtcn)) {
                    agregarOActualizarProceso(id, "CREADO", tEjecStr, tPtcnStr, "0", "N/A", "N/A");
                } else {
                    JOptionPane.showMessageDialog(this, "Error del servidor al agregar el proceso (quizás el ID está duplicado o excede la capacidad).", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Datos inválidos. El ID no puede estar vacío y el tiempo debe ser un número positivo.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void actualizarEstadoConexion(String texto, Color colorFondo) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Estado: " + texto);
            statusLabel.setBackground(colorFondo);
        });
    }

    public void agregarOActualizarProceso(String id, String estado, String tEjec,String tPeticion, String tEspera, String tFin, String tPenal) {
        String hash = id+estado+tEjec+tPeticion+tEspera+tFin+tPenal;

        // Evitar repintar si no hay cambios
        if(hash.equals(estadoLocalProcesos.get(id))) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            int fila = buscarFilaPorID(id);

            if (fila == -1) {
                Object[] nuevaFila = {id, estado, tEjec, tEspera, tFin, tPenal};
                modeloProcesos.addRow(nuevaFila);
            } else {
                modeloProcesos.setValueAt(estado, fila, 1);
                modeloProcesos.setValueAt(tEjec, fila, 2);
                modeloProcesos.setValueAt(tEspera, fila, 3);
                modeloProcesos.setValueAt(tFin, fila, 4);
                modeloProcesos.setValueAt(tPenal, fila, 5);
            }
            estadoLocalProcesos.put(id, hash); // Actualizar estado local
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
}
