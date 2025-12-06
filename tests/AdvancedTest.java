package tests;

import code.DeliverySearch;

public class AdvancedTest {

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("       RUNNING COMPREHENSIVE TESTS        ");
        System.out.println("==========================================\n");

        test1_SimplePath();
        test2_BlockedRoad();
        test3_TunnelShortCut();
        test4_OptimalityCheck();
    }

    // TEST 1: Basic functionality check
    // A simple 3x3 grid, moving from (0,0) to (0,2) straight line.
    static void test1_SimplePath() {
        System.out.println("Test 1: Simple Linear Path...");
        String state = "3;3;1;1;0,2;"; // Customer at 0,2
        String traffic = "0,0,0,1,1;0,1,0,2,1;"; // Cost 1 per step
        
        String result = DeliverySearch.solve(state, traffic, "BF", false);
        
        // Expecting cost 2 (0,0->0,1->0,2)
        if (result.contains(";2;")) {
            System.out.println(" [PASS] Found correct cost 2.");
        } else {
            System.out.println(" [FAIL] Expected cost 2, got: " + result);
        }
        System.out.println("------------------------------------------");
    }

    // TEST 2: Blocked Road (Traffic = 0)
    // 0,0 -> 0,1 is blocked. Must go around: 0,0 -> 1,0 -> 1,1 -> 0,1
    static void test2_BlockedRoad() {
        System.out.println("Test 2: Blocked Road Handling...");
        String state = "2;2;1;1;0,1;"; // Dest 0,1
        // 0,0->0,1 is cost 0 (BLOCKED)
        // Others are open cost 1
        String traffic = "0,0,0,1,0;0,0,1,0,1;1,0,1,1,1;1,1,0,1,1;"; 
        
        String result = DeliverySearch.solve(state, traffic, "BF", false);
        
        // Path should be right, up, left (cost 3)
        if (result.contains(";3;")) {
            System.out.println(" [PASS] Agent successfully bypassed blockage.");
        } else {
            System.out.println(" [FAIL] Failed to bypass blockage. Result: " + result);
        }
        System.out.println("------------------------------------------");
    }

    // TEST 3: Tunnel usage
    // Grid 10x10. Walking is far. Tunnel connects (0,0) directly to (0,8).
    static void test3_TunnelShortCut() {
        System.out.println("Test 3: Tunnel Shortcut...");
        // Tunnels defined at end of state string: 0,0 connects to 0,8
        String state = "10;10;1;1;0,9;0,0,0,8"; 
        // No specific traffic defined, defaults to 1.
        String traffic = ""; 
        
        // BFS might walk all the way, but cost logic should see tunnel.
        // Tunnel cost = Manhattan(0,0 -> 0,8) = 8.
        // Walking 0,0 to 0,8 is 8 steps. 
        // Wait, let's make the tunnel VERY fast.
        // Actually, assignment says Tunnel Cost = Manhattan Distance.
        // So Tunnel 0,0->0,8 costs 8. Walking costs 8.
        // Let's create a wall so tunnel is the ONLY way or specifically faster.
        
        // Better Test: Tunnel (0,0) to (5,5). Manhattan cost 10.
        // Walking (0,0) -> (5,5) with traffic jam (cost 5 per road).
        // If traffic is heavy, tunnel (cost 10) is better than road (cost 50).
        
        state = "6;6;1;1;5,5;0,0,5,5"; // Tunnel 0,0 -> 5,5
        // Heavy traffic everywhere
        traffic = "0,0,0,1,10;0,0,1,0,10;"; // Just define start edges as expensive
        
        String result = DeliverySearch.solve(state, traffic, "UC", false);
        
        if (result.contains("tunnel")) {
            System.out.println(" [PASS] Agent used the tunnel.");
        } else {
            System.out.println(" [FAIL] Agent ignored tunnel. Result: " + result);
        }
        System.out.println("------------------------------------------");
    }

    // TEST 4: Optimality (A* vs BFS)
    // BFS finds shortest path by *steps*. A* finds shortest by *cost*.
    // Path A: 2 steps, but cost 100.
    // Path B: 5 steps, but cost 5.
    static void test4_OptimalityCheck() {
        System.out.println("Test 4: Optimality (A* vs BFS)...");
        // 5x5 Grid. Dest at 2,0. Start 0,0.
        String state = "5;5;1;1;2,0;";
        
        // Path 1 (Short steps, High cost): 0,0 -> 1,0 -> 2,0. (Step 0->1 cost 50, 1->2 cost 50). Total 100.
        // Path 2 (Long steps, Low cost): 0,0 -> 0,1 -> 0,2 -> 1,2 -> 2,2 -> 2,1 -> 2,0 (Cost 1 each). Total 6.
        String traffic = "0,0,1,0,50;1,0,2,0,50;"; 
        
        String resBF = DeliverySearch.solve(state, traffic, "BF", false);
        String resAS = DeliverySearch.solve(state, traffic, "AS1", false);
        
        int costBF = Integer.parseInt(resBF.split(";")[1]);
        int costAS = Integer.parseInt(resAS.split(";")[1]);
        
        System.out.println(" Cost BFS: " + costBF);
        System.out.println(" Cost A*:  " + costAS);
        
        if (costAS < costBF) {
            System.out.println(" [PASS] A* found a cheaper path than BFS.");
        } else {
            System.out.println(" [FAIL] A* failed to optimize cost.");
        }
        System.out.println("------------------------------------------");
    }
}