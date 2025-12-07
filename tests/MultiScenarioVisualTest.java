package tests;

import code.DeliveryPlanner;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.QuadCurve2D;
import java.util.*;
import java.util.List;

/**
 * Multiple Test Scenarios for Delivery Visualization
 * Uses DeliveryPlanner for optimal truck-to-package assignment
 */
public class MultiScenarioVisualTest extends JPanel {

    private final int m, n;
    private final List<Point> stores = new ArrayList<>();
    private final List<Point> customers = new ArrayList<>();
    private final Map<Point, Point> tunnels = new HashMap<>();
    private final Map<String, Integer> trafficMap = new HashMap<>();
    private final Map<Integer, List<Point>> paths = new HashMap<>();
    private final String scenarioName;

    private static final int GRID_SPACING = 70;
    private static final int PADDING = 50;

    public MultiScenarioVisualTest(String scenarioName, String initialState, String traffic, String solution) {
        this.scenarioName = scenarioName;

        String[] parts = initialState.split(";");
        this.m = Integer.parseInt(parts[0]);
        this.n = Integer.parseInt(parts[1]);

        if (parts.length > 4 && !parts[4].isEmpty()) {
            String[] sCoords = parts[4].split(",");
            for (int i = 0; i + 1 < sCoords.length; i += 2) {
                stores.add(new Point(Integer.parseInt(sCoords[i]), Integer.parseInt(sCoords[i + 1])));
            }
        }

        if (parts.length > 5 && !parts[5].isEmpty()) {
            String[] cCoords = parts[5].split(",");
            for (int i = 0; i + 1 < cCoords.length; i += 2) {
                customers.add(new Point(Integer.parseInt(cCoords[i]), Integer.parseInt(cCoords[i + 1])));
            }
        }

        if (parts.length > 6 && !parts[6].isEmpty()) {
            String[] tTokens = parts[6].split(",");
            for (int i = 0; i + 3 < tTokens.length; i += 4) {
                Point p1 = new Point(Integer.parseInt(tTokens[i]), Integer.parseInt(tTokens[i + 1]));
                Point p2 = new Point(Integer.parseInt(tTokens[i + 2]), Integer.parseInt(tTokens[i + 3]));
                tunnels.put(p1, p2);
                tunnels.put(p2, p1);
            }
        }

        String[] lines = traffic.split(";");
        for (String line : lines) {
            if (line.trim().isEmpty())
                continue;
            String[] t = line.split(",");
            if (t.length < 5)
                continue;
            String key = t[0] + "," + t[1] + "," + t[2] + "," + t[3];
            trafficMap.put(key, Integer.parseInt(t[4]));
            String revKey = t[2] + "," + t[3] + "," + t[0] + "," + t[1];
            trafficMap.put(revKey, Integer.parseInt(t[4]));
        }

        parseSolution(solution);

        int width = Math.max(600, m * GRID_SPACING + 2 * PADDING);
        int height = Math.max(500, n * GRID_SPACING + 2 * PADDING + 40);
        this.setPreferredSize(new Dimension(width, height));
        this.setBackground(Color.WHITE);
    }

    private void parseSolution(String solution) {
        String[] solLines = solution.split("\\n");
        for (String line : solLines) {
            if (!line.startsWith("(S"))
                continue;

            try {
                int commaIdx = line.indexOf(",");
                int parenEndIdx = line.indexOf(")");
                int colonIdx = line.indexOf(":");
                int semiIdx = line.indexOf(";");
                if (colonIdx == -1 || semiIdx == -1)
                    continue;

                int storeIdx = Integer.parseInt(line.substring(2, commaIdx)) - 1;
                int custIdx = Integer.parseInt(line.substring(commaIdx + 2, parenEndIdx)) - 1;
                String opsStr = line.substring(colonIdx + 1, semiIdx);

                if (opsStr.equals("NoPath") || opsStr.isEmpty())
                    continue;
                if (storeIdx >= stores.size())
                    continue;

                List<Point> pathPoints = new ArrayList<>();
                Point current = new Point(stores.get(storeIdx));
                pathPoints.add(new Point(current));

                String[] ops = opsStr.split(",");
                for (String op : ops) {
                    if (op.equals("up"))
                        current.y++;
                    else if (op.equals("down"))
                        current.y--;
                    else if (op.equals("right"))
                        current.x++;
                    else if (op.equals("left"))
                        current.x--;
                    else if (op.equals("tunnel") && tunnels.containsKey(current)) {
                        current = new Point(tunnels.get(current));
                    }
                    pathPoints.add(new Point(current));
                }
                paths.put(custIdx, pathPoints);
            } catch (Exception e) {
                System.err.println("Error parsing: " + line);
            }
        }
    }

    private Point toPixel(int x, int y) {
        return new Point(PADDING + x * GRID_SPACING, PADDING + 30 + (n - 1 - y) * GRID_SPACING);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.drawString(scenarioName, PADDING, 25);

        g2.setStroke(new BasicStroke(2));
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                Point p1 = toPixel(x, y);
                if (x + 1 < m)
                    drawEdge(g2, x, y, x + 1, y, p1, toPixel(x + 1, y));
                if (y + 1 < n)
                    drawEdge(g2, x, y, x, y + 1, p1, toPixel(x, y + 1));
            }
        }

        g2.setStroke(
                new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] { 6.0f }, 0.0f));
        g2.setColor(new Color(100, 50, 150));
        Set<String> drawn = new HashSet<>();
        for (Map.Entry<Point, Point> e : tunnels.entrySet()) {
            Point s = e.getKey(), end = e.getValue();
            String k = (s.x < end.x || (s.x == end.x && s.y < end.y)) ? s + "-" + end : end + "-" + s;
            if (drawn.contains(k))
                continue;
            drawn.add(k);
            Point p1 = toPixel(s.x, s.y), p2 = toPixel(end.x, end.y);
            QuadCurve2D q = new QuadCurve2D.Float();
            q.setCurve(p1.x, p1.y, (p1.x + p2.x) / 2 - (p2.y - p1.y) * 0.15,
                    (p1.y + p2.y) / 2 + (p2.x - p1.x) * 0.15, p2.x, p2.y);
            g2.draw(q);
            drawTunnelIcon(g2, p1);
            drawTunnelIcon(g2, p2);
        }

        g2.setStroke(new BasicStroke(4));
        for (Map.Entry<Integer, List<Point>> e : paths.entrySet()) {
            int idx = e.getKey();
            float hue = (float) idx / Math.max(1, customers.size());
            Color c = Color.getHSBColor(hue, 0.85f, 0.95f);
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 180));

            List<Point> path = e.getValue();
            for (int i = 0; i < path.size() - 1; i++) {
                Point cur = path.get(i), nxt = path.get(i + 1);
                Point px1 = toPixel(cur.x, cur.y), px2 = toPixel(nxt.x, nxt.y);
                if (Math.abs(cur.x - nxt.x) + Math.abs(cur.y - nxt.y) > 1) {
                    g2.setStroke(
                            new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[] { 7 }, 0));
                    g2.drawLine(px1.x, px1.y, px2.x, px2.y);
                    g2.setStroke(new BasicStroke(4));
                } else {
                    g2.drawLine(px1.x, px1.y, px2.x, px2.y);
                }
            }
        }

        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                Point p = toPixel(x, y);
                g2.setColor(new Color(80, 80, 80));
                g2.fillOval(p.x - 3, p.y - 3, 6, 6);
                g2.setColor(Color.LIGHT_GRAY);
                g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
                g2.drawString(x + "," + y, p.x + 6, p.y + 12);
            }
        }

        for (int i = 0; i < stores.size(); i++) {
            Point p = toPixel(stores.get(i).x, stores.get(i).y);
            g2.setColor(new Color(30, 100, 200));
            g2.fillRoundRect(p.x - 12, p.y - 12, 24, 24, 6, 6);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            String label = "S" + (i + 1);
            g2.drawString(label, p.x - g2.getFontMetrics().stringWidth(label) / 2, p.y + 5);
        }

        for (int i = 0; i < customers.size(); i++) {
            Point p = toPixel(customers.get(i).x, customers.get(i).y);
            g2.setColor(new Color(30, 160, 50));
            g2.fillOval(p.x - 12, p.y - 12, 24, 24);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            String label = "D" + (i + 1);
            g2.drawString(label, p.x - g2.getFontMetrics().stringWidth(label) / 2, p.y + 5);
        }
    }

    private void drawTunnelIcon(Graphics2D g2, Point p) {
        g2.setColor(new Color(100, 50, 150));
        g2.fillRect(p.x - 8, p.y - 10, 16, 20);
        g2.setColor(Color.YELLOW);
        g2.fillOval(p.x - 3, p.y - 1, 6, 6);
    }

    private void drawEdge(Graphics2D g2, int x1, int y1, int x2, int y2, Point p1, Point p2) {
        String key = x1 + "," + y1 + "," + x2 + "," + y2;
        Integer cost = trafficMap.get(key);
        int displayCost = (cost != null) ? cost : 1;

        if (cost != null && cost == 0) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(7));
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            int mx = (p1.x + p2.x) / 2, my = (p1.y + p2.y) / 2;
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString("✕", mx - 5, my + 5);
            g2.setStroke(new BasicStroke(2));
        } else {
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            if (displayCost > 1) {
                int mx = (p1.x + p2.x) / 2, my = (p1.y + p2.y) / 2;
                g2.setColor(new Color(200, 100, 50));
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                g2.drawString(String.valueOf(displayCost), mx + 3, my - 2);
            }
        }
    }

    private static String generateTraffic(int m, int n, long seed) {
        StringBuilder sb = new StringBuilder();
        Random rand = new Random(seed);
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                if (x + 1 < m) {
                    sb.append(x).append(",").append(y).append(",").append(x + 1).append(",").append(y)
                            .append(",").append(rand.nextInt(5) + 1).append(";");
                }
                if (y + 1 < n) {
                    sb.append(x).append(",").append(y).append(",").append(x).append(",").append(y + 1)
                            .append(",").append(rand.nextInt(5) + 1).append(";");
                }
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("   MULTI-SCENARIO VISUALIZATION TEST SUITE   ");
        System.out.println("   Using DeliveryPlanner for Optimization    ");
        System.out.println("==============================================\n");

        Scenario[] scenarios = {
                scenario1_SimpleTunnel(),
                scenario2_TunnelRequired(),
                scenario3_RiverCrossing()
        };

        for (int i = 0; i < scenarios.length; i++) {
            final int index = i;
            final Scenario s = scenarios[i];

            SwingUtilities.invokeLater(() -> {
                JFrame frame = new JFrame("Scenario " + (index + 1) + ": " + s.name);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                MultiScenarioVisualTest panel = new MultiScenarioVisualTest(s.name, s.state, s.traffic, s.solution);
                JScrollPane scroll = new JScrollPane(panel);

                frame.add(scroll);
                frame.pack();
                frame.setLocation(50 + index * 60, 50 + index * 60);
                frame.setVisible(true);
            });

            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
            }
        }
    }

    static Scenario scenario1_SimpleTunnel() {
        String name = "Simple 6x4 - Tunnel Shortcut";
        String state = "6;4;1;1;0,1;5,2;1,2,4,2";
        String traffic = generateTraffic(6, 4, 123);

        System.out.println("Solving: " + name);
        String solution = DeliveryPlanner.solve(state, traffic, "AS1", false);
        System.out.println("Solution: " + solution);

        return new Scenario(name, state, traffic, solution);
    }

    static Scenario scenario2_TunnelRequired() {
        String name = "Tunnel Required 8x5 - Expensive Direct Path";
        String state = "8;5;1;1;0,2;7,2;3,2,5,2";

        StringBuilder traffic = new StringBuilder();
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 5; y++) {
                if (x + 1 < 8) {
                    int cost = (y == 2 && x >= 3 && x < 5) ? 100 : 1;
                    traffic.append(x).append(",").append(y).append(",")
                            .append(x + 1).append(",").append(y).append(",")
                            .append(cost).append(";");
                }
                if (y + 1 < 5) {
                    traffic.append(x).append(",").append(y).append(",")
                            .append(x).append(",").append(y + 1).append(",1;");
                }
            }
        }

        System.out.println("Solving: " + name);
        System.out.println("  Note: Direct costs 200, tunnel costs 1");
        String solution = DeliveryPlanner.solve(state, traffic.toString(), "UC", false);
        System.out.println("Solution: " + solution);

        return new Scenario(name, state, traffic.toString(), solution);
    }

    static Scenario scenario3_RiverCrossing() {
        String name = "Complex Multi-Tunnel Network with Obstacles";
        // 5 packages, 1 store at (0,2), 5 customers, 3 tunnels
        String state = "7;5;5;1;0,2;6,2,6,4,0,4,3,0,3,4;1,2,5,2,2,1,2,3,4,0,4,4";

        StringBuilder traffic = new StringBuilder();
        for (int x = 0; x < 7; x++) {
            for (int y = 0; y < 5; y++) {
                if (x + 1 < 7) {
                    int cost = 1;

                    // Make horizontal travel VERY expensive from x=1 to x=5 at y=2
                    if (y == 2 && x >= 1 && x < 5) {
                        cost = 200;
                    }

                    // Block some roads to create obstacles
                    if ((x == 3 && y == 0) || (x == 5 && y == 4)) {
                        cost = 0; // Blocked roads
                    }

                    // Expensive detour areas
                    if (y == 1 && x >= 3 && x < 5) {
                        cost = 50;
                    }

                    // Extremely expensive road from (0,4) to (1,4)
                    if (x == 0 && y == 4) {
                        cost = 2000;
                    }
                    if (x == 1 && y == 4) {
                        cost = 2000;
                    }
                    if (x == 2 && y == 4) {
                        cost = 2000;
                    }
                    if (x == 2 && y == 3) {
                        cost = 2000;
                    }

                    traffic.append(x).append(",").append(y).append(",")
                            .append(x + 1).append(",").append(y).append(",")
                            .append(cost).append(";");
                }
                if (y + 1 < 5) {
                    int cost = 1;

                    // Block some vertical roads
                    if ((x == 3 && y == 2) || (x == 1 && y == 0)) {
                        cost = 0; // Blocked vertical roads
                    }

                    // Expensive vertical sections
                    if (x == 5 && y == 1) {
                        cost = 40;
                    }

                    traffic.append(x).append(",").append(y).append(",")
                            .append(x).append(",").append(y + 1).append(",")
                            .append(cost).append(";");
                }
            }
        }

        // Store at (0,2) - starting point
        // Customers at: (6,2), (6,4), (0,4), (3,0), (3,4)
        // Tunnel 1: (1,2) <-> (5,2) - main horizontal crossing
        // Tunnel 2: (2,1) <-> (2,3) - vertical shortcut
        // Tunnel 3: (4,0) <-> (4,4) - full height jump
        // Multiple blocked roads + 2000-cost road from (0,4) to (1,4)

        System.out.println("Solving: " + name);
        System.out.println("  5 customers, 3 tunnels, multiple obstacles + extremely expensive road");
        String solution = DeliveryPlanner.solve(state, traffic.toString(), "UC", false);
        System.out.println("Solution: " + solution);

        return new Scenario(name, state, traffic.toString(), solution);
    }

    static class Scenario {
        String name, state, traffic, solution;

        Scenario(String name, String state, String traffic, String solution) {
            this.name = name;
            this.state = state;
            this.traffic = traffic;
            this.solution = solution;
        }
    }
}
