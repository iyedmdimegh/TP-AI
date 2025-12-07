package tests;

import code.DeliverySearch;
import java.util.HashMap;
import java.util.Map;

public class AdvancedTest {

	public static void main(String[] args) {
		System.out.println("==========================================");
		System.out.println("       RUNNING COMPREHENSIVE TESTS        ");
		System.out.println("==========================================\n");

		test1_SimplePath();
		test2_BlockedRoad();
		test3_TunnelShortCut();
		test4_OptimalityCheck();
		test5_MultipleStores();
	}

	// quick sanity check: store at 0,0, cust at 0,2, uniform cost grid.
	static void test1_SimplePath() {
		System.out.println("Test 1: Simple Linear Path...");

		String state = "3;3;1;1;0,0;0,2;";
		String traffic = "0,0,0,1,1;0,1,0,2,1;";

		String result = DeliverySearch.solve(state, traffic, "BF", false);

		// expect cost 2 (0,0 -> 0,1 -> 0,2); otherwise we goofed somwhere.
		if (result.contains(";2;")) {
			System.out.println(" [PASS] Found correct cost 2.");
		} else {
			System.out.println(" [FAIL] Expected cost 2, got: " + result);
		}
		System.out.println("------------------------------------------");
	}

	// block one edge so the agent has to wiggle around.
	static void test2_BlockedRoad() {
		System.out.println("Test 2: Blocked Road Handling...");

		String state = "2;2;1;1;0,0;0,1;";
		String traffic = "0,0,0,1,0;0,0,1,0,1;1,0,1,1,1;1,1,0,1,1;";

		String result = DeliverySearch.solve(state, traffic, "BF", false);

		// we expect the detour (right, up, left) with total cost 3.
		if (result.contains(";3;")) {
			System.out.println(" [PASS] Agent successfully bypassed blockage.");
		} else {
			System.out.println(" [FAIL] Failed to bypass blockage. Result: " + result);
		}
		System.out.println("------------------------------------------");
	}

	// make sure tunnels beat the slog when roads get pricy.
	static void test3_TunnelShortCut() {
		System.out.println("Test 3: Tunnel Shortcut...");

		String state = "6;6;1;1;0,0;5,5;0,0,5,5";
		String traffic = "0,0,0,1,10;0,0,1,0,10;";

		String result = DeliverySearch.solve(state, traffic, "UC", false);

		if (result.contains("tunnel")) {
			System.out.println(" [PASS] Agent used the tunnel.");
		} else {
			System.out.println(" [FAIL] Agent ignored tunnel. Result: " + result);
		}
		System.out.println("------------------------------------------");
	}

	// let A* flex by picking the cheaper (not just shorter) trip.
	static void test4_OptimalityCheck() {
		System.out.println("Test 4: Optimality (A* vs BFS)...");

		String state = "5;5;1;1;0,0;2,0;";
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

	// wishlist: once store data is sane, the solver should favor the closer shop.
	static void test5_MultipleStores() {
		System.out.println("Test 5: Multiple Stores (Choosing the Closest)...");

		String state = "5;5;2;2;0,0,4,4;2,2,3,3;";
		String traffic = "";

		String result = DeliverySearch.solve(state, traffic, "BF", false);
		System.out.println(" Output: " + result);

		if (result.contains("(S3,D1)")) {
			System.out.println(" [PASS] Selected Store 3 (closest).");
			visualizePath(state, result);
		} else {
			System.out.println(" [FAIL] Did not select Store 3. Result: " + result);
		}
		System.out.println("------------------------------------------");
	}

	// dump the path coordinates for eyeballing; sorry for the messy loops.
	private static void visualizePath(String stateStr, String resultStr) {
		System.out.println(" Path Coordinates:");
		String[] lines = resultStr.split("\n");
		String[] stateParts = stateStr.split(";");

		String[] storeTokens = stateParts[4].split(",");

		Map<String, String> tunnelMap = new HashMap<>();
		if (stateParts.length > 6 && !stateParts[6].isEmpty()) {
			String[] tTokens = stateParts[6].split(",");
			for (int i = 0; i < tTokens.length; i += 4) {
				String entry = tTokens[i] + "," + tTokens[i + 1];
				String exit = tTokens[i + 2] + "," + tTokens[i + 3];
				tunnelMap.put(entry, exit);
				tunnelMap.put(exit, entry);
			}
		}

		for (String line : lines) {
			if (line.startsWith("(S")) {
				int commaIdx = line.indexOf(",");
				int storeId = Integer.parseInt(line.substring(2, commaIdx));
				int storeIdx = storeId - 1;

				int currX = Integer.parseInt(storeTokens[storeIdx * 2]);
				int currY = Integer.parseInt(storeTokens[storeIdx * 2 + 1]);

				System.out.print("  Store " + storeId + ": (" + currX + "," + currY + ")");

				int colonIdx = line.indexOf(":");
				int semiIdx = line.indexOf(";");
				if (colonIdx != -1 && semiIdx != -1) {
					String actionsStr = line.substring(colonIdx + 1, semiIdx);
					if (!actionsStr.isEmpty() && !actionsStr.equals("NoPath")) {
						String[] actions = actionsStr.split(",");
						for (String action : actions) {
							if (action.equals("up")) {
								currY++;
							} else if (action.equals("down")) {
								currY--;
							} else if (action.equals("right")) {
								currX++;
							} else if (action.equals("left")) {
								currX--;
							} else if (action.equals("tunnel")) {
								String key = currX + "," + currY;
								if (tunnelMap.containsKey(key)) {
									String dest = tunnelMap.get(key);
									String[] dCoords = dest.split(",");
									currX = Integer.parseInt(dCoords[0]);
									currY = Integer.parseInt(dCoords[1]);
								}
							}
							System.out.print(" -> (" + currX + "," + currY + ")");
						}
					}
				}
				System.out.println();
			}
		}
	}
}

