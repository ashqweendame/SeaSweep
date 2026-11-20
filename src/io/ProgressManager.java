package io;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

// Remembers how many levels have been cleared. The file holds a single number, e.g. "3".
// Level N is playable when N <= cleared + 1.
public class ProgressManager {
    private final String fileName;
    private final int maxLevel;

    public ProgressManager(String fileName, int maxLevel){
        this.fileName = fileName;
        this.maxLevel = maxLevel;
    }

    // missing, empty or garbled file = nothing cleared yet
    public int loadCleared(){
        File file = new File(fileName);
        if(!file.exists()) return 0;

        try(Scanner scanner = new Scanner(file)){
            if(scanner.hasNextLine()){
                int value = Integer.parseInt(scanner.nextLine().trim());
                return Math.max(0, Math.min(maxLevel, value));
            }
        }catch(Exception e){}
        return 0;
    }

    public void saveCleared(int cleared){
        int clean = Math.max(0, Math.min(maxLevel, cleared));
        try(PrintWriter writer = new PrintWriter(new File(fileName))){
            writer.println(clean);
        }catch(Exception e){}
    }
}