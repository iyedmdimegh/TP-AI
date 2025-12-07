package tests;

import code.DeliverySearch;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.QuadCurve2D;
import java.util.*;
import java.util.List;

/**
 * A Graphical User Interface (GUI) test runner for the Delivery Search project.
 * heeeeeeeeeeeeeeeeeeree edit doors mayeb to become portals?
 */
public class VisualTest extends JPanel {

    // --- Problem Data Structures ---
    private final int m, n;
    private final List<Point> stores = new ArrayList<>();
    private final List<Point> customers = new ArrayList<>();
    private final Map<Point, Point> tunnels = new HashMap<>();
    // Store traffic cost to visualize blockades (cost 0)
    private final Map<String, Integer> trafficMap = new HashMap<>();
    
    // --- Solution Data ---
    private final Map<Integer, List<Point>> paths = new HashMap<>(); // Key: Customer Index, Value: List of points

    // --- GUI Constants ---
    private static final int GRID_SPACING = 80; // Distance between intersections
    private static final int PADDING = 50;
    private static final int NODE_RADIUS = 6;

    public VisualTest(String initialState, String traffic, String solution) {
        // 1. Parse Initial State
        String[] parts = initialState.split(";");
        this.m = Integer.parseInt(parts[0]);
        this.n = Integer.parseInt(parts[1]);
        int P = Integer.parseInt(parts[2]);
        int S = Integer.parseInt(parts[3]);

        // Parse Stores (Index 4)
        if (parts.length > 4 && !parts[4].isEmpty()) {
            String[] sCoords = parts[4].split(",");
            for (int i = 0; i < sCoords.length; i += 2) {
                stores.add(new Point(Integer.parseInt(sCoords[i]), Integer.parseInt(sCoords[i+1])));
            }
        }
        
        // Parse Customers (Index 5)
        if (parts.length > 5 && !parts[5].isEmpty()) {
            String[] cCoords = parts[5].split(",");
            for (int i = 0; i < cCoords.length; i += 2) {
                customers.add(new Point(Integer.parseInt(cCoords[i]), Integer.parseInt(cCoords[i+1])));
            }
        }

        // Parse Tunnels (Index 6)
        if (parts.length > 6 && !parts[6].isEmpty()) {
            String[] tTokens = parts[6].split(",");
            for (int i = 0; i < tTokens.length; i += 4) {
                Point p1 = new Point(Integer.parseInt(tTokens[i]), Integer.parseInt(tTokens[i+1]));
                Point p2 = new Point(Integer.parseInt(tTokens[i+2]), Integer.parseInt(tTokens[i+3]));
                tunnels.put(p1, p2);
                tunnels.put(p2, p1);
            }
        }

        // 2. Parse Traffic
        String[] lines = traffic.split(";");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            String[] t = line.split(",");
            // Store both directions
            String key = t[0] + "," + t[1] + "," + t[2] + "," + t[3];
            int cost = Integer.parseInt(t[4]);
            trafficMap.put(key, cost);
            
            String revKey = t[2] + "," + t[3] + "," + t[0] + "," + t[1];
            trafficMap.put(revKey, cost);
        }

        // 3. Parse Solution Paths
        parseSolution(solution);
        
        // GUI Setup
        // Calculate size based on grid intersections
        this.setPreferredSize(new Dimension((m - 1) * GRID_SPACING + 2 * PADDING, (n - 1) * GRID_SPACING + 2 * PADDING));
        this.setBackground(Color.WHITE);
    }

    private void parseSolution(String solution) {
        String[] solLines = solution.split("\n");
        for (String line : solLines) {
            if (!line.startsWith("(S")) continue;
            
            try {
                int commaIdx = line.indexOf(",");
                int parenEndIdx = line.indexOf(")");
                int colonIdx = line.indexOf(":");
                int semiIdx = line.indexOf(";");

                if (colonIdx == -1 || semiIdx == -1) continue;

                // Extract Indices (1-based in string, convert to 0-based)
                String storeStr = line.substring(2, commaIdx); 
                String custStr = line.substring(commaIdx + 2, parenEndIdx); 
                int storeIdx = Integer.parseInt(storeStr) - 1;
                int custIdx = Integer.parseInt(custStr) - 1;

                String opsStr = line.substring(colonIdx + 1, semiIdx);
                if (opsStr.equals("NoPath") || opsStr.isEmpty()) continue;

                List<Point> pathPoints = new ArrayList<>();
                // Safety check for store index
                if (storeIdx < stores.size()) {
                    Point current = new Point(stores.get(storeIdx));
                    pathPoints.add(new Point(current)); 

                    String[] ops = opsStr.split(",");
                    for (String op : ops) {
                        if (op.equals("up")) current.y++;
                        else if (op.equals("down")) current.y--;
                        else if (op.equals("right")) current.x++;
                        else if (op.equals("left")) current.x--;
                        else if (op.equals("tunnel")) {
                            if (tunnels.containsKey(current)) {
                                current = tunnels.get(current);
                            }
                        }
                        pathPoints.add(new Point(current));
                    }
                    paths.put(custIdx, pathPoints);
                }

            } catch (Exception e) {
                System.err.println("Error parsing solution line: " + line);
            }
        }
    }

    /**
     * Helper to convert logical grid coordinates to pixel coordinates.
     * Flip Y so (0,0) is at bottom-left.
     */
    private Point toPixel(int x, int y) {
        int px = PADDING + x * GRID_SPACING;
        int py = PADDING + (n - 1 - y) * GRID_SPACING;
        return new Point(px, py);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // --- 1. Draw Grid Lines (Streets) ---
        g2.setStroke(new BasicStroke(2));
        g2.setColor(Color.LIGHT_GRAY);

        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                Point p1 = toPixel(x, y);

                // Draw Horizontal Line (to Right)
                if (x + 1 < m) {
                    Point p2 = toPixel(x + 1, y);
                    drawEdge(g2, x, y, x + 1, y, p1, p2);
                }
                // Draw Vertical Line (to Up)
                if (y + 1 < n) {
                    Point p2 = toPixel(x, y + 1);
                    drawEdge(g2, x, y, x, y + 1, p1, p2);
                }
            }
        }

        // --- 2. Draw Tunnels ---
        g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5.0f}, 0.0f));
        g2.setColor(new Color(100, 100, 100)); // Dark Gray connection line
        
        Set<String> drawnTunnels = new HashSet<>();

        for (Map.Entry<Point, Point> entry : tunnels.entrySet()) {
            Point start = entry.getKey();
            Point end = entry.getValue();
            
            // Generate unique key to avoid drawing twice
            String key = (start.x < end.x) ? start + "-" + end : end + "-" + start;
            if (drawnTunnels.contains(key)) continue;
            drawnTunnels.add(key);

            Point p1 = toPixel(start.x, start.y);
            Point p2 = toPixel(end.x, end.y);

            // Draw Curve Connection
            QuadCurve2D q = new QuadCurve2D.Float();
            double mx = (p1.x + p2.x) / 2.0;
            double my = (p1.y + p2.y) / 2.0;
            double dx = p2.x - p1.x;
            double dy = p2.y - p1.y;
            double offset = 40.0;
            double cx = mx - dy * 0.2; 
            double cy = my + dx * 0.2;
            
            q.setCurve(p1.x, p1.y, cx, cy, p2.x, p2.y);
            g2.draw(q);
            
            // Draw Door Icons at both ends
            drawDoor(g2, p1);
            drawDoor(g2, p2);
        }

        // --- 3. Draw Paths (Solution) ---
        g2.setStroke(new BasicStroke(4));
        for (Map.Entry<Integer, List<Point>> entry : paths.entrySet()) {
            int custIdx = entry.getKey();
            List<Point> path = entry.getValue();
            
            // Unique color per customer
            float hue = (float) custIdx / Math.max(1, customers.size());
            Color c = Color.getHSBColor(hue, 0.9f, 0.9f);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 150)); // Semi-transparent

            for (int i = 0; i < path.size() - 1; i++) {
                Point cur = path.get(i);
                Point next = path.get(i+1);
                Point pix1 = toPixel(cur.x, cur.y);
                Point pix2 = toPixel(next.x, next.y);
                
                // If tunnel (distance > 1), draw straight line or curve? 
                if (Math.abs(cur.x - next.x) + Math.abs(cur.y - next.y) > 1) {
                     g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{5}, 0));
                     g2.drawLine(pix1.x, pix1.y, pix2.x, pix2.y);
                     g2.setStroke(new BasicStroke(4));
                } else {
                    g2.drawLine(pix1.x, pix1.y, pix2.x, pix2.y);
                }
            }
        }

        // --- 4. Draw Nodes (Intersections) ---
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                Point p = toPixel(x, y);
                g2.setColor(Color.DARK_GRAY);
                g2.fillOval(p.x - 3, p.y - 3, 6, 6);
                
                // Draw coord label
                g2.setColor(Color.GRAY);
                g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
                g2.drawString(x + "," + y, p.x + 5, p.y + 15);
            }
        }

        // --- 5. Draw Stores & Customers ---
        
        // Stores: Blue Squares
        for (int i = 0; i < stores.size(); i++) {
            Point p = toPixel(stores.get(i).x, stores.get(i).y);
            g2.setColor(Color.BLUE);
            g2.fillRect(p.x - 10, p.y - 10, 20, 20);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString("S" + (i+1), p.x - 7, p.y + 5);
        }

        // Customers: Green Circles
        for (int i = 0; i < customers.size(); i++) {
            Point p = toPixel(customers.get(i).x, customers.get(i).y);
            g2.setColor(new Color(0, 180, 0));
            g2.fillOval(p.x - 10, p.y - 10, 20, 20);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString("D" + (i+1), p.x - 7, p.y + 5);
        }
    }

    private void drawDoor(Graphics2D g2, Point p) {
        int w = 16;
        int h = 24;
        int x = p.x - w / 2;
        int y = p.y - h / 2;
        
        // Save current color/stroke
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        
        // Door Frame
        g2.setColor(new Color(139, 69, 19)); // Saddle Brown
        g2.fillRect(x, y, w, h);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1));
        g2.drawRect(x, y, w, h);
        
        // Knob
        g2.setColor(Color.YELLOW);
        g2.fillOval(x + w - 5, y + h / 2 - 2, 3, 3);
        
        // Restore
        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
    }

    private void drawEdge(Graphics2D g2, int x1, int y1, int x2, int y2, Point p1, Point p2) {
        String key = x1 + "," + y1 + "," + x2 + "," + y2;
        Integer cost = trafficMap.get(key);
        
        int displayCost;
        if (cost != null) {
            displayCost = cost;
        } else {
            // Fallback for visual safety, but map should now be fully populated
            int h = Objects.hash(x1, y1, x2, y2);
            displayCost = (Math.abs(h) % 5) + 1;
        }

        if (cost != null && cost == 0) {
            // Blockade: Red Thick Line
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(6));
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            
            // X mark in middle
            int mx = (p1.x + p2.x) / 2;
            int my = (p1.y + p2.y) / 2;
            g2.setColor(Color.RED);
            g2.drawString("X", mx - 4, my + 4);
            
            // Reset
            g2.setStroke(new BasicStroke(2));
            g2.setColor(Color.LIGHT_GRAY);
        } else {
            // Normal Street
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            
            // Draw Weight
            int mx = (p1.x + p2.x) / 2;
            int my = (p1.y + p2.y) / 2;
            
            // Draw a small background for text readability
            g2.setColor(Color.WHITE);
            // g2.fillOval(mx - 6, my - 6, 12, 12); // Optional white dot behind text
            
            g2.setColor(new Color(50, 50, 150)); // Dark Blue for visibility
            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            // Offset slightly to appear above the line
            g2.drawString(String.valueOf(displayCost), mx + 2, my - 3);
        }
    }




    // --- Helper to Generate Random Traffic Input ---
    private static String generateRandomTraffic(int m, int n) {
        StringBuilder sb = new StringBuilder();
        // Use fixed seed for consistency between run and visualize
        Random rand = new Random(12345); 
        
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                // Horizontal (Right)
                if (x + 1 < m) {
                    int cost = rand.nextInt(5) + 1; // 1-5
                    sb.append(x).append(",").append(y).append(",")
                      .append(x + 1).append(",").append(y).append(",")
                      .append(cost).append(";");
                }
                // Vertical (Up)
                if (y + 1 < n) {
                    int cost = rand.nextInt(5) + 1; // 1-5
                    sb.append(x).append(",").append(y).append(",")
                      .append(x).append(",").append(y + 1).append(",")
                      .append(cost).append(";");
                }
            }
        }
        return sb.toString();
    }
















    // --- Main Execution ---
    public static void main(String[] args) {
        // Scenario:
        // Grid: 6x6
        // Stores: 2 at (0,0) and (5,5)
        // Customers: 3 at (2,2), (3,3), (1,5)
        // Tunnels: (0,1) <-> (0,5)
        // Blockades: (1,1)->(1,2)
        
        String state = "6;6;3;2;0,0,5,5;2,2,3,3,1,5;0,1,0,5";
        
        // 1. Generate full random map (weights 1-5)
        String baseTraffic = generateRandomTraffic(6, 6);
        
        // 2. Append blockades (overwriting the random weight for that specific edge)
        // Block (1,1) -> (1,2) with cost 0
        String traffic = baseTraffic + "1,1,1,2,0;";

        System.out.println("Solving for Visualization...");
        String solution = DeliverySearch.solve(state, traffic, "AS1", false);
        System.out.println("Solution found:\n" + solution);

        // Launch GUI
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Delivery Agent Visualization");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            VisualTest panel = new VisualTest(state, traffic, solution);
            JScrollPane scroll = new JScrollPane(panel);
            
            frame.add(scroll);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}