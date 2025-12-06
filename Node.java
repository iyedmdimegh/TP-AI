package code;

public class Node implements Comparable<Node> {
    public Object state;
    public Node parent;
    public String operator;
    public int depth;
    public double pathCost;
    public double heuristic;

    public Node(Object state, Node parent, String operator, int depth, double pathCost, double heuristic) {
        this.state = state;
        this.parent = parent;
        this.operator = operator;
        this.depth = depth;
        this.pathCost = pathCost;
        this.heuristic = heuristic;
    }

    @Override
    public int compareTo(Node other) {
        double f1 = pathCost + heuristic;
        double f2 = other.pathCost + other.heuristic;
        return Double.compare(f1, f2);
    }

    @Override
    public String toString() {
        return "State: " + state + ", Op: " + operator + ", Cost: " + pathCost;
    }
}
