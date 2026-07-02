/**
 * Comment by ghost ide
 * @author: Ninjacoder
 */
package ir.hanzodev1375.ghostide.codeeditors.ui.power;

import android.graphics.Canvas;
import android.graphics.Paint;

public class SmokeParticle extends CircleParticle {

    public SmokeParticle(float x, float y, float dx, float dy, int color, float size, float gravity, float friction, int life) {
        super(x, y, dx, dy, color, size, gravity, friction, life);
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        // Smoke expands as it rises
        size += 0.1;
    }
}
