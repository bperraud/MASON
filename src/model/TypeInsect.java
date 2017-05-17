package model;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.field.grid.Grid2D;
import sim.util.Bag;
import sim.util.IntBag;

import java.util.Random;

public class TypeInsect implements Steppable {
    public int x, y;
    Stoppable stoppable;

    private Beings model;

    // Car. statiques
    private final int DISTANCE_DEPLACEMENT;
    private final int DISTANCE_PERCEPTION;
    private final int CHARGE_MAX;

    // Car. dynamiques
    private int energy;
    private int load;

    private final int HUNGRY_THRESHOLD = Constants.MAX_ENERGY / 2;

    TypeInsect(Beings model) {
        int available_skillpoints_pool = Constants.CAPACITY;
        int max_load                   = Constants.MAX_LOAD;

        int low  = 0;
        int high = 3;

        int dist_depl  = 1;
        int dist_perc  = 1;
        int charge_max = 1;

        for (int i = 0; i < available_skillpoints_pool; i++) {
            // Lancer de dé
            Random r      = new Random();
            int    result = r.nextInt(high - low) + low;

            switch (result) {
                case 0:
                    dist_depl++;
                    break;
                case 1:
                    dist_perc++;
                    break;
                case 2:
                    if (charge_max == max_load) {
                        i--;
                        break;
                    }
                    charge_max++;
                    break;
            }
        }

        // Assignation
        DISTANCE_DEPLACEMENT = dist_depl;
        DISTANCE_PERCEPTION = dist_perc;
        CHARGE_MAX = charge_max;

        load = 0;
        // TODO : vérifier le random
        energy = (new Random()).nextInt(Constants.MAX_ENERGY - 1) + 1;

        this.model = model;
    }

    @Override
    public void step(SimState simState) {
        IntBag xPos       = new IntBag();
        IntBag yPos       = new IntBag();
        Bag    neighbours = perceive(xPos, yPos);

        boolean isHungry = energy <= HUNGRY_THRESHOLD;

        // I'M HUNGRY!!! X(
        if (isHungry) {

            // TODO : I'M HUNGRY

            return;
        }

        // Everthing's cool, buddy. So I want to fill my doggybag!
        if (!isFullyLoaded()) {

            // Check if I've got food at my feet...
            int       index           = 0;
            boolean   foodAtFeetFound = false;
            Integer[] dest            = null;

            for (Object object : neighbours) {
                int x = xPos.get(index);
                int y = yPos.get(index);

                // I've got food, hurry!
                if (object instanceof Food) {
                    // and it's at my feet, great!
                    if (x == this.x && y == this.y) {
                        load();
                        foodAtFeetFound = true;
                        break;
                    }
                    // but I've got to reach it, god damn it... :(
                    else if (dest == null) {
                        dest = prepareMoveToCell(x, y);
                    }
                }
                index++;
            }

            if (foodAtFeetFound)
                return;

            // If I haven't seen any burger nearby, let's go crazy
            if (dest == null) {
                dest = prepareMoveToCell();
                move(dest[0], dest[1]);
            }

            // If need to move to some burger
            move(dest[0], dest[1]);
        }

        // I've got plenty of food on my back, so I'm a wanderer...

    }

    private Integer[] prepareMoveToCell(int x, int y) {
        return null;
    }

    private Integer[] prepareMoveToCell() {
        Integer[] dest   = new Integer[2];
        int       dist   = DISTANCE_DEPLACEMENT;
        Random    random = new Random();
//        int goForwardX =    random.nextInt(dist - low) + low;
        // TODO: On fait le choix du signe des deux directions puis on itère sur dist comme pour les points de capacité

        return dest;
    }

    private boolean isFullyLoaded() {
        return load == CHARGE_MAX;
    }

    private Bag perceive(IntBag xPos, IntBag yPos) {
        Bag result = new Bag();
        model.yard.getRadialNeighbors(x, y, DISTANCE_PERCEPTION, Grid2D.TOROIDAL, true, result, xPos, yPos);
        return result;
    }

    private void eat(Food f) {
        // Consums one charge
        if (f == null) {
            load--;
        }
        // Consums a placed food unit
        else {
            f.consumeOneSubunit();
        }
        energy = Math.min(energy + Constants.FOOD_ENERGY, Constants.MAX_ENERGY);
    }

    private void move(int x, int y) {
        if (--energy <= 0) {
            kill();
            return;
        }

        this.x += x;
        this.y += y;
        model.yard.setObjectLocation(this, this.x, this.y);
    }

    /**
     * We check BEFORE the call that it can't overload
     */
    private void load() {
        for (Object o : model.yard.getObjectsAtLocation(x, y)) {
            if (o instanceof Food) {
                load++;
                ((Food) o).consumeOneSubunit();
                break;
            }
        }
    }

    private void kill() {
        model.yard.remove(this);
        stoppable.stop();
    }
}
