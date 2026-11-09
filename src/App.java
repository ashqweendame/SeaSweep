// import javax.swing.*;
// import java.awt.event.ActionEvent;

// import engine.GameManager;
// import view.GamePanel;

// public class App {
//     public static void main(String[] args) throws Exception {
//         GameManager manager = new GameManager();
//         GamePanel panel = new GamePanel(manager);

//         JFrame window = new JFrame("Sea Sweep!");
//         window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//         window.setResizable(false);
//         window.add(panel);
//         window.pack();
//         // window.setLocationRelativeTo(null);
//         window.setVisible(true);

//         panel.getInputMap(GamePanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("LEFT"), "moveLeft");
//         panel.getActionMap().put("moveLeft",new AbstractAction() {
//             @Override 
//             public void actionPerformed(ActionEvent e){
//                 manager.executePlayerAction("left");
//             }
//         });

//         panel.getInputMap(GamePanel.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("RIGHT"), "moveRight");
//         panel.getActionMap().put("moveRight",new AbstractAction() {
//             @Override 
//             public void actionPerformed(ActionEvent e){
//                 manager.executePlayerAction("right");
//             }
//         });

//         Timer gameLoopTimer = new Timer(16, e->{
//             manager.processFrameUpdate(0.016);
//             panel.repaint();
//         });

//         gameLoopTimer.start();
//     }
// }
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import engine.GameManager;
import view.GamePanel;

public class App {
    public static void main(String[] args) throws Exception {
        GameManager manager = new GameManager();
        GamePanel panel = new GamePanel(manager);

        JFrame window = new JFrame("Sea Sweep!");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.add(panel);
        window.pack();
        window.setLocationRelativeTo(null);

        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "moveLeft");
        panel.getActionMap().put("moveLeft", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                manager.executePlayerAction("left");
            }
        });

        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "moveRight");
        panel.getActionMap().put("moveRight", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                manager.executePlayerAction("right");
            }
        });

        Timer gameLoopTimer = new Timer(16, e -> {
            manager.processFrameUpdate(0.016);
            panel.repaint();
        });
        gameLoopTimer.start();

        window.setVisible(true);
    }
}