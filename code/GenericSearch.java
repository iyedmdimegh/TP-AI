package code;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

public abstract class GenericSearch {

    public abstract boolean isGoal(Object state);

    public abstract List<Node> expand(Node node);

    public abstract double getHeuristic(Object state);

    public String search(Object initialState, String strategy) {
        if ("ID".equals(strategy)) {
            return iterativeDeepening(initialState);
        }

        Collection<Node> frontier;
        if ("BF".equals(strategy) || "DF".equals(strategy)) {
            frontier = new LinkedList<>();
        } else {
            frontier = new PriorityQueue<>();
        }

        double heuristicValue = requiresHeuristic(strategy) ? getHeuristic(initialState) : 0;
        Node root = new Node(initialState, null, null, 0, 0, heuristicValue);
        addToFrontier(frontier, root, strategy);

        int nodesExpanded = 0;
        Set<String> visited = new HashSet<>();

        while (!frontier.isEmpty()) {
            Node node = removeFromFrontier(frontier, strategy);
            if (node == null) {
                break;
            }

            if (isGoal(node.state)) {
                return reconstructPath(node, nodesExpanded);
            }

            String stateKey = node.state.toString();
            if (visited.contains(stateKey)) {
                continue;
            }
            visited.add(stateKey);

            nodesExpanded++;

            List<Node> successors = expand(node);
            for (Node child : successors) {
                if (requiresHeuristic(strategy)) {
                    child.heuristic = getHeuristic(child.state);
                } else {
                    child.heuristic = 0;
                }

                if (!visited.contains(child.state.toString())) {
                    addToFrontier(frontier, child, strategy);
                }
            }
        }

        return "NoPath;0;" + nodesExpanded;
    }

    private boolean requiresHeuristic(String strategy) {
        return strategy.startsWith("GR") || strategy.startsWith("AS");
    }

    private String iterativeDeepening(Object initialState) {
        int depthLimit = 0;
        while (depthLimit <= 10000) {
            SearchResult result = depthLimitedSearch(initialState, depthLimit);
            if (!result.cutoff) {
                return result.solution != null ? result.solution : "NoPath;0;" + result.expanded;
            }
            depthLimit++;
        }
        return "NoPath;0;0";
    }

    private SearchResult depthLimitedSearch(Object start, int limit) {
        LinkedList<Node> stack = new LinkedList<>();
        stack.push(new Node(start, null, null, 0, 0, 0));
        int expanded = 0;
        boolean cutoff = false;

        while (!stack.isEmpty()) {
            Node node = stack.pop();

            if (isGoal(node.state)) {
                return new SearchResult(reconstructPath(node, expanded), expanded, false);
            }

            if (node.depth >= limit) {
                cutoff = true;
                continue;
            }

            expanded++;
            List<Node> successors = expand(node);
            Collections.reverse(successors);
            for (Node child : successors) {
                if (!repeatsInPath(child)) {
                    stack.push(child);
                }
            }
        }

        return new SearchResult(null, expanded, cutoff);
    }

    private boolean repeatsInPath(Node node) {
        Object currentState = node.state;
        Node cursor = node.parent;
        while (cursor != null) {
            if (cursor.state.equals(currentState)) {
                return true;
            }
            cursor = cursor.parent;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void addToFrontier(Collection<Node> frontier, Node node, String strategy) {
        if ("DF".equals(strategy)) {
            ((LinkedList<Node>) frontier).push(node);
        } else if ("BF".equals(strategy)) {
            ((LinkedList<Node>) frontier).add(node);
        } else {
            frontier.add(node);
        }
    }

    @SuppressWarnings("unchecked")
    private Node removeFromFrontier(Collection<Node> frontier, String strategy) {
        if ("DF".equals(strategy) || "BF".equals(strategy)) {
            return ((LinkedList<Node>) frontier).pollFirst();
        }
        return ((PriorityQueue<Node>) frontier).poll();
    }

    private String reconstructPath(Node node, int expanded) {
        StringBuilder plan = new StringBuilder();
        double cost = node.pathCost;
        while (node.parent != null) {
            if (plan.length() > 0) {
                plan.insert(0, ",");
            }
            plan.insert(0, node.operator);
            node = node.parent;
        }
        return plan + ";" + (int) cost + ";" + expanded;
    }

    private static class SearchResult {
        final String solution;
        final int expanded;
        final boolean cutoff;

        SearchResult(String solution, int expanded, boolean cutoff) {
            this.solution = solution;
            this.expanded = expanded;
            this.cutoff = cutoff;
        }
    }
}
