package sh.sit.plp.config;

import net.minecraft.network.FriendlyByteBuf;

import java.util.Locale;
import java.util.Properties;

public class ModConfig {
    public boolean enabled = true;
    public boolean sendServerConfig = true;
    public boolean sendDistance = true;
    public int maxDistance = 0;
    public float directionPrecision = 300.0F;
    public int ticksBetweenUpdates = 5;
    public boolean sneakingHides = true;
    public boolean pumpkinHides = true;
    public boolean mobHeadsHide = true;
    public boolean invisibilityHides = true;

    public boolean visible = true;
    public boolean visibleEmpty = false;
    public boolean alwaysVisibleInSpectator = false;
    public boolean acceptServerConfig = true;
    public boolean fadeMarkers = true;
    public int fadeStart = 100;
    public int fadeEnd = 1000;
    public float fadeEndOpacity = 0.3F;
    public boolean showHeight = true;
    public boolean alwaysShowHeads = false;
    public boolean showHeadsOnTab = true;
    public boolean showNamesOnTab = true;

    public ColorMode colorMode = ColorMode.UUID;
    public int constantColor = 0xFFFFFF;

    public enum ColorMode {
        UUID,
        TEAM_COLOR,
        CUSTOM,
        CONSTANT
    }

    public ModConfig copy() {
        ModConfig ret = new ModConfig();
        ret.enabled = enabled;
        ret.sendServerConfig = sendServerConfig;
        ret.sendDistance = sendDistance;
        ret.maxDistance = maxDistance;
        ret.directionPrecision = directionPrecision;
        ret.ticksBetweenUpdates = ticksBetweenUpdates;
        ret.sneakingHides = sneakingHides;
        ret.pumpkinHides = pumpkinHides;
        ret.mobHeadsHide = mobHeadsHide;
        ret.invisibilityHides = invisibilityHides;
        ret.visible = visible;
        ret.visibleEmpty = visibleEmpty;
        ret.alwaysVisibleInSpectator = alwaysVisibleInSpectator;
        ret.acceptServerConfig = acceptServerConfig;
        ret.fadeMarkers = fadeMarkers;
        ret.fadeStart = fadeStart;
        ret.fadeEnd = fadeEnd;
        ret.fadeEndOpacity = fadeEndOpacity;
        ret.showHeight = showHeight;
        ret.alwaysShowHeads = alwaysShowHeads;
        ret.showHeadsOnTab = showHeadsOnTab;
        ret.showNamesOnTab = showNamesOnTab;
        ret.colorMode = colorMode;
        ret.constantColor = constantColor;
        return ret;
    }

    public void validate() {
        if (fadeStart < 0) fadeStart = 0;
        if (fadeEnd < 1 || fadeEnd <= fadeStart) fadeEnd = fadeStart + 1;
        if (fadeEndOpacity < 0.0F || fadeEndOpacity > 1.0F) fadeEndOpacity = 0.3F;
        if (ticksBetweenUpdates < 0) ticksBetweenUpdates = 0;
        if (directionPrecision <= 1.0F) directionPrecision = 300.0F;
        if (maxDistance < 0) maxDistance = 0;
        constantColor = constantColor & 0xFFFFFF;
    }

    public void load(Properties properties) {
        enabled = getBool(properties, "enabled", enabled);
        sendServerConfig = getBool(properties, "sendServerConfig", sendServerConfig);
        sendDistance = getBool(properties, "sendDistance", sendDistance);
        maxDistance = getInt(properties, "maxDistance", maxDistance);
        directionPrecision = getFloat(properties, "directionPrecision", directionPrecision);
        ticksBetweenUpdates = getInt(properties, "ticksBetweenUpdates", ticksBetweenUpdates);
        sneakingHides = getBool(properties, "sneakingHides", sneakingHides);
        pumpkinHides = getBool(properties, "pumpkinHides", pumpkinHides);
        mobHeadsHide = getBool(properties, "mobHeadsHide", mobHeadsHide);
        invisibilityHides = getBool(properties, "invisibilityHides", invisibilityHides);
        visible = getBool(properties, "visible", visible);
        visibleEmpty = getBool(properties, "visibleEmpty", visibleEmpty);
        alwaysVisibleInSpectator = getBool(properties, "alwaysVisibleInSpectator", alwaysVisibleInSpectator);
        acceptServerConfig = getBool(properties, "acceptServerConfig", acceptServerConfig);
        fadeMarkers = getBool(properties, "fadeMarkers", fadeMarkers);
        fadeStart = getInt(properties, "fadeStart", fadeStart);
        fadeEnd = getInt(properties, "fadeEnd", fadeEnd);
        fadeEndOpacity = getFloat(properties, "fadeEndOpacity", fadeEndOpacity);
        showHeight = getBool(properties, "showHeight", showHeight);
        alwaysShowHeads = getBool(properties, "alwaysShowHeads", alwaysShowHeads);
        showHeadsOnTab = getBool(properties, "showHeadsOnTab", showHeadsOnTab);
        showNamesOnTab = getBool(properties, "showNamesOnTab", showNamesOnTab);
        colorMode = getEnum(properties, "colorMode", colorMode);
        constantColor = getColor(properties, "constantColor", constantColor);
        validate();
    }

    public Properties save() {
        Properties properties = new Properties();
        properties.setProperty("enabled", Boolean.toString(enabled));
        properties.setProperty("sendServerConfig", Boolean.toString(sendServerConfig));
        properties.setProperty("sendDistance", Boolean.toString(sendDistance));
        properties.setProperty("maxDistance", Integer.toString(maxDistance));
        properties.setProperty("directionPrecision", Float.toString(directionPrecision));
        properties.setProperty("ticksBetweenUpdates", Integer.toString(ticksBetweenUpdates));
        properties.setProperty("sneakingHides", Boolean.toString(sneakingHides));
        properties.setProperty("pumpkinHides", Boolean.toString(pumpkinHides));
        properties.setProperty("mobHeadsHide", Boolean.toString(mobHeadsHide));
        properties.setProperty("invisibilityHides", Boolean.toString(invisibilityHides));
        properties.setProperty("visible", Boolean.toString(visible));
        properties.setProperty("visibleEmpty", Boolean.toString(visibleEmpty));
        properties.setProperty("alwaysVisibleInSpectator", Boolean.toString(alwaysVisibleInSpectator));
        properties.setProperty("acceptServerConfig", Boolean.toString(acceptServerConfig));
        properties.setProperty("fadeMarkers", Boolean.toString(fadeMarkers));
        properties.setProperty("fadeStart", Integer.toString(fadeStart));
        properties.setProperty("fadeEnd", Integer.toString(fadeEnd));
        properties.setProperty("fadeEndOpacity", Float.toString(fadeEndOpacity));
        properties.setProperty("showHeight", Boolean.toString(showHeight));
        properties.setProperty("alwaysShowHeads", Boolean.toString(alwaysShowHeads));
        properties.setProperty("showHeadsOnTab", Boolean.toString(showHeadsOnTab));
        properties.setProperty("showNamesOnTab", Boolean.toString(showNamesOnTab));
        properties.setProperty("colorMode", colorMode.name());
        properties.setProperty("constantColor", String.format(Locale.ROOT, "#%06X", constantColor & 0xFFFFFF));
        return properties;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
        buf.writeBoolean(sendServerConfig);
        buf.writeBoolean(sendDistance);
        buf.writeVarInt(maxDistance);
        buf.writeFloat(directionPrecision);
        buf.writeVarInt(ticksBetweenUpdates);
        buf.writeBoolean(sneakingHides);
        buf.writeBoolean(pumpkinHides);
        buf.writeBoolean(mobHeadsHide);
        buf.writeBoolean(invisibilityHides);
        buf.writeBoolean(visible);
        buf.writeBoolean(visibleEmpty);
        buf.writeBoolean(alwaysVisibleInSpectator);
        buf.writeBoolean(acceptServerConfig);
        buf.writeBoolean(fadeMarkers);
        buf.writeVarInt(fadeStart);
        buf.writeVarInt(fadeEnd);
        buf.writeFloat(fadeEndOpacity);
        buf.writeBoolean(showHeight);
        buf.writeBoolean(alwaysShowHeads);
        buf.writeBoolean(showHeadsOnTab);
        buf.writeBoolean(showNamesOnTab);
        buf.writeEnum(colorMode);
        buf.writeInt(constantColor);
    }

    public static ModConfig read(FriendlyByteBuf buf) {
        ModConfig config = new ModConfig();
        config.enabled = buf.readBoolean();
        config.sendServerConfig = buf.readBoolean();
        config.sendDistance = buf.readBoolean();
        config.maxDistance = buf.readVarInt();
        config.directionPrecision = buf.readFloat();
        config.ticksBetweenUpdates = buf.readVarInt();
        config.sneakingHides = buf.readBoolean();
        config.pumpkinHides = buf.readBoolean();
        config.mobHeadsHide = buf.readBoolean();
        config.invisibilityHides = buf.readBoolean();
        config.visible = buf.readBoolean();
        config.visibleEmpty = buf.readBoolean();
        config.alwaysVisibleInSpectator = buf.readBoolean();
        config.acceptServerConfig = buf.readBoolean();
        config.fadeMarkers = buf.readBoolean();
        config.fadeStart = buf.readVarInt();
        config.fadeEnd = buf.readVarInt();
        config.fadeEndOpacity = buf.readFloat();
        config.showHeight = buf.readBoolean();
        config.alwaysShowHeads = buf.readBoolean();
        config.showHeadsOnTab = buf.readBoolean();
        config.showNamesOnTab = buf.readBoolean();
        config.colorMode = buf.readEnum(ColorMode.class);
        config.constantColor = buf.readInt();
        config.validate();
        return config;
    }

    private static boolean getBool(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private static int getInt(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)).trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float getFloat(Properties properties, String key, float fallback) {
        try {
            return Float.parseFloat(properties.getProperty(key, Float.toString(fallback)).trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int getColor(Properties properties, String key, int fallback) {
        String value = properties.getProperty(key);
        if (value == null) return fallback;
        value = value.trim();
        if (value.startsWith("#")) value = value.substring(1);
        try {
            return Integer.parseInt(value, 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static <T extends Enum<T>> T getEnum(Properties properties, String key, T fallback) {
        String value = properties.getProperty(key);
        if (value == null) return fallback;
        try {
            return Enum.valueOf(fallback.getDeclaringClass(), value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
