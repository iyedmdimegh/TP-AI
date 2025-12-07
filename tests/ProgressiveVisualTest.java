package tests;

import code.DeliverySearch;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.QuadCurve2D;
import java.util.*;
import java.util.List;

/**
 * Progressive Visual Test Suite
 * Starts with simple scenarios and gradually increases complexity
 * Each test can be run independently via menu selection
 */
public class ProgressiveVisualTest extends JPanel {

    // --- Problem Data Structures ---
    private final int m, n;
    private final List<Point> stores = new ArrayList<>();
    private final List<Point> customers = new ArrayList<>();
    private final Map<Point, Point> tunnels = new HashMap<>();
    private final Map<String, Integer> trafficMap = new HashMap<>();
    private final Map<Integer, List<Point>> paths = new HashMap<>();
    
    // --- GUI Constants ---
    private static final int GRID_SPACING = 70;
    private static final int PADDING = 50;
    
    private String testName = "";

    public ProgressiveVisualTest(String initialState, String traffic, String solution, String testName) {
        this.testName = testName;
        
        // Parse Initial State
        String[] parts = initialState.split(";");
        this.m = Integer.parseInt(parts[0]);
        this.n = Integer.parseInt(parts[1]);

        // Parse Stores
        if (parts.length > 4 && !parts[4].isEmpty()) {
            String[] sCoords = parts[4].split(",");
            for (int i = 0; i < sCoords.length; i += 2) {
                stores.add(new Point(Integer.parseInt(sCoords[i]), Integer.parseInt(sCoords[i+1])));
            }
        }
        
        // Parse Customers
        if (parts.length > 5 && !parts[5].isEmpty()) {
            String[] cCoords = parts[5].split(",");
            for (int i = 0; i < cCoords.length; i += 2) {
                customers.add(new Point(Integer.parseInt(cCoords[i]), Integer.parseInt(cCoords[i+1])));
            }
        }

        // Parse Tunnels
        if (parts.length > 6 && !parts[6].isEmpty()) {
            String[] tTokens = parts[6].split(",");
            for (int i = 0; i + 3 < tTokens.length; i += 4) {
                Point p1 = new Point(Integer.parseInt(tTokens[i]), Integer.parseInt(tTokens[i+1]));
                Point p2 = new Point(Integer.parseInt(tTokens[i+2]), Integer.parseInt(tTokens[i+3]));
                tunnels.put(p1, p2);
                tunnels.put(p2, p1);
            }
        }

        // Parse Traffic
        String[] lines = traffic.split(";");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            String[] t = line.split(",");
            String key = t[0] + "," + t[1] + "," + t[2] + "," + t[3];
            int cost = Integer.parseInt(t[4]);
            trafficMap.put(key, cost);
            String revKey = t[2] + "," + t[3] + "," + t[0] + "," + t[1];
            trafficMap.put(revKey, cost);
        }

        parseSolution(solution);
        
        this.setPreferredSize(new Dimension(
            Math.max(600, (m - 1) * GRID_SPACING + 2 * PADDING), 
            Math.max(500, (n - 1) * GRID_SPACING + 2 * PADDING + 50)
        ));
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

                int storeIdx = Integer.parseInt(line.substring(2, commaIdx)) - 1;
                int custIdx = Integer.parseInt(line.substring(commaIdx + 2, parenEndIdx)) - 1;
                String opsStr = line.substring(colonIdx + 1, semiIdx);
                
                if (opsStr.equals("NoPath") || opsStr.isEmpty()) continue;

                List<Point> pathPoints = new ArrayList<>();
                if (storeIdx < stores.size()) {
                    Point current = new Point(stores.get(storeIdx));
                    pathPoints.add(new Point(current)); 

                    String[] ops = opsStr.split(",");
                    for (String op : ops) {
                        if (op.equals("up")) current.y++;
                        else if (op.equals("down")) current.y--;
                        else if (op.equals("right")) current.x++;
                        else if (op.equals("left")) current.x--;
                        else if (op.equals("tunnel") && tunnels.containsKey(current)) {
                            current = tunnels.get(current);
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

    private Point toPixel(int x, int y) {
        int px = PADDING + x * GRID_SPACING;
        int py = PADDING + 30 + (n - 1 - y) * GRID_SPACING;
        return new Point(px, py);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw Title
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.drawString(testName, 10, 25);

        // Draw Grid Lines
        g2.setStroke(new BasicStroke(2));
        g2.setColor(Color.LIGHT_GRAY);
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                Point p1 = toPixel(x, y);
                if (x + 1 < m) {
                    Point p2 = toPixel(x + 1, y);
                    drawEdge(g2, x, y, x + 1, y, p1, p2);
                }
                if (y + 1 < n) {
                    Point p2 = toPixel(x, y + 1);
                    drawEdge(g2, x, y, x, y + 1, p1, p2);
                }
            }
        }

        // Draw Tunnels
        g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{5.0f}, 0.0f));
        g2.setColor(new Color(100, 100, 100));
        Set<String> drawnTunnels = new HashSet<>();
        for (Map.Entry<Point, Point> entry : tunnels.entrySet()) {
            Point start = entry.getKey();
            Point end = entry.getValue();
            String key = (start.x < end.x) ? start + "-" + end : end + "-" + start;
            if (drawnTunnels.contains(key)) continue;
            drawnTunnels.add(key);

            Point p1 = toPixel(start.x, start.y);
            Point p2 = toPixel(end.x, end.y);
            QuadCurve2D q = new QuadCurve2D.Float();
            double mx = (p1.x + p2.x) / 2.0;
            double my = (p1.y + p2.y) / 2.0;
            double dx = p2.x - p1.x;
            double dy = p2.y - p1.y;
            double cx = mx - dy * 0.2;
            double cy = my + dx * 0.2;
            q.setCurve(p1.x, p1.y, cx, cy, p2.x, p2.y);
            g2.draw(q);
            drawDoor(g2, p1);
            drawDoor(g2, p2);
        }

        // Draw Paths
        g2.setStroke(new BasicStroke(4));
        for (Map.Entry<Integer, List<Point>> entry : paths.entrySet()) {
            int custIdx = entry.getKey();
            List<Point> path = entry.getValue();
            
            // Special color for Test 12 (Single Path test) - use cyan to avoid confusion with red obstacles
            Color c;
            if (testName.contains("Test 12") || testName.contains("Single Path")) {
                c = new Color(0, 200, 255); // Bright cyan
            } else {
                float hue = (float) custIdx / Math.max(1, customers.size());
                c = Color.getHSBColor(hue, 0.9f, 0.9f);
            }
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 180)); // Semi-transparent

            for (int i = 0; i < path.size() - 1; i++) {
                Point cur = path.get(i);
                Point next = path.get(i+1);
                Point pix1 = toPixel(cur.x, cur.y);
                Point pix2 = toPixel(next.x, next.y);
                
                if (Math.abs(cur.x - next.x) + Math.abs(cur.y - next.y) > 1) {
                    g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{5}, 0));
                    g2.drawLine(pix1.x, pix1.y, pix2.x, pix2.y);
                    g2.setStroke(new BasicStroke(4));
                } else {
                    g2.drawLine(pix1.x, pix1.y, pix2.x, pix2.y);
                }
            }
        }

        // Draw Nodes
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                Point p = toPixel(x, y);
                g2.setColor(Color.DARK_GRAY);
                g2.fillOval(p.x - 3, p.y - 3, 6, 6);
                g2.setColor(Color.GRAY);
                g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
                g2.drawString(x + "," + y, p.x + 5, p.y + 12);
            }
        }

        // Draw Stores
        for (int i = 0; i < stores.size(); i++) {
            Point p = toPixel(stores.get(i).x, stores.get(i).y);
            g2.setColor(Color.BLUE);
            g2.fillRect(p.x - 10, p.y - 10, 20, 20);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.drawString("S" + (i+1), p.x - 6, p.y + 4);
        }

        // Draw Customers
        for (int i = 0; i < customers.size(); i++) {
            Point p = toPixel(customers.get(i).x, customers.get(i).y);
            g2.setColor(new Color(0, 180, 0));
            g2.fillOval(p.x - 10, p.y - 10, 20, 20);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            g2.drawString("D" + (i+1), p.x - 6, p.y + 4);
        }
    }

    private void drawDoor(Graphics2D g2, Point p) {
        int w = 14, h = 20;
        int x = p.x - w / 2;
        int y = p.y - h / 2;
        Color oldColor = g2.getColor();
        Stroke oldStroke = g2.getStroke();
        g2.setColor(new Color(139, 69, 19));
        g2.fillRect(x, y, w, h);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1));
        g2.drawRect(x, y, w, h);
        g2.setColor(Color.YELLOW);
        g2.fillOval(x + w - 4, y + h / 2 - 1, 2, 2);
        g2.setColor(oldColor);
        g2.setStroke(oldStroke);
    }

    private void drawEdge(Graphics2D g2, int x1, int y1, int x2, int y2, Point p1, Point p2) {
        String key = x1 + "," + y1 + "," + x2 + "," + y2;
        Integer cost = trafficMap.get(key);

        if (cost != null && cost == 0) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(6));
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            int mx = (p1.x + p2.x) / 2;
            int my = (p1.y + p2.y) / 2;
            g2.drawString("X", mx - 4, my + 4);
            g2.setStroke(new BasicStroke(2));
            g2.setColor(Color.LIGHT_GRAY);
        } else {
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            if (cost != null && cost > 1) {
                int mx = (p1.x + p2.x) / 2;
                int my = (p1.y + p2.y) / 2;
                g2.setColor(new Color(50, 50, 150));
                g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                g2.drawString(String.valueOf(cost), mx + 2, my - 2);
            }
        }
    }

    // ========== TEST CASES ==========

    // Level 1: Simplest - Tiny Grid, Single Package
    static void test1_TinyGrid() {
        System.out.println("\n=== Test 1: Tiny Grid (3x3) ===");
        String state = "3;3;1;1;0,0;2,2;";
        String traffic = "";
        runTest(state, traffic, "BF", "Test 1: Tiny Grid - Basic Path");
    }

    // Level 2: Small Grid with One Blockage
    static void test2_SingleBlockage() {
        System.out.println("\n=== Test 2: Small Grid with Blockage (4x4) ===");
        String state = "4;4;1;1;0,0;3,3;";
        String traffic = "1,1,2,1,0;2,1,1,1,0;";
        runTest(state, traffic, "BF", "Test 2: Small Grid - Single Blockage");
    }

    // Level 3: Introduction of One Tunnel
    static void test3_OneTunnel() {
        System.out.println("\n=== Test 3: One Tunnel (5x5) ===");
        String state = "5;5;1;1;0,0;4,4;0,0,4,4";
        String traffic = "0,0,1,0,10;0,0,0,1,10;";
        runTest(state, traffic, "UC", "Test 3: One Tunnel - Shortcut");
    }

    // Level 4: Multiple Stores
    static void test4_MultipleStores() {
        System.out.println("\n=== Test 4: Multiple Stores (6x6) ===");
        String state = "6;6;2;3;0,0,5,5,2,2;3,3,4,4;";
        String traffic = "";
        runTest(state, traffic, "AS1", "Test 4: Multiple Stores - Selection");
    }

    // Level 5: Multiple Tunnels
    static void test5_MultipleTunnels() {
        System.out.println("\n=== Test 5: Multiple Tunnels (7x7) ===");
        String state = "7;7;2;2;0,0,6,6;2,2,5,5;0,0,2,2,4,4,6,6";
        String traffic = "1,0,2,0,8;2,0,3,0,8;3,0,4,0,8;";
        runTest(state, traffic, "AS1", "Test 5: Multiple Tunnels - Network");
    }

    // Level 6: Medium Grid with Traffic Variations
    static void test6_TrafficVariations() {
        System.out.println("\n=== Test 6: Traffic Variations (8x8) ===");
        String state = "8;8;3;2;0,0,7,7;2,2,5,5,7,0;";
        StringBuilder traffic = new StringBuilder();
        Random rand = new Random(42);
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                if (x + 1 < 8) {
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x + 1).append(",").append(y).append(",")
                           .append(rand.nextInt(5) + 1).append(";");
                }
                if (y + 1 < 8) {
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x).append(",").append(y + 1).append(",")
                           .append(rand.nextInt(5) + 1).append(";");
                }
            }
        }
        runTest(state, traffic.toString(), "AS2", "Test 6: Traffic Variations");
    }

    // Level 7: Complex Maze with Blockages
    static void test7_Maze() {
        System.out.println("\n=== Test 7: Maze with Blockages (9x9) ===");
        String state = "9;9;2;1;0,0;8,8,4,4;";
        StringBuilder traffic = new StringBuilder();
        // Create maze walls
        traffic.append("2,0,2,1,0;2,1,2,0,0;");
        traffic.append("2,2,2,3,0;2,3,2,2,0;");
        traffic.append("4,2,4,3,0;4,3,4,2,0;");
        traffic.append("6,4,6,5,0;6,5,6,4,0;");
        traffic.append("3,6,4,6,0;4,6,3,6,0;");
        runTest(state, traffic.toString(), "AS1", "Test 7: Maze Navigation");
    }

    // Level 8: Large Grid - Multiple Everything
    static void test8_LargeComplex() {
        System.out.println("\n=== Test 8: Large Complex (10x10) ===");
        String state = "10;10;4;3;0,0,9,9,5,5;2,2,7,7,4,6,8,1;1,1,8,8,3,3,6,6";
        StringBuilder traffic = new StringBuilder();
        Random rand = new Random(100);
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                if (x + 1 < 10) {
                    int cost = rand.nextInt(6);
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x + 1).append(",").append(y).append(",")
                           .append(cost).append(";");
                }
                if (y + 1 < 10) {
                    int cost = rand.nextInt(6);
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x).append(",").append(y + 1).append(",")
                           .append(cost).append(";");
                }
            }
        }
        runTest(state, traffic.toString(), "AS1", "Test 8: Large Complex Grid");
    }

    // Level 9: Wide Grid
    static void test9_WideGrid() {
        System.out.println("\n=== Test 9: Wide Grid (15x5) ===");
        String state = "15;5;3;2;0,2,14,2;5,2,10,2,7,4;";
        String traffic = "3,2,4,2,0;4,2,3,2,0;8,2,9,2,0;9,2,8,2,0;";
        runTest(state, traffic, "AS1", "Test 9: Wide Grid");
    }

    // Level 10: Tall Grid
    static void test10_TallGrid() {
        System.out.println("\n=== Test 10: Tall Grid (5x15) ===");
        String state = "5;15;3;2;2,0,2,14;2,5,2,10,3,7;0,0,0,14";
        String traffic = "2,4,2,5,0;2,5,2,4,0;2,9,2,10,0;2,10,2,9,0;";
        runTest(state, traffic, "AS2", "Test 10: Tall Grid");
    }

    // Level 11: Obstacle Groups Pattern
    static void test11_ObstacleGroups() {
        System.out.println("\n=== Test 11: Obstacle Groups - Rectangles & Squares (11x11) ===");
        String state = "11;11;3;2;0,0,10,10;5,5,8,3,3,8;";
        StringBuilder traffic = new StringBuilder();
        
        // Create rectangular and square obstacle formations to force wiggly paths
        
        // Rectangle 1: Horizontal barrier at rows 2-3, columns 2-5
        for (int x = 2; x <= 5; x++) {
            for (int y = 2; y <= 3; y++) {
                // Block all edges within and around the rectangle
                if (x < 5) {
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x + 1).append(",").append(y).append(",0;");
                    traffic.append(x + 1).append(",").append(y).append(",")
                           .append(x).append(",").append(y).append(",0;");
                }
                if (y < 3) {
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x).append(",").append(y + 1).append(",0;");
                    traffic.append(x).append(",").append(y + 1).append(",")
                           .append(x).append(",").append(y).append(",0;");
                }
                // Block edges entering the rectangle from outside
                if (x == 2) {
                    traffic.append(x - 1).append(",").append(y).append(",")
                           .append(x).append(",").append(y).append(",0;");
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x - 1).append(",").append(y).append(",0;");
                }
                if (x == 5) {
                    traffic.append(x + 1).append(",").append(y).append(",")
                           .append(x).append(",").append(y).append(",0;");
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x + 1).append(",").append(y).append(",0;");
                }
                if (y == 2) {
                    traffic.append(x).append(",").append(y - 1).append(",")
                           .append(x).append(",").append(y).append(",0;");
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x).append(",").append(y - 1).append(",0;");
                }
                if (y == 3) {
                    traffic.append(x).append(",").append(y + 1).append(",")
                           .append(x).append(",").append(y).append(",0;");
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x).append(",").append(y + 1).append(",0;");
                }
            }
        }
        
        // Square 1: 2x2 at position (7,6-7)
        for (int x = 7; x <= 8; x++) {
            for (int y = 6; y <= 7; y++) {
                // Block internal edges
                if (x < 8) {
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x + 1).append(",").append(y).append(",0;");
                    traffic.append(x + 1).append(",").append(y).append(",")
                           .append(x).append(",").append(y).append(",0;");
                }
                if (y < 7) {
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x).append(",").append(y + 1).append(",0;");
                    traffic.append(x).append(",").append(y + 1).append(",")
                           .append(x).append(",").append(y).append(",0;");
                }
                // Block perimeter edges
                if (x == 7) {
                    traffic.append(x - 1).append(",").append(y).append(",")
                           .append(x).append(",").append(y).append(",0;");
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x - 1).append(",").append(y).append(",0;");
                }
                if (x == 8) {
                    traffic.append(x + 1).append(",").append(y).append(",")
                           .append(x).append(",").append(y).append(",0;");
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x + 1).append(",").append(y).append(",0;");
                }
                if (y == 6) {
                    traffic.append(x).append(",").append(y - 1).append(",")
                           .append(x).append(",").append(y).append(",0;");
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x).append(",").append(y - 1).append(",0;");
                }
                if (y == 7) {
                    traffic.append(x).append(",").append(y + 1).append(",")
                           .append(x).append(",").append(y).append(",0;");
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x).append(",").append(y + 1).append(",0;");
                }
            }
        }
        
        // Rectangle 2: Vertical barrier at columns 1-2, rows 6-8
        for (int x = 1; x <= 2; x++) {
            for (int y = 6; y <= 8; y++) {
                // Block internal edges
                if (x < 2) {
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x + 1).append(",").append(y).append(",0;");
                    traffic.append(x + 1).append(",").append(y).append(",")
                           .append(x).append(",").append(y).append(",0;");
                }
                if (y < 8) {
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x).append(",").append(y + 1).append(",0;");
                    traffic.append(x).append(",").append(y + 1).append(",")
                           .append(x).append(",").append(y).append(",0;");
                }
                // Block perimeter edges
                if (x == 1) {
                    traffic.append(x - 1).append(",").append(y).append(",")
                           .append(x).append(",").append(y).append(",0;");
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x - 1).append(",").append(y).append(",0;");
                }
                if (x == 2) {
                    traffic.append(x + 1).append(",").append(y).append(",")
                           .append(x).append(",").append(y).append(",0;");
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x + 1).append(",").append(y).append(",0;");
                }
                if (y == 6) {
                    traffic.append(x).append(",").append(y - 1).append(",")
                           .append(x).append(",").append(y).append(",0;");
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x).append(",").append(y - 1).append(",0;");
                }
                if (y == 8) {
                    traffic.append(x).append(",").append(y + 1).append(",")
                           .append(x).append(",").append(y).append(",0;");
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x).append(",").append(y + 1).append(",0;");
                }
            }
        }
        
        runTest(state, traffic.toString(), "AS1", "Test 11: Obstacle Groups - Rectangles Force Wiggly Paths");
    }

    // Level 12: Single Path - Forced Corridor
    static void test12_SinglePath() {
        System.out.println("\n=== Test 12: Single Path - Strategic Obstacles (11x11) ===");
        String state = "11;11;1;1;0,0;10,10;";
        StringBuilder traffic = new StringBuilder();
        
        // Define the ONLY allowed path as a sequence of moves
        String[][] path = {
            {"0", "0"},   // Start
            {"1", "0"},   // right
            {"2", "0"},   // right
            {"3", "0"},   // right
            {"3", "1"},   // up
            {"3", "2"},   // up
            {"4", "2"},   // right
            {"5", "2"},   // right
            {"5", "3"},   // up
            {"5", "4"},   // up
            {"4", "4"},   // left
            {"3", "4"},   // left
            {"2", "4"},   // left
            {"2", "5"},   // up
            {"2", "6"},   // up
            {"3", "6"},   // right
            {"4", "6"},   // right
            {"5", "6"},   // right
            {"6", "6"},   // right
            {"7", "6"},   // right
            {"7", "7"},   // up
            {"7", "8"},   // up
            {"6", "8"},   // left
            {"5", "8"},   // left
            {"5", "9"},   // up
            {"5", "10"},  // up
            {"6", "10"},  // right
            {"7", "10"},  // right
            {"8", "10"},  // right
            {"9", "10"},  // right
            {"10", "10"}  // right (goal)
        };
        
        // Store the allowed path edges in a set for quick lookup
        java.util.Set<String> allowedEdges = new java.util.HashSet<>();
        for (int i = 0; i < path.length - 1; i++) {
            int x1 = Integer.parseInt(path[i][0]);
            int y1 = Integer.parseInt(path[i][1]);
            int x2 = Integer.parseInt(path[i + 1][0]);
            int y2 = Integer.parseInt(path[i + 1][1]);
            
            // Add edge in both directions
            allowedEdges.add(x1 + "," + y1 + "," + x2 + "," + y2);
            allowedEdges.add(x2 + "," + y2 + "," + x1 + "," + y1);
        }
        
        // Now iterate through ALL possible edges in the grid
        // If edge is in our path, allow it (cost 1)
        // Otherwise, block it (cost 0)
        for (int x = 0; x < 11; x++) {
            for (int y = 0; y < 11; y++) {
                // Check horizontal edge to the right
                if (x + 1 < 11) {
                    String edgeKey = x + "," + y + "," + (x + 1) + "," + y;
                    if (allowedEdges.contains(edgeKey)) {
                        traffic.append(x).append(",").append(y).append(",")
                               .append(x + 1).append(",").append(y).append(",1;");
                    } else {
                        traffic.append(x).append(",").append(y).append(",")
                               .append(x + 1).append(",").append(y).append(",0;");
                    }
                }
                
                // Check vertical edge upward
                if (y + 1 < 11) {
                    String edgeKey = x + "," + y + "," + x + "," + (y + 1);
                    if (allowedEdges.contains(edgeKey)) {
                        traffic.append(x).append(",").append(y).append(",")
                               .append(x).append(",").append(y + 1).append(",1;");
                    } else {
                        traffic.append(x).append(",").append(y).append(",")
                               .append(x).append(",").append(y + 1).append(",0;");
                    }
                }
            }
        }
        
        runTest(state, traffic.toString(), "BF", "Test 12: Forced Winding Path via Strategic Obstacles");
    }

    // Level 13: Maximum Complexity
    static void test13_MaxComplexity() {
        System.out.println("\n=== Test 13: Maximum Complexity (12x12) ===");
        String state = "12;12;5;4;0,0,11,11,6,6,0,11;3,3,8,8,10,2,5,9,2,10;1,1,10,10,5,5,7,7,3,9,9,3";
        StringBuilder traffic = new StringBuilder();
        Random rand = new Random(200);
        for (int x = 0; x < 12; x++) {
            for (int y = 0; y < 12; y++) {
                if (x + 1 < 12) {
                    int cost = rand.nextInt(8);
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x + 1).append(",").append(y).append(",")
                           .append(cost).append(";");
                }
                if (y + 1 < 12) {
                    int cost = rand.nextInt(8);
                    traffic.append(x).append(",").append(y).append(",")
                           .append(x).append(",").append(y + 1).append(",")
                           .append(cost).append(";");
                }
            }
        }
        runTest(state, traffic.toString(), "AS1", "Test 13: Maximum Complexity");
    }

    // Helper method to run test - tries all strategies and picks the best one
    static void runTest(String state, String traffic, String suggestedStrategy, String testName) {
        System.out.println("Running: " + testName);
        System.out.println("Testing all strategies to find the most efficient...");
        
        String[] allStrategies = {"BF", "DF", "ID", "UC", "GR1", "GR2", "AS1", "AS2"};
        String bestStrategy = suggestedStrategy;
        int bestNodes = Integer.MAX_VALUE;
        String bestSolution = "";
        long bestTime = 0;
        
        // Try all strategies and track which uses fewest nodes
        for (String strategy : allStrategies) {
            try {
                long startTime = System.currentTimeMillis();
                String solution = DeliverySearch.solve(state, traffic, strategy, false);
                long endTime = System.currentTimeMillis();
                
                // Sum up total nodes expanded across all deliveries
                // Format: (S1,D1):actions;cost;nodes
                String[] deliveries = solution.split("\n");
                int totalNodes = 0;
                boolean valid = false;
                
                for (String delivery : deliveries) {
                    if (delivery.trim().startsWith("(S")) {
                        // Find the last semicolon - nodes is the last field
                        int lastSemicolon = delivery.lastIndexOf(';');
                        int secondLastSemicolon = delivery.lastIndexOf(';', lastSemicolon - 1);
                        
                        if (lastSemicolon > 0 && secondLastSemicolon > 0) {
                            String nodesStr = delivery.substring(lastSemicolon + 1).trim();
                            totalNodes += Integer.parseInt(nodesStr);
                            valid = true;
                        }
                    }
                }
                
                if (valid) {
                    System.out.println("  " + strategy + ": " + totalNodes + " nodes, " + 
                                     (endTime - startTime) + "ms");
                    
                    if (totalNodes < bestNodes) {
                        bestNodes = totalNodes;
                        bestStrategy = strategy;
                        bestSolution = solution;
                        bestTime = endTime - startTime;
                    }
                }
            } catch (Exception e) {
                System.out.println("  " + strategy + ": Failed - " + e.getMessage());
            }
        }
        
        System.out.println("\n✓ Best Strategy: " + bestStrategy + " (" + bestNodes + " nodes, " + bestTime + "ms)");
        System.out.println("Solution:\n" + bestSolution);

        final String finalStrategy = bestStrategy;
        final String finalSolution = bestSolution;
        
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(testName + " - " + finalStrategy);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            ProgressiveVisualTest panel = new ProgressiveVisualTest(state, traffic, finalSolution, testName);
            JScrollPane scroll = new JScrollPane(panel);
            frame.add(scroll);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // Main with Menu
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║  Progressive Visual Test Suite             ║");
        System.out.println("║  Complexity Levels: Simple → Complex       ║");
        System.out.println("╚════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Select Test:");
        System.out.println("  1. Tiny Grid (3x3) - Simplest");
        System.out.println("  2. Small Grid with Blockage (4x4)");
        System.out.println("  3. One Tunnel (5x5)");
        System.out.println("  4. Multiple Stores (6x6)");
        System.out.println("  5. Multiple Tunnels (7x7)");
        System.out.println("  6. Traffic Variations (8x8)");
        System.out.println("  7. Maze with Blockages (9x9)");
        System.out.println("  8. Large Complex (10x10)");
        System.out.println("  9. Wide Grid (15x5)");
        System.out.println(" 10. Tall Grid (5x15)");
        System.out.println(" 11. Obstacle Groups - Rectangles (11x11)");
        System.out.println(" 12. Single Path - Forced Snake (11x11)");
        System.out.println(" 13. Maximum Complexity (12x12)");
        System.out.println("  0. Run All Tests Sequentially");
        System.out.println();
        System.out.print("Enter choice (0-13): ");
        
        int choice = scanner.nextInt();
        
        switch(choice) {
            case 1: test1_TinyGrid(); break;
            case 2: test2_SingleBlockage(); break;
            case 3: test3_OneTunnel(); break;
            case 4: test4_MultipleStores(); break;
            case 5: test5_MultipleTunnels(); break;
            case 6: test6_TrafficVariations(); break;
            case 7: test7_Maze(); break;
            case 8: test8_LargeComplex(); break;
            case 9: test9_WideGrid(); break;
            case 10: test10_TallGrid(); break;
            case 11: test11_ObstacleGroups(); break;
            case 12: test12_SinglePath(); break;
            case 13: test13_MaxComplexity(); break;
            case 0:
                test1_TinyGrid();
                test2_SingleBlockage();
                test3_OneTunnel();
                test4_MultipleStores();
                test5_MultipleTunnels();
                test6_TrafficVariations();
                test7_Maze();
                test8_LargeComplex();
                test9_WideGrid();
                test10_TallGrid();
                test11_ObstacleGroups();
                test12_SinglePath();
                test13_MaxComplexity();
                break;
            default:
                System.out.println("Invalid choice. Running Test 1.");
                test1_TinyGrid();
        }
        
        scanner.close();
    }
}
