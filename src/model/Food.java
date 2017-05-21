package model;

import java.util.Random;

public class Food {
    int x, y;
    private int    quantity;
    private Beings model;

    Food(Beings model) {
        Random r = new Random();
        quantity = r.nextInt(Constants.MAX_FOOD + 1);
        this.model = model;
    }

    private void generateNewFoodUnit() {
        model.addFoodUnit();
    }

    void consumeOneSubunit() {
        if (--quantity <= 0) {
            model.yard.remove(this);
            generateNewFoodUnit();
        }
        System.out.println("Food quant. left on the ground : " + quantity);
    }
}
