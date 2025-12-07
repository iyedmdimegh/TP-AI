package code;

public class DeliveryPlanner {

    // Tiny wrapper so we punt all the heavy lifting to DeliverySearch.solve()
    public static String solve(String initialState, String traffic, String strategy, boolean visualize) {
        // pass prams here
        return DeliverySearch.solve(initialState, traffic, strategy, visualize);
    }

    public static void main(String[] args) {
        // run using the newer m;n;P;S;Stores;Customers;Tunnels layout
        // Grid: 5x5
        // P: 1 package
        // S: 1 store
        // Stores: (0,0)  
        // Customers: (1,4) 
        // Tunnels: (None)
        String state = "5;5;1;1;0,0;1,4;"; 
        
        String traffic = "0,0,0,1,1;0,1,0,2,2;0,2,0,3,1;0,3,1,3,1;1,3,1,4,1;";
        String strategy = "BF";

        System.out.println("--- DeliveryPlanner Executing ---");
        String plan = solve(state, traffic, strategy, true);
        System.out.println("Final Plan: " + plan);
    }
}