package model;

import sim.engine.SimState;
import sim.field.grid.SparseGrid2D;
import sim.util.Int2D;

public class Beings extends SimState {
    public  SparseGrid2D yard      = new SparseGrid2D(Constants.GRID_SIZE, Constants.GRID_SIZE);
    private int          nbInsects = Constants.NUM_INSECTS;

    public Beings(long seed) {
        super(seed);
    }

    public void start() {
        System.out.println("Simulation started");
        super.start();
        yard.clear();
        addFoodUnits();
        addAgentsInsects();
    }

    private void addAgentsInsects() {

        for (int i = 0; i < Constants.NUM_INSECTS; i++) {
            TypeInsect insect   = new TypeInsect(this);
            Int2D      location = getRandomLocation();
            yard.setObjectLocation(insect, location);
            insect.x = location.x;
            insect.y = location.y;
            insect.stoppable = schedule.scheduleRepeating(insect);
        }
    }

    private void addFoodUnits() {
        for (int i = 0; i < Constants.NUM_FOOD_CELLS; i++) {
            Food  f        = new Food(this);
            Int2D location = getFreeLocation();
            yard.setObjectLocation(f, location);
            f.x = location.x;
            f.y = location.y;
        }
    }

    void addFoodUnit() {
        Food  f        = new Food(this);
        Int2D location = getFreeLocation();
        yard.setObjectLocation(f, location);
        f.x = location.x;
        f.y = location.y;
    }

    private Int2D getRandomLocation() {
        return new Int2D(
                random.nextInt(yard.getWidth()),
                random.nextInt(yard.getHeight())
        );
    }

    private Int2D getFreeLocation() {
        Int2D location = new Int2D(
                random.nextInt(yard.getWidth()),
                random.nextInt(yard.getHeight())
        );
        while (yard.getObjectsAtLocation(location.x, location.y) != null) {
            location = new Int2D(
                    random.nextInt(yard.getWidth()),
                    random.nextInt(yard.getHeight())
            );
        }
        return location;
    }

    public int getNbInsects() {
        return nbInsects;
    }

    public void setNbInsects(int nbInsects) {
        this.nbInsects = nbInsects;
    }
}
