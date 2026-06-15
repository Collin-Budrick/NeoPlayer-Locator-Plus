package sh.sit.plp.color;

import java.util.Random;
import java.util.UUID;

public final class ColorUtils {
    private ColorUtils() {
    }

    public static int uuidToColor(UUID uuid) {
        Random random = new Random(uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits());
        return hslToColor(
                random.nextFloat() * 360.0F,
                random.nextFloat() / 4.0F + 0.75F,
                random.nextFloat() / 2.0F + 0.5F
        );
    }

    private static int hslToColor(float h, float s, float l) {
        float c = (1.0F - Math.abs(2.0F * l - 1.0F)) * s;
        float m = l - 0.5F * c;
        float x = c * (1.0F - Math.abs((h / 60.0F % 2.0F) - 1.0F));

        int hueSegment = (int) h / 60;
        float r = 0.0F;
        float g = 0.0F;
        float b = 0.0F;

        switch (hueSegment) {
            case 0 -> {
                r = c + m;
                g = x + m;
                b = m;
            }
            case 1 -> {
                r = x + m;
                g = c + m;
                b = m;
            }
            case 2 -> {
                r = m;
                g = c + m;
                b = x + m;
            }
            case 3 -> {
                r = m;
                g = x + m;
                b = c + m;
            }
            case 4 -> {
                r = x + m;
                g = m;
                b = c + m;
            }
            default -> {
                r = c + m;
                g = m;
                b = x + m;
            }
        }

        int red = clamp(Math.round(r * 255.0F));
        int green = clamp(Math.round(g * 255.0F));
        int blue = clamp(Math.round(b * 255.0F));
        return (red << 16) | (green << 8) | blue;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
