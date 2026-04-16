package game;

import javax.sound.sampled.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Programmatic sound effects using Java Sound API (no external files needed).
 * All sounds are synthesized from simple waveforms on a background thread pool.
 */
public class SoundManager {

    private static boolean muted = false;
    private static final ExecutorService pool = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "sound-thread");
        t.setDaemon(true);
        return t;
    });

    public static void setMuted(boolean m) {
        muted = m;
    }

    public static boolean isMuted() {
        return muted;
    }

    // ── Public sound cues ──────────────────────────────────────────────────────

    public static void buy() {
        play(() -> tone(880, 60, 0.25f, Shape.SINE));
    }

    public static void sell() {
        play(() -> tone(440, 80, 0.2f, Shape.SINE));
    }

    public static void deploy() {
        play(() -> tone(660, 70, 0.22f, Shape.SQUARE));
    }

    public static void hit() {
        play(() -> noise(40, 0.3f));
    }

    public static void cast() {
        play(() -> sweep(400, 900, 120, 0.3f));
    }

    public static void death() {
        play(() -> sweep(600, 150, 200, 0.35f));
    }

    public static void heal() {
        play(() -> sweep(500, 800, 100, 0.25f));
    }

    public static void stun() {
        play(() -> buzz(300, 80, 0.2f));
    }

    public static void combatStart() {
        play(() -> {
            tone(523, 80, 0.3f, Shape.SINE);
            sleep(60);
            tone(659, 100, 0.35f, Shape.SINE);
        });
    }

    public static void victory() {
        play(() -> {
            tone(523, 100, 0.4f, Shape.SINE);
            sleep(60);
            tone(659, 100, 0.4f, Shape.SINE);
            sleep(60);
            tone(784, 200, 0.4f, Shape.SINE);
        });
    }

    public static void defeat() {
        play(() -> {
            tone(400, 120, 0.35f, Shape.SINE);
            sleep(60);
            tone(300, 200, 0.35f, Shape.SINE);
        });
    }

    public static void starMerge() {
        play(() -> {
            tone(800, 80, 0.3f, Shape.SINE);
            sleep(40);
            tone(1000, 80, 0.3f, Shape.SINE);
            sleep(40);
            tone(1200, 120, 0.35f, Shape.SINE);
        });
    }

    public static void buttonClick() {
        play(() -> tone(660, 40, 0.15f, Shape.SINE));
    }

    // ── Internals ──────────────────────────────────────────────────────────────

    private enum Shape {
        SINE, SQUARE
    }

    private static void play(Runnable r) {
        if (muted)
            return;
        pool.submit(() -> {
            try {
                r.run();
            } catch (Exception ignored) {
            }
        });
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    /** Single-frequency tone */
    private static void tone(double freq, int durationMs, float volume, Shape shape) {
        try {
            int sampleRate = 44100;
            int nSamples = sampleRate * durationMs / 1000;
            byte[] buf = new byte[nSamples * 2];
            for (int i = 0; i < nSamples; i++) {
                double t = (double) i / sampleRate;
                double sample;
                if (shape == Shape.SQUARE) {
                    sample = Math.sin(2 * Math.PI * freq * t) >= 0 ? 1.0 : -1.0;
                } else {
                    sample = Math.sin(2 * Math.PI * freq * t);
                }
                // Envelope: 5ms attack, 10ms release
                double env = envelope(i, nSamples, sampleRate, 0.005, 0.010);
                short val = (short) (sample * env * volume * Short.MAX_VALUE);
                buf[i * 2] = (byte) (val & 0xff);
                buf[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
            }
            playBuffer(buf, sampleRate);
        } catch (Exception ignored) {
        }
    }

    /** Frequency sweep (portamento) */
    private static void sweep(double freqStart, double freqEnd, int durationMs, float volume) {
        try {
            int sampleRate = 44100;
            int nSamples = sampleRate * durationMs / 1000;
            byte[] buf = new byte[nSamples * 2];
            double phase = 0;
            for (int i = 0; i < nSamples; i++) {
                double progress = (double) i / nSamples;
                double freq = freqStart + (freqEnd - freqStart) * progress;
                phase += 2 * Math.PI * freq / sampleRate;
                double sample = Math.sin(phase);
                double env = envelope(i, nSamples, sampleRate, 0.005, 0.020);
                short val = (short) (sample * env * volume * Short.MAX_VALUE);
                buf[i * 2] = (byte) (val & 0xff);
                buf[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
            }
            playBuffer(buf, sampleRate);
        } catch (Exception ignored) {
        }
    }

    /** White-noise burst (hit/impact) */
    private static void noise(int durationMs, float volume) {
        try {
            int sampleRate = 44100;
            int nSamples = sampleRate * durationMs / 1000;
            byte[] buf = new byte[nSamples * 2];
            java.util.Random rng = new java.util.Random();
            for (int i = 0; i < nSamples; i++) {
                double env = envelope(i, nSamples, sampleRate, 0.001, 0.015);
                short val = (short) ((rng.nextDouble() * 2 - 1) * env * volume * Short.MAX_VALUE);
                buf[i * 2] = (byte) (val & 0xff);
                buf[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
            }
            playBuffer(buf, sampleRate);
        } catch (Exception ignored) {
        }
    }

    /** Buzzy square + fundamental blend for stun */
    private static void buzz(double freq, int durationMs, float volume) {
        tone(freq, durationMs, volume * 0.5f, Shape.SQUARE);
    }

    private static double envelope(int i, int n, int sampleRate, double attackSec, double releaseSec) {
        double attack = attackSec * sampleRate;
        double release = releaseSec * sampleRate;
        if (i < attack)
            return i / attack;
        if (i > n - release)
            return (n - i) / release;
        return 1.0;
    }

    private static void playBuffer(byte[] buf, int sampleRate) throws Exception {
        AudioFormat fmt = new AudioFormat(sampleRate, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
        if (!AudioSystem.isLineSupported(info))
            return;
        try (SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(fmt, buf.length);
            line.start();
            line.write(buf, 0, buf.length);
            line.drain();
        }
    }
}
