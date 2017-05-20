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
        energy = (new Random()).nextInt(Constants.MAX_ENERGY + 1);

        this.model = model;
    }

    /**
     * In this case, the insect doesn't look for the nearest food location,
     * and especially could stupidly die while trying to reach a food unit...
     *
     * @param simState
     */
    @Override
    public void step(SimState simState) {
        IntBag    xPos       = new IntBag();
        IntBag    yPos       = new IntBag();
        Bag       neighbours = perceive(xPos, yPos);
        Integer[] dest       = null;

        System.out.println("--- NEW STEP ---");
        System.out.println("Pos : " + this.x + ", " + this.y);
        System.out.println("neighbours :");
        System.out.println(neighbours.size());
        System.out.println(xPos.size());
        System.out.println(yPos.size());
        int i = 0;
        for (Object neighbour : neighbours) {
            System.out.println("type : " + neighbour.getClass().toString());
            System.out.println("xPos " + xPos.get(i));
            System.out.println("yPos " + yPos.get(i));
            i++;
        }
        System.out.println();

        boolean isHungry = energy <= HUNGRY_THRESHOLD;

        // I'M HUNGRY!!! X(
        if (isHungry) {
            System.out.println("I'm hungry");

            // If there's still some apple juice, let's have a picnic
            if (load > 0) {
                System.out.println("load > 0");
                eat(null);
                return;
            }

            // I need to find some food nearby
            // Check if I've got food at my feet...
            int     index           = 0;
            boolean foodAtFeetFound = false;

            for (Object object : neighbours) {
                int x = xPos.get(index);
                int y = yPos.get(index);

                // I've got food, hurry!
                if (object instanceof Food) {
                    System.out.println("I found some food");
                    System.out.println("Food found at " + x + ", " + y);
                    System.out.println("I'm at " + this.x + ", " + this.y);
                    // and it's at my feet or just close to me, great!
                    int minX  = Math.min(this.x, x);
                    int maxX  = Math.max(this.x, x);
                    int diffX = Math.min((maxX - minX), Constants.GRID_SIZE - (maxX - minX));
                    int minY  = Math.min(this.y, y);
                    int maxY  = Math.max(this.y, y);
                    int diffY = Math.min((maxY - minY), Constants.GRID_SIZE - (maxY - minY));

                    if ((x == this.x && y == this.y) || (diffX <= 1 && diffY <= 1)) {
                        System.out.println("Let's eat it");
                        eat((Food) object);
                        foodAtFeetFound = true;
                        break;
                    }
                    // but I've got to reach it, god damn it... :(
                    else if (dest == null) {
                        System.out.println("I've got to move towards it");
                        dest = prepareMoveToCell(x, y);
                    }
                }
                index++;
            }

            if (foodAtFeetFound)
                return;

            // If I haven't seen any burger nearby, let's go crazy
            if (dest == null) {
                System.out.println("I'm hungry but don't know where to find some, let's wander");
                dest = prepareMoveToCell(); // Random move...
            }

            System.out.println("dest = " + dest[0] + ", " + dest[1]);

            // If need to move to some burger
            move(dest[0], dest[1]);

            return;
        }

        // Everthing's cool, buddy. So I want to fill my doggybag!
        if (!isFullyLoaded()) {
            System.out.println("I'm cool and intend to load my back");

            // Check if I've got food at my feet...
            int     index           = 0;
            boolean foodAtFeetFound = false;

            for (Object object : neighbours) {
                int x = xPos.get(index);
                int y = yPos.get(index);

                // I've got food, hurry!
                if (object instanceof Food) {
                    System.out.println("Food found at " + x + ", " + y);
                    System.out.println("I'm at " + this.x + ", " + this.y);
                    // and it's at my feet, great!
                    if (x == this.x && y == this.y) {
                        System.out.println("... at my feet exactly");
                        load();
                        foodAtFeetFound = true;
                        break;
                    }
                    // but I've got to reach it, god damn it... :(
                    else if (dest == null) {
                        System.out.println("I need to go towards it");
                        dest = prepareMoveToCell(x, y);
                    }
                }
                index++;
            }

            if (foodAtFeetFound)
                return;

            // If I haven't seen any burger nearby, let's go crazy
            if (dest == null) {
                System.out.println("I'm not hungry but don't know where to go");
                dest = prepareMoveToCell(); // Random move...
            }

            // If need to move to some burger
            move(dest[0], dest[1]);
            return;
        }

        // I've got plenty of food on my back, so I'm a wanderer...
        System.out.println("Everything's cool, let's move randomly");
        dest = prepareMoveToCell(); // Random move...
        move(dest[0], dest[1]);
    }

    @SuppressWarnings("Duplicates")
    private Integer[] prepareMoveToCell(int x, int y) {
        Integer[] dest           = new Integer[2];
        int       minX           = Math.min(this.x, x);
        int       maxX           = Math.max(this.x, x);
        boolean   currPosXisLeft = this.x < x;
        int       goForwardX;
        if (currPosXisLeft) {
            // Go forward
            if ((maxX - minX) <= Constants.GRID_SIZE - (maxX - minX)) {
                goForwardX = 1;
            }
            // Go backward
            else {
                goForwardX = -1;
            }
        } else {
            // Go backward
            if ((maxX - minX) <= Constants.GRID_SIZE - (maxX - minX)) {
                goForwardX = -1;
            }
            // Go forward
            else {
                goForwardX = 1;
            }
        }
        int     diffX          = Math.min((maxX - minX), Constants.GRID_SIZE - (maxX - minX));
        int     minY           = Math.min(this.y, y);
        int     maxY           = Math.max(this.y, y);
        boolean currPosYisLeft = this.y < y;
        int     goForwardY;
        if (currPosYisLeft) {
            // Go forward
            if ((maxY - minY) <= Constants.GRID_SIZE - (maxY - minY)) {
                goForwardY = 1;
            }
            // Go backward
            else {
                goForwardY = -1;
            }
        } else {
            // Go backward
            if ((maxY - minY) <= Constants.GRID_SIZE - (maxY - minY)) {
                goForwardY = -1;
            }
            // Go forward
            else {
                goForwardY = 1;
            }
        }
        int diffY       = Math.min((maxY - minY), Constants.GRID_SIZE - (maxY - minY));
        int neededSteps = diffX + diffY;

        // I can go to my destination in one step! ^^
        if (neededSteps <= this.DISTANCE_DEPLACEMENT) {
            dest[0] = x;
            dest[1] = y;
            return dest;
        }

        // Let's move on the X axis first
        if (diffX <= diffY) {
            int remainingMove = this.DISTANCE_DEPLACEMENT - diffX;

            // I've still points to use for Y movement
            if (remainingMove > 0) {
                dest[0] = x;
                dest[1] = Math.floorMod(this.y + (remainingMove * goForwardY), remainingMove);
                return dest;
            }
            // No more points available
            else {
                dest[0] = Math.floorMod(this.x + (this.DISTANCE_DEPLACEMENT * goForwardX), this.DISTANCE_DEPLACEMENT);
                dest[1] = this.y;
                return dest;
            }
        }
        // Let's move on the Y axis first
        else {
            int remainingMove = this.DISTANCE_DEPLACEMENT - diffY;

            // I've still points to use for X movement
            if (remainingMove > 0) {
                dest[0] = Math.floorMod(this.x + (remainingMove * goForwardX), remainingMove);
                dest[1] = y;
                return dest;
            }
            // No more points available
            else {
                dest[0] = this.x;
                dest[1] = Math.floorMod(this.y + (this.DISTANCE_DEPLACEMENT * goForwardY), this.DISTANCE_DEPLACEMENT);
                return dest;
            }
        }
    }

    private Integer[] prepareMoveToCell() {
        Integer[] dest       = new Integer[2];
        Random    random     = new Random();
        int       goForwardX = random.nextInt(2) == 0 ? -1 : 1;
        int       goForwardY = random.nextInt(2) == 0 ? -1 : 1;
        int       x          = 0;
        int       y          = 0;

        for (int i = 0; i < DISTANCE_DEPLACEMENT; i++) {
            // Lancer de dé
            int result = random.nextInt(2);

            switch (result) {
                case 0:
                    x++;
                    break;
                case 1:
                    y++;
                    break;
            }
        }

        dest[0] = Math.floorMod(x * goForwardX, Constants.GRID_SIZE);
        dest[1] = Math.floorMod(y * goForwardY, Constants.GRID_SIZE);
        return dest;
    }

    private boolean isFullyLoaded() {
        return load == CHARGE_MAX;
    }

    // TODO : Ask Moulin how getRadialNeighbours actually works
    private Bag perceive(IntBag xPos, IntBag yPos) {
        Bag result = new Bag();
//        xPos.
        model.yard.getRadialNeighbors(x, y, DISTANCE_PERCEPTION, Grid2D.TOROIDAL, true, result, xPos, yPos);
        return result;
    }

    private void eat(Food f) {
        // Consums one charge
        if (f == null) {
            System.out.println("LOAD -- !");
            load--;
        }
        // Consums a placed food unit
        else {
            System.out.println("Eat placed food!");
            f.consumeOneSubunit();
        }
        energy = Math.min(energy + Constants.FOOD_ENERGY, Constants.MAX_ENERGY);
    }

    private void move(int x, int y) {
        if (--energy <= 0) {
            kill();
            return;
        }

        this.x = x;
        this.y = y;
        model.yard.setObjectLocation(this, this.x, this.y);
    }

    /**
     * We check BEFORE the call that it can't overload
     */
    private void load() {
        for (Object o : model.yard.getObjectsAtLocation(x, y)) {
            if (o instanceof Food) {
                System.out.println("LOAD!");
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
