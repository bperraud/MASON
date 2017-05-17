package main;

import sim.display.Console;
import gui.BeingsWithUI;
import model.Beings;

public class BeingsMain {
    public static void main(String[] args) {
        runUI();
    }

    private static void runUI() {
        Beings       model   = new Beings(System.currentTimeMillis());
        BeingsWithUI gui     = new BeingsWithUI(model);
        Console      console = new Console(gui);
        console.setVisible(true);
    }
}
