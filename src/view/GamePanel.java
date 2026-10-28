package view;

import engine.GameManager;
import model.Trash;

import java.awt.*;
import javax.swing.*;

public class GamePanel extends JPanel{

    private final GameManager gameManager;
    private final int startSize = 70;
    private final int maxSize = 120;

    private final int startY = 250;
    private final int missLineY = 510;

    // private int trashSize  = 70;
    private int width = 500;
    private int height = 700;

    private final Color COLOR_SKY = Color.decode("#BFF0F7");
    private final Color COLOR_SEA = Color.decode("#09c3db");
    private final Color COLOR_PLASTIC = Color.decode("#2F5FDE");
    private final Color COLOR_TIN = Color.decode("#E8383C");
    private final Color COLOR_PAPER = Color.decode("#F5B400");
    private final Color COLOR_GLASS = Color.decode("#2FB350");
    private final Color COLOR_MISS = Color.decode("#6F3198");
    private final Color COLOR_SCORE = Color.decode("#123A52");
    private final Color COLOR_LIVES = Color.decode("#E0447A");
    private final Color COLOR_TRANS = new Color(0, 0, 0, 0);
    private final Color COLOR_SHADE = new Color(0, 0, 0, 70);

    private Font fontBOLD = new Font("Arial", Font.BOLD, 16);

    

    public GamePanel(GameManager gameManager){
        this.gameManager = gameManager;
        this.setPreferredSize(new Dimension(width, height));
    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setFont(fontBOLD);
        FontMetrics fm = g2d.getFontMetrics();

        drawBackground(g2d);
        drawFloaters(g2d);
        drawFloaterLabels(g2d, fm);
        

        if(gameManager.isGameOver()){
            drawGameOverScreen(g2d, fm);
        }else drawActiveGameplay(g2d, fm);

        drawScorePanel(g2d, fm);
    }

    private void drawBackground(Graphics2D g2d){
        //sky
        g2d.setColor(COLOR_SKY);
        g2d.fillRect(0,0, width, 225);
        
        //sea
        g2d.setColor(COLOR_SEA);
        g2d.fillRect(0, 225, width, height - 225);
    }

    private void drawFloaters(Graphics2D g2d){
        int yTop = 315;
        int yBottom = 490;
        int slantOffset = 50;
        int roundRadius = 10;

        int yMissTop = 510;
        int yMissBottom = 680;
        int xBLeft = 90;
        int xBRight = 410;

        g2d.setStroke(new java.awt.BasicStroke(20f));
        
        //miss line 
        java.awt.geom.Path2D.Double missFloater = new java.awt.geom.Path2D.Double();
        missFloater.moveTo(xBLeft + roundRadius, yMissBottom);
        missFloater.lineTo(xBRight - roundRadius, yMissBottom);
        missFloater.quadTo(xBRight, yMissBottom, xBRight - (slantOffset / 60), yMissBottom - roundRadius);
        missFloater.lineTo((xBRight - slantOffset) + (slantOffset / 6), yMissTop + roundRadius);
        missFloater.quadTo(xBRight - slantOffset, yMissTop, xBRight - slantOffset - roundRadius, yMissTop);
        missFloater.lineTo(xBLeft + slantOffset + roundRadius, yMissTop);
        missFloater.quadTo(xBLeft + slantOffset, yMissTop, (xBLeft + slantOffset) - (slantOffset / 6), yMissTop + roundRadius);
        missFloater.lineTo(xBLeft + (slantOffset / 60), yMissBottom - roundRadius);
        missFloater.quadTo(xBLeft, yMissBottom, xBLeft + roundRadius, yMissBottom);
        missFloater.closePath();
        //shadow
        Graphics2D missShadow = (Graphics2D) g2d.create();

        missShadow.translate(0, 15);
        missShadow.setColor(COLOR_SHADE);
        missShadow.fill(missFloater);

        missShadow.dispose();
        //transparent miss line
        g2d.setColor(COLOR_TRANS);
        g2d.fill(missFloater); 

        //outline miss line
        g2d.setColor(COLOR_MISS);
        g2d.draw(missFloater);

        //left side
        java.awt.geom.Path2D.Double leftFloater = new java.awt.geom.Path2D.Double();
        leftFloater.moveTo(0 - 10, yTop);
        leftFloater.lineTo(170 - roundRadius, yTop);
        leftFloater.quadTo(170, yTop, 170 - (slantOffset / 50), yTop + roundRadius);
        leftFloater.lineTo(170 - slantOffset + (slantOffset / 6), yBottom - roundRadius);
        leftFloater.quadTo(170 - slantOffset, yBottom, 170 - slantOffset - roundRadius, yBottom);
        leftFloater.lineTo(0 - 10, yBottom);
        leftFloater.closePath();
        
        Color leftColor = getFloaterColorByLabel(gameManager.getLeftFloaterLabel());
        //shadow
        Graphics2D leftShadow = (Graphics2D) g2d.create();

        leftShadow.translate(8, 15);
        leftShadow.setColor(COLOR_SHADE);
        leftShadow.fill(leftFloater);

        leftShadow.dispose();

        //transparent left side
        g2d.setColor(COLOR_TRANS);
        g2d.fill(leftFloater);
        // outline left side
        g2d.setColor(leftColor);
        g2d.draw(leftFloater);


        // right side
        java.awt.geom.Path2D.Double rightFloater = new java.awt.geom.Path2D.Double();
        rightFloater.moveTo(width + 10, yTop);
        rightFloater.lineTo(330 + roundRadius, yTop);
        rightFloater.quadTo(330, yTop, 330 + (slantOffset / 50), yTop + roundRadius);
        rightFloater.lineTo(330 + slantOffset - (slantOffset/6), yBottom - roundRadius);
        rightFloater.quadTo(330 + slantOffset, yBottom, 330 + slantOffset + roundRadius, yBottom);
        rightFloater.lineTo(width + 10, yBottom);
        rightFloater.closePath();

        Color rightColor = getFloaterColorByLabel(gameManager.getRightFloaterLabel());
        //shadow
        Graphics2D rightShadow = (Graphics2D) g2d.create();

        rightShadow.translate(-8, 15);
        rightShadow.setColor(COLOR_SHADE);
        rightShadow.fill(rightFloater);

        rightShadow.dispose();
        
        //transparent right side
        g2d.setColor(COLOR_TRANS);
        g2d.fill(rightFloater);
        // outline right side
        g2d.setColor(rightColor);
        g2d.draw(rightFloater);
    }

    private void drawFloaterLabels(Graphics2D g2d, FontMetrics fm){
        g2d.setColor(COLOR_TIN);
        g2d.fillRoundRect(25, 240, 120, 50, 10, 10);

        g2d.setColor(Color.decode("#932426"));
        g2d.fillRoundRect(35, 250, 100, 30, 10, 10);

        g2d.setColor(COLOR_TIN);
        g2d.fillRoundRect(355, 240, 120, 50, 10, 10);

        g2d.setColor(Color.decode("#932426"));
        g2d.fillRoundRect(365, 250, 100, 30, 10, 10);
    }

    private void drawGameOverScreen(Graphics2D g2d, FontMetrics fm){
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(COLOR_MISS);
        g2d.fillRoundRect(100, 200, 300, 490-225, 30, 30);

        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        FontMetrics fmOver = g2d.getFontMetrics();
        String gameOverText = "GAME OVER";
        int overX = (500 - fmOver.stringWidth(gameOverText)) / 2;
        g2d.drawString(gameOverText, overX, 280);

        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        FontMetrics fmHighScore = g2d.getFontMetrics();
        String highScoreText = "High Score: " + gameManager.getDisplayScore();
        int highScoreX = (500 - fmHighScore.stringWidth(highScoreText)) / 2;
        g2d.drawString(highScoreText, highScoreX, 330);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 20));
        FontMetrics fmScore = g2d.getFontMetrics();
        String finalScoreText = "Your Score: " + gameManager.getDisplayScore();
        int scoreX = (500 - fmScore.stringWidth(finalScoreText)) / 2;
        g2d.drawString(finalScoreText, scoreX, 355);

        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(175, 385, 150, 40, 10, 10);

        g2d.setColor(COLOR_PAPER);
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        FontMetrics fmRestart = g2d.getFontMetrics();
        String restartText = "RESTART";
        int restartX = (500 - fmRestart.stringWidth(restartText)) / 2;
        g2d.drawString(restartText, restartX, 415);
    }

    private void drawActiveGameplay(Graphics2D g2d, FontMetrics fm){
        Trash trash = gameManager.getActiveTrash();

        if(trash != null){
            int trashY = trash.getyPosition();
            int trashSize = getTrashSize(trashY);

            g2d.setColor(getTrashColorByLabel(trash.getType().toString()));

            int drawX = trash.getxPosition() - (trashSize / 2);
            int drawY = trashY - (trashSize / 2);

            g2d.fillOval(drawX, drawY, trashSize, trashSize);
        }

        g2d.setColor(Color.WHITE);

        String leftLabel = gameManager.getLeftFloaterLabel();
        int leftLabelX = (170 - fm.stringWidth(leftLabel)) / 2;
        g2d.drawString(leftLabel, leftLabelX, 270);

        String rightLabel = gameManager.getRightFloaterLabel();
        int rightLabelX = 330 + ((170 - fm.stringWidth(rightLabel)) / 2);
        g2d.drawString(rightLabel, rightLabelX, 270);

        String missLabel = "DISCARD";
        int missLabelX = (500 - fm.stringWidth(missLabel)) / 2;
        g2d.drawString(missLabel, missLabelX, 670);

    }

    private void drawScorePanel(Graphics2D g2d, FontMetrics fm){
        //SCORE
        g2d.setColor(COLOR_SCORE);
        String scoreStr = "Score: " + gameManager.getDisplayScore();
        int scoreStrX = 250 + ((250 - fm.stringWidth(scoreStr)) / 2);
        g2d.drawString(scoreStr, scoreStrX, 30);

        //LIVES
        g2d.setColor(COLOR_LIVES);
        String livesStr = "Lives: " + gameManager.getPlayerLives();
        int livesStrX = 330 + ((170 - fm.stringWidth(livesStr)) / 2);
        g2d.drawString(livesStr, livesStrX, 80);
    }

    private Color getTrashColorByLabel(String label){
        switch(label){
            case "PAPER": return COLOR_PAPER;
            case "PLASTIC": return COLOR_PLASTIC;
            case "TIN": return COLOR_TIN;
            case "GLASS": return COLOR_GLASS;
            default: return Color.MAGENTA;
        }
    }

    private Color getFloaterColorByLabel(String label){
        switch(label){
            case "PAPER": return COLOR_PAPER;
            case "PLASTIC": return COLOR_PLASTIC;
            case "TIN": return COLOR_TIN;
            case "GLASS": return COLOR_GLASS;
            default: return Color.MAGENTA;
        }
    }

    private int getTrashSize(int trashY) {
        double progress = (double) (trashY - startY)
                / (missLineY - startY);

        // Keep progress between 0 and 1
        progress = Math.max(0, Math.min(1, progress));

        return (int) (startSize + (maxSize - startSize) * progress);
    }
}
