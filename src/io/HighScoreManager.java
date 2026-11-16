package io;

import model.ScoreEntry;
import java.io.*;
import java.util.*;

public class HighScoreManager {
    private final int MAX_STORED = 10;
    private final String fileName;

    // every mode keeps its own file, e.g. new HighScoreManager("highscore-levels.txt")
    public HighScoreManager(String fileName){
        this.fileName = fileName;
    }

    // Every line in the file looks like:  NAME,SCORE
    public List<ScoreEntry> loadScores(){
        List<ScoreEntry> scores = new ArrayList<>();
        File file = new File(fileName);

        if(file.exists()){
            try(Scanner scanner = new Scanner(file)){
                while(scanner.hasNextLine()){
                    ScoreEntry entry = parseLine(scanner.nextLine());
                    if(entry != null){
                        scores.add(entry);
                    }
                }
            }catch(Exception e){}
        }

        sortHighestFirst(scores);
        return scores;
    }

    public void saveScore(String name, int score){
        String cleanName = name.replace(",", "").trim();
        if(cleanName.isEmpty()){
            cleanName = "PLAYER";
        }

        List<ScoreEntry> scores = loadScores();
        scores.add(new ScoreEntry(cleanName, score));
        sortHighestFirst(scores);

        if(scores.size() > MAX_STORED){
            scores = new ArrayList<>(scores.subList(0, MAX_STORED));
        }

        try(PrintWriter writer = new PrintWriter(new File(fileName))){
            for(ScoreEntry entry : scores){
                writer.println(entry.getName() + "," + entry.getScore());
            }
        }catch(Exception e){}
    }

    public List<ScoreEntry> getTopScores(int count){
        List<ScoreEntry> scores = loadScores();
        if(scores.size() > count){
            return new ArrayList<>(scores.subList(0, count));
        }
        return scores;
    }

    private ScoreEntry parseLine(String line){
        line = line.trim();
        if(line.isEmpty()) return null;

        int comma = line.lastIndexOf(',');
        try{
            if(comma < 0){
                // old format: a file that only held one number
                return new ScoreEntry("PLAYER", Integer.parseInt(line));
            }
            String name = line.substring(0, comma).trim();
            int score = Integer.parseInt(line.substring(comma + 1).trim());
            return new ScoreEntry(name, score);
        }catch(NumberFormatException e){
            return null;
        }
    }

    // stable sort: on a tie the older entry stays ahead
    private void sortHighestFirst(List<ScoreEntry> scores){
        scores.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
    }
}