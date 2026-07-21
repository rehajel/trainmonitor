import java.awt.Color;
import java.awt.Font;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class Main {

    private static final int MAX_ROWS = 5;
    private static javax.swing.JLabel[] timeLabels = new javax.swing.JLabel[MAX_ROWS];
    private static javax.swing.JLabel[] destLabels = new javax.swing.JLabel[MAX_ROWS];
    private static javax.swing.JLabel[] platLabels = new javax.swing.JLabel[MAX_ROWS];
    private static javax.swing.JLabel[] statusLabels = new javax.swing.JLabel[MAX_ROWS];

    private static javax.swing.JLabel clockLabel;

    public static void main(String[] args) {

        String url = "https://fahrplan.oebb.at/bin/mgate.exe";

        HttpClient client = HttpClient.newHttpClient();

        JFrame frame = new JFrame("Train Monitor");
        frame.setSize(550, 320);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); // Center on screen

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.DARK_GRAY);

        JPanel headerPanel = new JPanel(new java.awt.BorderLayout());
        headerPanel.setBackground(Color.DARK_GRAY);
        headerPanel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 80));
        // headerPanel.setPreferredSize(new java.awt.Dimension(Integer.MAX_VALUE, 20));
        headerPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));

        javax.swing.JLabel titleLabel = new javax.swing.JLabel("Abfahrten nach Hatting");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, java.awt.BorderLayout.WEST);

        clockLabel = new javax.swing.JLabel("Stand: --:--");
        clockLabel.setFont(new Font("Arial", Font.ITALIC, 16));
        clockLabel.setForeground(Color.LIGHT_GRAY);
        headerPanel.add(clockLabel, java.awt.BorderLayout.EAST);

        panel.add(headerPanel);

        // Separator Line
        JPanel separator = new JPanel();
        separator.setBackground(new Color(100, 100, 100));
        separator.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 2));
        separator.setPreferredSize(new java.awt.Dimension(Integer.MAX_VALUE, 2));
        panel.add(separator);

        // Space Below Separator
        panel.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 15)));

        // 2. Loop to build our 5 blank rows
        for (int i = 0; i < MAX_ROWS; i++) {
            JPanel row = new JPanel(new java.awt.BorderLayout());
            row.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 45));
            row.setBackground(Color.DARK_GRAY);
            row.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 15, 5, 15));

            // --- 1. INITIALIZE ALL INSTANCES FIRST (Prevents NullPointerException!) ---
            timeLabels[i] = new javax.swing.JLabel("00:00");
            destLabels[i] = new javax.swing.JLabel("Lade Zug...");
            platLabels[i] = new javax.swing.JLabel("-");
            statusLabels[i] = new javax.swing.JLabel("Warten...");

            // --- 2. STYLE THE LABELS ---
            Font rowFont = new Font("Arial", Font.PLAIN, 18);
            timeLabels[i].setFont(new Font("Arial", Font.BOLD, 18));
            timeLabels[i].setForeground(Color.WHITE);
            destLabels[i].setFont(rowFont);
            destLabels[i].setForeground(Color.WHITE);
            platLabels[i].setFont(rowFont);
            platLabels[i].setForeground(Color.LIGHT_GRAY);
            statusLabels[i].setFont(rowFont);
            statusLabels[i].setForeground(Color.GRAY);

            // --- 3. CREATE THE INNER HORIZONTAL LAYOUT ---
            JPanel leftGroup = new JPanel();
            leftGroup.setLayout(new javax.swing.BoxLayout(leftGroup, javax.swing.BoxLayout.X_AXIS));
            leftGroup.setBackground(Color.DARK_GRAY);

            // Now this is completely safe because nothing is null!
            leftGroup.add(timeLabels[i]);
            leftGroup.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(25, 0)));
            leftGroup.add(destLabels[i]);
            leftGroup.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(40, 0)));
            leftGroup.add(platLabels[i]);

            // --- 4. ASSEMBLE THE FINAL OUTER ROW ---
            row.add(leftGroup, java.awt.BorderLayout.WEST);
            row.add(statusLabels[i], java.awt.BorderLayout.EAST);

            panel.add(row);
        }

        panel.add(javax.swing.Box.createVerticalGlue());
        frame.add(panel);

        URL iconURL = Main.class.getResource("/train.png");
        ImageIcon icon = new ImageIcon(iconURL);
        frame.setIconImage(icon.getImage());

        frame.setVisible(true);

        ActionListener refreshAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fetchAndDraw(client, url, panel);
            }
        };

        Timer timer = new Timer(60000, refreshAction);
        refreshAction.actionPerformed(null); // run immediatly
        timer.start();
    }

    private static JSONArray fetchDepartures(HttpClient client, String url) throws IOException, InterruptedException {
        String jsonPayload = Files.readString(Paths.get("request.json"));
        // String jsonPayload = Main.class.getResourceAsStream("/request.json");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        String responseBody = response.body();
        JSONObject root = new JSONObject(responseBody);
        JSONArray svcResL = root.getJSONArray("svcResL");
        JSONObject res = svcResL.getJSONObject(0).getJSONObject("res");

        // jnyL = list of trains
        JSONArray jnyL = res.getJSONArray("jnyL");
        return jnyL;
    }

    private static void updateLabels(JSONArray jnyL, JPanel panel) {
        int trainsToDisplay = Math.min(jnyL.length(), MAX_ROWS);

        // iterate over trains
        for (int i = 0; i < trainsToDisplay; i++) {
            boolean delayed = false;
            JSONObject jny = jnyL.getJSONObject(i);
            // Get Values
            // dirText = Destionation
            String dest = jny.optString("dirTxt", "Unbekannt");
            // dTimeS = Scheduled Time
            String scheduledTimeRaw = jny.getJSONObject("stbStop").getString("dTimeS");
            String scheduledTime = scheduledTimeRaw.substring(0, 2) + ":" + scheduledTimeRaw.substring(2, 4);
            // dTimeR = real Time
            String realTimeRaw = jny.getJSONObject("stbStop").optString("dTimeR");
            // dPltfS = Scheduled Platform
            String scheduledPlatform = jny.getJSONObject("stbStop").getJSONObject("dPltfS").optString("txt",
                    "schedPF");
            // dPltfR = Real Platform
            String realPlatform = scheduledPlatform;
            if (jny.getJSONObject("stbStop").has("dPltfR")) {
                JSONObject realPlatformObj = jny.getJSONObject("stbStop").getJSONObject("dPltfR");
                realPlatform = realPlatformObj.optString("txt", scheduledPlatform);
            }
            // On time?
            String statusText = "Pünktlich";

            if (!realTimeRaw.isEmpty() && !realTimeRaw.equals(scheduledTimeRaw)) {
                String realTime = realTimeRaw.substring(0, 2) + ":" + realTimeRaw.substring(2, 4);
                statusText = "Verspätung! Neu: " + realTime;
                delayed = true;
            }

            // Overwrite the text on the existing labels
            timeLabels[i].setText(scheduledTime);
            destLabels[i].setText(dest);
            platLabels[i].setText(realPlatform);
            statusLabels[i].setText(statusText);

            // Apply conditional colors dynamically
            if (delayed) {
                statusLabels[i].setForeground(Color.RED);
            } else {
                statusLabels[i].setForeground(new Color(50, 205, 50)); // Lime green
            }
        }
        for (int i = trainsToDisplay; i < MAX_ROWS; i++) {
            timeLabels[i].setText("");
            destLabels[i].setText("");
            platLabels[i].setText("");
            statusLabels[i].setText("");
        }
        // panel.add(javax.swing.Box.createVerticalGlue());
        // panel.revalidate(); // Recalculate layout components
        panel.repaint(); // Redraw the screen
        String currentTime = java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        clockLabel.setText("Stand: " + currentTime);
    }

    public static void fetchAndDraw(HttpClient client, String url, JPanel panel) {

        SwingWorker<JSONArray, Void> worker = new SwingWorker<JSONArray, Void>() {

            @Override
            protected JSONArray doInBackground() throws Exception {
                return fetchDepartures(client, url);
            }

            @Override
            protected void done() {
                try {
                    JSONArray jnyL = get();
                    updateLabels(jnyL, panel);
                } catch (InterruptedException e) {
                    clockLabel.setText(e.getMessage());
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof java.nio.file.NoSuchFileException) {
                        clockLabel.setText("Error: request.json is missing" + cause.getMessage());
                    }
                    else if (cause instanceof java.io.IOException) {
                        clockLabel.setText("Error: Could not connect to ÖBB servers" + cause.getMessage());
                    }
                    else if (cause instanceof org.json.JSONException) {
                        clockLabel.setText(
                                "Error: ÖBB changed their data format, or request.json is corrupted." + cause.getMessage());
                    } else {
                        clockLabel.setText("Error: Unknown Error Occured" + cause.getMessage());
                    }

                }
            }
        };

        worker.execute();

    }
}
