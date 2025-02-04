// Tony Mazich, Kevin Jimenez
// CS1400
// Assignment 6: Disease Modeling
// Due November 12th, 2024

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.Scanner;

public class DM {
    // Just for simplicity's sake, we used a ton of static variables. Not the cleanest, but it works!
    static int numSusceptible;
    static int numInfected;
    static int numRecovered;
    static double infectedToTotalRatio;
    static double delay = 0.10;
    static int time = 0;
    static int timeSpan = 100;
    static double infectionRate = 0.0;
    static double recoveryRate = 0.0;
    static int gridSize = 5;
    static Individual[][] grid;
    static Individual[][] prevGrid;
    static Random random = new Random();

////////////////////////////////////////// Classes //////////////////////////////////////////

    // Class txtColor: Contains ANSI escape codes for text color in the console. This is used to color-code the output for better readability.
    public class txtColor {
        public static final String RESET = "\033[0m";
        public static final String RED = "\033[0;31m"; 
        public static final String GREEN = "\033[0;32m"; 
        public static final String BLUE = "\033[0;34m"; 
    }

    // class Individual: Contains the blueprint for the Individual object, defines all the fields and methods for handling individuals and their interactions.
    static class Individual {
        char status; // 'S' for Susceptible, 'I' for Infected, 'R' for Recovered

        public Individual() {
            this.status = 'S';
        }

        public Individual(char status) {
            this.status = status;
        }

        // Copy constructor
        public Individual(Individual other) {
            this.status = other.status;
        }

        // Method findInfectedNeighborCount: For each non-diagonally adjacent neighbor, check if they are infected
        public int findInfectedNeighborCount(int xCoord, int yCoord) {
            int numInfectedNeighbors = 0;

            // We check the four adjacencies of the individual, but only if they're within the bounds of the grid. This handles both corner and boundary cases.
            if (xCoord - 1 >= 0) { // Left neighbor
                if (prevGrid[xCoord - 1][yCoord].status == 'I') {
                    numInfectedNeighbors++;
                }
            }

            if (xCoord + 1 < gridSize) { // Right neighbor
                if (prevGrid[xCoord + 1][yCoord].status == 'I') {
                    numInfectedNeighbors++;
                }
            }

            if (yCoord - 1 >= 0) { // Bottom neighbor
                if (prevGrid[xCoord][yCoord - 1].status == 'I') {
                    numInfectedNeighbors++;
                }
            }

            if (yCoord + 1 < gridSize) { // Top neighbor
                if (prevGrid[xCoord][yCoord + 1].status == 'I') {
                    numInfectedNeighbors++;
                }
            }

            return numInfectedNeighbors;
        }
    } // end of class Individual

    ////////////////////////////////////////// Methods //////////////////////////////////////////

    // initializeGrids: Method to initialize the grid and prevGrid with individuals. Sets one individual as infected (patient zero).
    public static void initializeGrids() {
        grid = new Individual[gridSize][gridSize];
        prevGrid = new Individual[gridSize][gridSize];

        numSusceptible = gridSize * gridSize - 1; // All but one are susceptible
        numInfected = 1; // One infected individual
        numRecovered = 0; // No recovered individuals initially

        // Initialize the grid with individual objects.
        for (int yCoord = 0; yCoord < gridSize; yCoord++) {
            for (int xCoord = 0; xCoord < gridSize; xCoord++) {
                grid[yCoord][xCoord] = new Individual();
                prevGrid[yCoord][xCoord] = new Individual();
            }
        }

        // Set a random individual as infected (patient zero)
        int patientZeroxCoord = random.nextInt(gridSize);
        int patientZeroyCoord = random.nextInt(gridSize);
        grid[patientZeroxCoord][patientZeroyCoord].status = 'I';
        prevGrid[patientZeroxCoord][patientZeroyCoord].status = 'I';

    }

    // copyCoordGrid: Copies statuses of each individual from the current grid to the previous grid. Used to calculate statuses for the next time step.
    public static void copyCoordGrid() { 
        for (int yCoord = 0; yCoord < gridSize; yCoord++) {
            for (int xCoord = 0; xCoord < gridSize; xCoord++) {
                prevGrid[yCoord][xCoord].status = grid[yCoord][xCoord].status;
            }
        }
    }

    // sendToFile: Method to write the number of susceptible, infected, and recovered individuals to a file.
    public static void sendStatsToFile() {
        try {
            FileWriter fw = new FileWriter("diseaseStats.csv", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);

            out.println(time + "," + numSusceptible + "," + numInfected + "," + numRecovered + "," + infectedToTotalRatio);

            out.close();
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the file.");
            e.printStackTrace();
        }
    }

    // printGrid: Method to print the grid to the console. Utilizes txtColor class to color-code the output.
    public static void printGrid(Individual[][] grid) {

        StringBuilder sb = new StringBuilder();
        for (int yCoord = 0; yCoord < gridSize; yCoord++) {
            for (int xCoord = 0; xCoord < gridSize; xCoord++) {
                if (grid[yCoord][xCoord].status == 'S') {
                    sb.append(txtColor.BLUE).append(grid[yCoord][xCoord].status).append(txtColor.RESET).append("  ");
                } else if (grid[yCoord][xCoord].status == 'I') {
                    sb.append(txtColor.RED).append(grid[yCoord][xCoord].status).append(txtColor.RESET).append("  ");
                } else {
                    sb.append(txtColor.GREEN).append(grid[yCoord][xCoord].status).append(txtColor.RESET).append("  ");
                }
            }
            sb.append("\n");
        }

        sb.append("Time Step: ").append(time).append("\n");
        sb.append("Number of Susceptible: ").append(numSusceptible).append("\n");
        sb.append("Number of Infected: ").append(numInfected).append("\n");
        sb.append("Number of Recovered: ").append(numRecovered).append("\n");
        sb.append("Infected to Total Ratio: ").append(infectedToTotalRatio).append("\n");
        System.out.print(sb.toString());

        sendStatsToFile();
    }

    // runTimeStep: Method to run a single time step of the simulation. This method is called repeatedly in the runSimulation method.
    public static void runTimeStep() {
        // For each individual in the grid, calculate its new status based on the previous states of its neighbors.
        for (int yCoord = 0; yCoord < gridSize; yCoord++) {
            for (int xCoord = 0; xCoord < gridSize; xCoord++) {
                int numInfectedNeighbors = grid[yCoord][xCoord].findInfectedNeighborCount(yCoord, xCoord);
                double infectionProbability = (double)numInfectedNeighbors * infectionRate;
    
                // If the individual is susceptible, calculate the probability of infection and update its status.
                if (grid[yCoord][xCoord].status == 'S') {
                    if (random.nextDouble() < infectionProbability) {
                        grid[yCoord][xCoord].status = 'I';
                        numSusceptible--;
                        numInfected++;
                    }
                } else if (grid[yCoord][xCoord].status == 'I') {
                    // If the individual is infected, calculate the probability of recovery and update its status.
                    if (random.nextDouble() < recoveryRate) {
                        grid[yCoord][xCoord].status = 'R';
                        numInfected--;
                        numRecovered++;
                    }
                }
            }
        }
    
        infectedToTotalRatio = (double)numInfected / (gridSize * gridSize);
        time++;
    }

    // runSimulation: Contains the algorithm for all the simulation steps, including calling copyCoordGrid, runTimeStep, and printing the grid. Loops as many times as specified by user via timeSpan.
    public static void runSimulation() {

        for (int i = 0; i < timeSpan; i++) {
            copyCoordGrid();
            runTimeStep();
    
            clearScreen();
            printGrid(grid);
            
            try {
                Thread.sleep((long)(delay * 1000)); // delay is converted from seconds to milliseconds.
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Thread was interrupted, Failed to complete sleep");
            }
        }
    }

    // clearScreen: Method to clear the console screen. Uses ANSI escape code to clear the previous grid for a continuous visual.
    public static void clearScreen() {  
        System.out.print("\033[H\033[J");
        System.out.flush();  
    }  
    

    ////////////////////////////////////////// Main Program //////////////////////////////////////////
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // User inputs
        System.out.print("Enter grid size (For example, 25 will create a 25x25 grid): ");
        gridSize = scanner.nextInt();
        System.out.print("Enter the number of time steps to run the simulation for: ");
        timeSpan = scanner.nextInt();
        System.out.print("Enter the infection rate for this run (Between 0 and 1): ");
        infectionRate = scanner.nextDouble();
        System.out.print("Enter the recovery rate for this run (Between 0 and 1): ");
        recoveryRate = scanner.nextDouble();
        System.out.print("Enter the delay between time steps in seconds: ");
        delay = scanner.nextDouble();

        initializeGrids();
        runSimulation();
    }
}