package model;

import java.awt.Color;

public class Particle {
    public double x, y; // pixel position
    public double vx, vy; // velocity
    public float alpha; // 0.0 - 1.0
    public float size;
    public Color color;
    public int lifetime; // ticks remaining
    public int maxLifetime;
    public String text; // optional floating text (e.g. damage number)

    public Particle(double x, double y, double vx, double vy, Color color, float size, int lifetime) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
        this.size = size;
        this.lifetime = lifetime;
        this.maxLifetime = lifetime;
        this.alpha = 1.0f;
    }

    public static Particle text(double x, double y, String text, Color color, int lifetime) {
        Particle p = new Particle(x, y, 0, -1.5, color, 0, lifetime);
        p.text = text;
        return p;
    }

    public void update() {
        x += vx;
        y += vy;
        lifetime--;
        alpha = (float) lifetime / maxLifetime;
        vy += 0.05; // gravity
    }

    public boolean isDead() {
        return lifetime <= 0;
    }
}
