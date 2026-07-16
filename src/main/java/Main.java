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
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Main {

    private static final int MAX_ROWS = 5;
    private static javax.swing.JLabel[] timeLabels = new javax.swing.JLabel[MAX_ROWS];
    private static javax.swing.JLabel[] destLabels = new javax.swing.JLabel[MAX_ROWS];
    private static javax.swing.JLabel[] platLabels = new javax.swing.JLabel[MAX_ROWS];
    private static javax.swing.JLabel[] statusLabels = new javax.swing.JLabel[MAX_ROWS];

    public static void main(String[] args) {

        String url = "https://fahrplan.oebb.at/bin/mgate.exe";

        HttpClient client = HttpClient.newHttpClient();

        JFrame frame = new JFrame("Train Monitor");
        frame.setSize(820, 520);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null); // Center on screen

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.DARK_GRAY);
        frame.add(panel);
        // 1. Add your Header Title once (since it never needs to be recreated!)
        JPanel headerPanel = new JPanel(new java.awt.BorderLayout());
        headerPanel.setBackground(Color.DARK_GRAY);
        headerPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));

        javax.swing.JLabel titleLabel = new javax.swing.JLabel("Abfahrten nach Hatting");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, java.awt.BorderLayout.WEST);

        // We will make a dedicated top-level field for the clock later,
        // for now let's just add the header background structure.
        panel.add(headerPanel);

        // Optional: Separator Line
        JPanel separator = new JPanel();
        separator.setBackground(new Color(100, 100, 100));
        separator.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 2));
        panel.add(separator);
        panel.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 15)));

        // 2. Loop to build our 5 blank rows
        for (int i = 0; i < MAX_ROWS; i++) {
            // Create a horizontal grid row split into 4 columns
            JPanel row = new JPanel(new java.awt.GridLayout(1, 4));
            row.setMaximumSize(new java.awt.Dimension(800, 50));
            row.setBackground(Color.DARK_GRAY);
            row.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 15, 5, 15));

            // Initialize the actual labels into our top-level arrays
            timeLabels[i] = new javax.swing.JLabel("00:00");
            destLabels[i] = new javax.swing.JLabel("Destination");
            platLabels[i] = new javax.swing.JLabel("Platform");
            statusLabels[i] = new javax.swing.JLabel("Status");

            // Style the fonts and colors once
            Font rowFont = new Font("Arial", Font.PLAIN, 18);
            timeLabels[i].setFont(new Font("Arial", Font.BOLD, 18));
            timeLabels[i].setForeground(Color.WHITE);
            destLabels[i].setFont(rowFont);
            destLabels[i].setForeground(Color.WHITE);
            platLabels[i].setFont(rowFont);
            platLabels[i].setForeground(Color.LIGHT_GRAY);
            statusLabels[i].setFont(rowFont);
            statusLabels[i].setForeground(Color.GRAY);

            // Place labels into the row panel
            row.add(timeLabels[i]);
            row.add(destLabels[i]);
            row.add(platLabels[i]);
            row.add(statusLabels[i]);

            // Add this row panel to our main window panel
            panel.add(row);
        }

        // 3. Add Vertical Glue at the very bottom to hold them tight against the top
        panel.add(javax.swing.Box.createVerticalGlue());

        frame.setVisible(true);

        URL iconURL = Main.class.getResource("/train.png");
        ImageIcon icon = new ImageIcon(iconURL);
        frame.setIconImage(icon.getImage());

        ActionListener refreshAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fetchAndDraw(client, url, frame, panel);
            }
        };

        Timer timer = new Timer(60000, refreshAction);
        refreshAction.actionPerformed(null); // run immediatly
        timer.start();
    }

    public static void fetchAndDraw(HttpClient client, String url, JFrame frame, JPanel panel) {
        try {

            String jsonPayload = Files.readString(Paths.get("request.json"));
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

            // --- BUILD THE HEADER PANEL ---
            JPanel headerPanel = new JPanel(new java.awt.BorderLayout());
            headerPanel.setBackground(Color.DARK_GRAY);
            // Add some spacing around the header (Top, Left, Bottom, Right padding)
            headerPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 15, 15, 15));

            // 1. Main Title Label
            javax.swing.JLabel titleLabel = new javax.swing.JLabel("Abfahrten nach Hatting");
            titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
            titleLabel.setForeground(Color.WHITE);
            headerPanel.add(titleLabel, java.awt.BorderLayout.WEST); // Align to the left

            // 2. Live "Last Updated" Clock
            String currentTime = java.time.LocalTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            javax.swing.JLabel clockLabel = new javax.swing.JLabel("Stand: " + currentTime);
            clockLabel.setFont(new Font("Arial", Font.ITALIC, 16));
            clockLabel.setForeground(Color.LIGHT_GRAY);
            headerPanel.add(clockLabel, java.awt.BorderLayout.EAST); // Align to the right

            // Add the complete header to the main panel first
            panel.add(headerPanel);

            // Optional: Add a stylized separator line under the header
            JPanel separator = new JPanel();
            separator.setBackground(new Color(100, 100, 100)); // Medium gray line
            separator.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 2)); // 2 pixels tall, stretches wide
            panel.add(separator);

            // Labels
            JPanel columnsRow = new JPanel(new java.awt.GridLayout(1, 4));
            columnsRow.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 30));
            columnsRow.setBackground(Color.DARK_GRAY);
            // Add a little padding to match the edges of the window
            columnsRow.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 15, 5, 15));

            Font headerFont = new Font("Arial", Font.BOLD, 14);
            Color headerColor = Color.LIGHT_GRAY;

            String[] headers = { "Zeit", "Ziel", "Gleis", "Status" };
            for (String title : headers) {
                javax.swing.JLabel label = new javax.swing.JLabel(title);
                label.setFont(headerFont);
                label.setForeground(headerColor);
                columnsRow.add(label);
            }

            panel.add(columnsRow);

            // Add a little blank space before the train rows start
            panel.add(javax.swing.Box.createRigidArea(new java.awt.Dimension(0, 15)));

            // iterate over trains
            for (int i = 0; i < jnyL.length(); i++) {
                Boolean delayed = false;
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

                // Create a horizontal row panel for this single train
                JPanel row = new JPanel(new java.awt.GridLayout(1, 4)); // 1 row, 4 equal columns
                row.setMaximumSize(new java.awt.Dimension(800, 50)); // Prevent rows from stretching vertically
                row.setBackground(Color.DARK_GRAY);

                // Create individual labels
                javax.swing.JLabel timeLabel = new javax.swing.JLabel(scheduledTime);
                javax.swing.JLabel destLabel = new javax.swing.JLabel(dest);
                javax.swing.JLabel platLabel = new javax.swing.JLabel(realPlatform);
                javax.swing.JLabel statusLabel = new javax.swing.JLabel(statusText);

                // Styling text with color and font!
                Font rowFont = new Font("Arial", Font.PLAIN, 18);
                timeLabel.setFont(new Font("Arial", Font.BOLD, 18));
                timeLabel.setForeground(Color.WHITE);
                destLabel.setFont(rowFont);
                destLabel.setForeground(Color.WHITE);
                platLabel.setFont(rowFont);
                platLabel.setForeground(Color.LIGHT_GRAY);

                // Conditional Colors for delays!
                statusLabel.setFont(rowFont);
                if (delayed) {
                    statusLabel.setForeground(Color.RED); // Vibrant red text for delays!
                } else {
                    statusLabel.setForeground(new Color(50, 205, 50)); // Bright Lime Green for on-time!
                }

                // Add labels to the row in order (left to right)
                row.add(timeLabel);
                row.add(destLabel);
                row.add(platLabel);
                row.add(statusLabel);

                // Add this complete row to your main list panel
                panel.add(row);
            }
            panel.add(javax.swing.Box.createVerticalGlue());
            panel.revalidate(); // Recalculate layout components
            panel.repaint(); // Redraw the screen

        } catch (java.nio.file.NoSuchFileException e) {
            System.out.println("Error: 'request.json' is missing from the folder!");
        } catch (java.io.IOException e) {
            System.out.println("Network Error: Could not connect to ÖBB servers. Check your internet.");
        } catch (org.json.JSONException e) {
            System.out.println("JSON Error: ÖBB changed their data format, or request.json is corrupted.");
        } catch (Exception e) {
            System.out.println("Unknown Error occurred: " + e.getMessage());
        }
    }
}
