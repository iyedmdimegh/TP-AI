package code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DeliverySearch extends GenericSearch {

    static final class State {
        final int x;
        final int y;

        State(int x, int y) {
            this.x = x;
            this.y = y;
        }

        int manhattan(State other) {
            return Math.abs(x - other.x) + Math.abs(y - other.y);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof State)) {
                return false;
            }
            State other = (State) obj;
            return x == other.x && y == other.y;
        }


        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return x + "," + y;
        }
    }




    private final int m;
    private final int n;
    private final Map<String, Integer> trafficMap;
    private final Map<State, State> tunnels;
    private final int heuristicType;
    private State target;

    public DeliverySearch(int m, int n, Map<String, Integer> trafficMap, Map<State, State> tunnels, int heuristicType) {
        this.m = m;
        this.n = n;
        this.trafficMap = trafficMap;
        this.tunnels = tunnels;
        this.heuristicType = heuristicType;
    }

    public void setTarget(State target) {
        this.target = target;
    }

    @Override
    public boolean isGoal(Object state) {
        return state.equals(target);
    }

    @Override
    public double getHeuristic(Object state) {
        if (!(state instanceof State)) {
            return 0;
        }
        State current = (State) state;
        if (heuristicType == 1) {
            return Math.abs(current.x - target.x) + Math.abs(current.y - target.y);
        }
        if (heuristicType == 2) {
            int dx = current.x - target.x;
            int dy = current.y - target.y;
            return Math.sqrt(dx * dx + dy * dy);
        }
        return 0;
    }

    @Override
    public List<Node> expand(Node node) {
        List<Node> children = new ArrayList<>();
        State current = (State) node.state;

        int[][] directions = {{0, 1}, {0, -1}, {-1, 0}, {1, 0}};
        String[] actions = {"up", "down", "left", "right"};

        for (int i = 0; i < directions.length; i++) {
            int nx = current.x + directions[i][0];
            int ny = current.y + directions[i][1];

            if (nx >= 0 && nx < m && ny >= 0 && ny < n) {
                int cost = getTrafficCost(current.x, current.y, nx, ny);
                if (cost > 0) {
                    State nextState = new State(nx, ny);
                    children.add(new Node(nextState, node, actions[i], node.depth + 1, node.pathCost + cost, 0));
                }
            }
        }

        if (tunnels.containsKey(current)) {
            State exit = tunnels.get(current);
            int tunnelCost = current.manhattan(exit);
            children.add(new Node(exit, node, "tunnel", node.depth + 1, node.pathCost + tunnelCost, 0));
        }

        return children;
    }

    private int getTrafficCost(int x1, int y1, int x2, int y2) {
        String keyForward = x1 + "," + y1 + "," + x2 + "," + y2;
        String keyBackward = x2 + "," + y2 + "," + x1 + "," + y1;
        if (trafficMap.containsKey(keyForward)) {
            return trafficMap.get(keyForward);
        }
        if (trafficMap.containsKey(keyBackward)) {
            return trafficMap.get(keyBackward);
        }
        return 1;
    }

    public static String solve(String initialState, String traffic, String strategy, boolean visualize) {
        ParsedInput input = parseInitialState(initialState);
        Map<String, Integer> trafficMap = parseTraffic(traffic);

        int heuristicType = 0;
        if (strategy.endsWith("1")) {
            heuristicType = 1;
        } else if (strategy.endsWith("2")) {
            heuristicType = 2;
        }

        DeliverySearch agent = new DeliverySearch(input.m, input.n, trafficMap, input.tunnels, heuristicType);

        StringBuilder output = new StringBuilder();
        int deliveries = Math.min(input.customers.size(), input.packageCount);

        for (int i = 0; i < deliveries; i++) {
            State customer = input.customers.get(i);
            agent.setTarget(customer);

            int bestCost = Integer.MAX_VALUE;
            String bestResult = null;
            int bestStoreIndex = -1;

            // Iterate over all stores to find the one with the lowest cost path to the customer
            for (int storeIndex = 0; storeIndex < input.stores.size(); storeIndex++) {
                State store = input.stores.get(storeIndex);
                String result = agent.search(store, strategy);
                String[] parts = result.split(";");
                if (parts.length < 3 || "NoPath".equals(parts[0])) {
                    continue;
                }
                int cost = Integer.parseInt(parts[1]);
                if (cost < bestCost) {
                    bestCost = cost;
                    bestResult = result;
                    bestStoreIndex = storeIndex;
                }
            }

            if (output.length() > 0) {
                output.append(System.lineSeparator());
            }

            if (bestStoreIndex != -1 && bestResult != null) {
                output.append("(S").append(bestStoreIndex + 1).append(",D").append(i + 1).append("):")
                      .append(bestResult);
                if (visualize) {
                    System.out.println("Delivering from store " + (bestStoreIndex + 1) + " to customer " + (i + 1) + ": " + bestResult);
                }
            } else {
                output.append("(S?,D").append(i + 1).append("):")
                      .append("NoPath;0;0");
            }
        }

        return output.toString();
    }

    public static String GenGrid() {
        // Updated to match new format: m;n;P;S;Stores;Customers;Tunnels
        return "5;5;2;1;0,0;4,4,3,3;1,1,2,2";
    }

    private static ParsedInput parseInitialState(String initialState) {
        String[] sections = initialState.split(";");
        // Expecting at least: m;n;P;S;Stores...
        if (sections.length < 5) {
            throw new IllegalArgumentException("Invalid initial state format. Expected m;n;P;S;Stores;Customers;Tunnels");
        }

        int m = Integer.parseInt(sections[0]);
        int n = Integer.parseInt(sections[1]);
        int packageCount = Integer.parseInt(sections[2]);
        int storeCount = Integer.parseInt(sections[3]);

        // --- NEW FORMAT MAPPING ---
        // Section 4: Stores
        // Section 5: Customers
        // Section 6: Tunnels

        // 1. Parse Stores (Index 4)
        List<State> stores = new ArrayList<>();
        if (!sections[4].isEmpty()) {
            String[] storeTokens = sections[4].split(",");
            for (int i = 0; i + 1 < storeTokens.length; i += 2) {
                stores.add(new State(Integer.parseInt(storeTokens[i]), Integer.parseInt(storeTokens[i + 1])));
            }
        }

        // Fallback if stores are empty but S > 0 (Robustness)
        if (stores.isEmpty()) {
            stores.add(new State(0, 0));
            if (storeCount > 1) {
                stores.add(new State(Math.max(0, m - 1), 0));
            }
            if (storeCount > 2) {
                stores.add(new State(0, Math.max(0, n - 1)));
            }
        }

        // 2. Parse Customers (Index 5)
        List<State> customers = new ArrayList<>();
        if (sections.length > 5 && !sections[5].isEmpty()) {
            String[] coords = sections[5].split(",");
            for (int i = 0; i + 1 < coords.length; i += 2) {
                customers.add(new State(Integer.parseInt(coords[i]), Integer.parseInt(coords[i + 1])));
            }
        }

        // 3. Parse Tunnels (Index 6)
        Map<State, State> tunnels = new HashMap<>();
        if (sections.length > 6 && !sections[6].isEmpty()) {
            String[] tunnelTokens = sections[6].split(",");
            for (int i = 0; i + 3 < tunnelTokens.length; i += 4) {
                State entry = new State(Integer.parseInt(tunnelTokens[i]), Integer.parseInt(tunnelTokens[i + 1]));
                State exit = new State(Integer.parseInt(tunnelTokens[i + 2]), Integer.parseInt(tunnelTokens[i + 3]));
                tunnels.put(entry, exit);
                tunnels.put(exit, entry);
            }
        }

        return new ParsedInput(m, n, packageCount, storeCount, customers, stores, tunnels);
    }

    private static Map<String, Integer> parseTraffic(String traffic) {
        Map<String, Integer> trafficMap = new HashMap<>();
        if (traffic == null || traffic.trim().isEmpty()) {
            return trafficMap;
        }
        String[] lines = traffic.split(";");
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] tokens = line.split(",");
            if (tokens.length < 5) {
                continue;
            }
            String key = tokens[0] + "," + tokens[1] + "," + tokens[2] + "," + tokens[3];
            trafficMap.put(key, Integer.parseInt(tokens[4]));
        }
        return trafficMap;
    }

    private static final class ParsedInput {
        final int m;
        final int n;
        final int packageCount;
        final int storeCount;
        final List<State> customers;
        final List<State> stores;
        final Map<State, State> tunnels;

        ParsedInput(int m, int n, int packageCount, int storeCount, List<State> customers, List<State> stores, Map<State, State> tunnels) {
            this.m = m;
            this.n = n;
            this.packageCount = packageCount;
            this.storeCount = storeCount;
            this.customers = customers;
            this.stores = stores;
            this.tunnels = tunnels;
        }
    }
}