package code;

public class DeliveryPlanner {

    /**
     * This class implements the planning of which trucks to send for each product.
     * Based on the updated requirements, the core logic for parsing and solving 
     * is encapsulated within the static DeliverySearch.solve() method.
     * * This class acts as a high-level entry point or wrapper as requested 
     * in the initial project description.
     */
    
    public static String solve(String initialState, String traffic, String strategy, boolean visualize) {
        // Delegates the planning and searching to the mandatory static method in DeliverySearch
        return DeliverySearch.solve(initialState, traffic, strategy, visualize);
    }

    /**
     * Main method for standalone testing of the planner.
     */
    public static void main(String[] args) {
        // Example Input
        // Updated to match the new format: m;n;P;S;Stores;Customers;Tunnels
        // Grid: 5x5
        // P: 1 package
        // S: 1 store
        // Stores: (0,0)  <-- Added explicit store location
        // Customers: (1,4) <-- Moved to correct slot
        // Tunnels: (None)
        String state = "5;5;1;1;0,0;1,4;"; 
        
        String traffic = "0,0,0,1,1;0,1,0,2,2;0,2,0,3,1;0,3,1,3,1;1,3,1,4,1;";
        String strategy = "BF";

        System.out.println("--- DeliveryPlanner Executing ---");
        String plan = solve(state, traffic, strategy, true);
        System.out.println("Final Plan: " + plan);
    }
}