package io;
import java.util.*;
import java.io.*;

public class HighScoreManager {
    private final String FILE_NAME = "highscore.txt";

    public int loadHighScore(){
        File file = new File(FILE_NAME);
        if(file.exists()){
            try (Scanner scanner = new Scanner(file)){
                if(scanner.hasNextInt()){
                    return scanner.nextInt();
                }
            } catch (Exception e){}
        }
        return 0;
    }

    public void saveIfRecords(int finalScore){
        int currentRecord = loadHighScore();
        if(finalScore > currentRecord){
            try(PrintWriter writer = new PrintWriter(new File(FILE_NAME))){
                writer.print(finalScore);
            }catch (Exception e){}
        }
    }
}
