package model;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.field.grid.Grid2D;
import sim.util.Bag;
import sim.util.IntBag;

import java.util.Random;

@SuppressWarnings("SuspiciousNameCombination")
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
            int     index            = 0;
            boolean foodAtFeetFound  = false;
            int     minDistanceFound = Constants.GRID_SIZE * 2;

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

                    if (diffX <= 1 && diffY <= 1) {
                        System.out.println("Let's eat it");
                        eat((Food) object);
                        foodAtFeetFound = true;
                        break;
                    }
                    // but I've got to reach it, god damn it... :(
                    else if (dest == null || (diffX + diffY) < minDistanceFound) {
                        System.out.println("I've got to move towards it");
                        dest = prepareMoveToCell(x, y);
                        System.out.println("Planned dest : " + dest[0] + ", " + dest[1]);
                        minDistanceFound = diffX + diffY;
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
            int     index            = 0;
            boolean foodAtFeetFound  = false;
            int     minDistanceFound = Constants.GRID_SIZE * 2;

            for (Object object : neighbours) {
                int x = xPos.get(index);
                int y = yPos.get(index);

                // I've got food, hurry!
                if (object instanceof Food) {
                    System.out.println("Food found at " + x + ", " + y);
                    System.out.println("I'm at " + this.x + ", " + this.y);

                    // and it's at my feet, great!
                    int minX  = Math.min(this.x, x);
                    int maxX  = Math.max(this.x, x);
                    int diffX = Math.min((maxX - minX), Constants.GRID_SIZE - (maxX - minX));
                    int minY  = Math.min(this.y, y);
                    int maxY  = Math.max(this.y, y);
                    int diffY = Math.min((maxY - minY), Constants.GRID_SIZE - (maxY - minY));

                    if (x == this.x && y == this.y) {
                        System.out.println("... at my feet exactly");
                        load();
                        foodAtFeetFound = true;
                        break;
                    }
                    // but I've got to reach it, god damn it... :(
                    else if (dest == null || (diffX + diffY) < minDistanceFound) {
                        System.out.println("I need to go towards it");
                        dest = prepareMoveToCell(x, y);
                        System.out.println("Planned dest : " + dest[0] + ", " + dest[1]);
                        minDistanceFound = diffX + diffY;
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
        Integer[] dest          = new Integer[2];
        int       remainingMove = this.DISTANCE_DEPLACEMENT;
        int       tempX         = this.x;
        int       tempY         = this.y;

        while (!(tempX == x && tempY == y) && remainingMove > 0) {
            Boolean mustGoForwardX = null;
            Boolean mustGoForwardY = null;
            // X calculs
            if (tempX != x) {
                int     minX                 = Math.min(tempX, x);
                int     maxX                 = Math.max(tempX, x);
                boolean currPosXisLeftOfDest = tempX < x;
//                System.out.println("currPosXisLeftOfDest : " + currPosXisLeftOfDest);
                mustGoForwardX = (maxX - minX) <= Constants.GRID_SIZE - (maxX - minX);
                if (!currPosXisLeftOfDest) mustGoForwardX = !mustGoForwardX;
            }
            // Y calculs
            if (tempY != y) {
                int     minY                 = Math.min(tempY, y);
                int     maxY                 = Math.max(tempY, y);
                boolean currPosYisLeftOfDest = tempY < y;
//                System.out.println("currPosYisLeftOfDest : " + currPosYisLeftOfDest);
                mustGoForwardY = (maxY - minY) <= Constants.GRID_SIZE - (maxY - minY);
                if (!currPosYisLeftOfDest) mustGoForwardY = !mustGoForwardY;
            }
//            System.out.println("mustGoForwardX : " + mustGoForwardX);
//            System.out.println("mustGoForwardY : " + mustGoForwardY);

            int direction;
            if (mustGoForwardX == null) {
                assert mustGoForwardY != null;
                direction = mustGoForwardY ? 0 : 4;
            } else if (mustGoForwardX) {
                if (mustGoForwardY == null) direction = 2;
                else if (mustGoForwardY) direction = 1;
                else direction = 3;
            } else {
                if (mustGoForwardY == null) direction = 6;
                else if (mustGoForwardY) direction = 7;
                else direction = 5;
            }

            if (1 <= direction && direction <= 3) tempX++; // Right movement
            if (5 <= direction) tempX--; // Left movement
            if (direction <= 1 || direction == 7) tempY++; // Top movement
            if (3 <= direction && direction <= 5) tempY--; // Bottom movement

            // Re-center in the toric dim
            tempX = Math.floorMod(tempX, Constants.GRID_SIZE);
            tempY = Math.floorMod(tempY, Constants.GRID_SIZE);

            remainingMove--;
        }

        dest[0] = tempX;
        dest[1] = tempY;
        return dest;
    }

    private Integer[] prepareMoveToCell() {
        Integer[] dest   = new Integer[2];
        Random    random = new Random();
        int       x      = 0;
        int       y      = 0;

        for (int i = 0; i < DISTANCE_DEPLACEMENT; i++) {
            // Lancer de dé
            int direction = random.nextInt(8);  // Cardinal directions by anticlockwise rotation, 0 = North
            if (1 <= direction && direction <= 3) x++;  // Right movement
            if (5 <= direction) x--;                    // Left movement
            if (direction <= 1 || direction == 7) y++;  // Top movement
            if (3 <= direction && direction <= 5) y--;  // Bottom movement
        }

        dest[0] = Math.floorMod(x, Constants.GRID_SIZE);
        dest[1] = Math.floorMod(y, Constants.GRID_SIZE);
        return dest;
    }

    private boolean isFullyLoaded() {
        return load == CHARGE_MAX;
    }

    private Bag perceive(IntBag xPos, IntBag yPos) {
        // TODO: ask Moulin why xPos/yPos size may differ from the result one... :(
        System.out.println("PERCEIVE BEGIN");
        Bag result = new Bag();
        model.yard.getRadialNeighborsAndLocations(x, y, DISTANCE_PERCEPTION, Grid2D.TOROIDAL, true, result, xPos, yPos);
        System.out.println("result size : " + result.size());
        System.out.println("xPos size : " + xPos.size());
        System.out.println("yPos size : " + yPos.size());
        System.out.println("PERCEIVE END");
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
        System.out.println("Move at " + x + ", " + y);
        if (--energy <= 0) {
            kill();
            System.out.println("Noooo, I'm out of energy... I DIE X(");
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
