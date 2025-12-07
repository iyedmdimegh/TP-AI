package Benchmark;

import code.DeliverySearch;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Random;
import java.util.concurrent.*;

public class benching1 {

    public static void main(String[] args) {
        System.out.println("===================================================================================");
        System.out.println("                         SEARCH ALGORITHM BENCHMARK                               ");
        System.out.println("===================================================================================");
        System.out.printf("%-10s | %-15s | %-12s | %-15s | %-15s | %-15s%n", 
                "Strategy", "Map Size", "Time (ms)", "Nodes Exp.", "RAM (MB)", "CPU (µs)");
        System.out.println("-----------------------------------------------------------------------------------");

        // Define Scenarios
        runSuite("Small (5x5)", 5, 5, 123);
        runSuite("Medium (10x10)", 10, 10, 456);
        runSuite("Large (20x20)", 20, 20, 789);
        
        // Force exit to ensure thread pool doesn't hang application if tasks are stuck
        System.exit(0);
    }

    private static void runSuite(String label, int m, int n, long seed) {
        String[] strategies = {"BF", "DF", "ID", "UC", "GR1", "GR2", "AS1", "AS2"};
        
        String state = generateState(m, n);
        String traffic = generateTraffic(m, n, seed);

        for (String strategy : strategies) {
            runTest(label, strategy, state, traffic);
        }
        System.out.println("-----------------------------------------------------------------------------------");
    }

    private static void runTest(String label, String strategy, String state, String traffic) {
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        ExecutorService executor = Executors.newSingleThreadExecutor();
        long startTime = System.nanoTime();

        try {
            // Run the algorithm in a separate thread to support timeout
            Future<BenchResult> future = executor.submit(() -> {
                ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                // Capture metrics inside the worker thread for accuracy
                long startCpu = bean.isCurrentThreadCpuTimeSupported() ? bean.getCurrentThreadCpuTime() : 0;
                long startMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                
                String result = DeliverySearch.solve(state, traffic, strategy, false);
                
                long endMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                long endCpu = bean.isCurrentThreadCpuTimeSupported() ? bean.getCurrentThreadCpuTime() : 0;
                
                return new BenchResult(result, startMem, endMem, startCpu, endCpu);
            });

            // Wait up to 5000ms
            BenchResult metrics = future.get(5000, TimeUnit.MILLISECONDS);
            
            long endTime = System.nanoTime();

            // Calculate differences
            double timeMs = (endTime - startTime) / 1_000_000.0;
            double cpuMicros = (metrics.endCpu - metrics.startCpu) / 1_000.0; // Nanoseconds to Microseconds
            double memUsedMb = Math.max(0, (metrics.endMem - metrics.startMem) / (1024.0 * 1024.0)); 

            // Parse nodes expanded
            int nodesExpanded = parseNodesExpanded(metrics.result);

            System.out.printf("%-10s | %-15s | %-12.2f | %-15d | %-15.4f | %-15.2f%n", 
                    strategy, label, timeMs, nodesExpanded, memUsedMb, cpuMicros);

        } catch (TimeoutException e) {
            System.out.printf("%-10s | %-15s | %-12s | %-15s | %-15s | %-15s%n", 
                    strategy, label, "> 5000", "TIMEOUT", "-", "-");
        } catch (Exception e) {
            System.out.printf("%-10s | %-15s | %-12s | %-15s | %-15s | %-15s%n", 
                    strategy, label, "ERROR", "ERROR", "-", "-");
        } finally {
            executor.shutdownNow(); // Kill the thread
        }
    }

    private static int parseNodesExpanded(String result) {
        int nodesExpanded = 0;
        if (result != null && result.contains(";")) {
            try {
                String[] lines = result.split("\n");
                for(String line : lines) {
                    if(line.contains(";")) {
                        String[] parts = line.split(";");
                        if (parts.length >= 3) {
                             nodesExpanded += Integer.parseInt(parts[2].trim());
                        }
                    }
                }
            } catch (Exception e) {
                return -1;
            }
        }
        return nodesExpanded;
    }

    // --- Helpers ---

    private static String generateState(int m, int n) {
        return m + ";" + n + ";1;1;0,0;" + (m-1) + "," + (n-1) + ";"; 
    }

    private static String generateTraffic(int m, int n, long seed) {
        StringBuilder sb = new StringBuilder();
        Random rand = new Random(seed);
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n; y++) {
                if (x + 1 < m) {
                    sb.append(x).append(",").append(y).append(",")
                      .append(x + 1).append(",").append(y).append(",")
                      .append(rand.nextInt(5) + 1).append(";");
                }
                if (y + 1 < n) {
                    sb.append(x).append(",").append(y).append(",")
                      .append(x).append(",").append(y + 1).append(",")
                      .append(rand.nextInt(5) + 1).append(";");
                }
            }
        }
        return sb.toString();
    }
    
    // Wrapper for returning metrics from the thread
    private static class BenchResult {
        String result;
        long startMem, endMem, startCpu, endCpu;
        public BenchResult(String r, long sm, long em, long sc, long ec) {
            this.result = r; this.startMem = sm; this.endMem = em; this.startCpu = sc; this.endCpu = ec;
        }
    }
}