package com.g1739.firmalifegreenhousepatch.client;

import com.g1739.firmalifegreenhousepatch.common.menu.ClimateStationTemperatureMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.client.screen.TFCContainerScreen;
import net.dries007.tfc.network.ScreenButtonPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class ClimateStationTemperatureScreen extends TFCContainerScreen<ClimateStationTemperatureMenu>
{
    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");
    private static final int PANEL_X = 8;
    private static final int PANEL_Y = 5;
    private static final int PANEL_WIDTH = 160;
    private static final int PANEL_HEIGHT = 78;
    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int PANEL_BORDER = 0xFF8B8B8B;
    private static final int TEXT_COLOR = 0x404040;
    private static final float TITLE_SCALE = 0.85f;
    private static final float HEADER_SCALE = 0.65f;
    private static final float HINT_SCALE = 0.50f;
    private static final float RESULT_SCALE = 0.65f;
    private static final float LABEL_SCALE = 0.55f;
    private static final float DETAIL_SCALE = 0.50f;

    @Nullable
    private EditBox temperatureBox;
    @Nullable
    private Button saveButton;

    public ClimateStationTemperatureScreen(ClimateStationTemperatureMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title, TFCContainerScreen.INVENTORY_1x1);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 72;
    }

    @Override
    protected void init()
    {
        super.init();

        addRenderableWidget(Button.builder(Component.literal("-5"), button -> adjustTemperature(-5)).bounds(leftPos + 41, topPos + 46, 16, 14).build());
        addRenderableWidget(Button.builder(Component.literal("-1"), button -> adjustTemperature(-1)).bounds(leftPos + 59, topPos + 46, 16, 14).build());
        addRenderableWidget(Button.builder(Component.literal("+1"), button -> adjustTemperature(1)).bounds(leftPos + 101, topPos + 46, 16, 14).build());
        addRenderableWidget(Button.builder(Component.literal("+5"), button -> adjustTemperature(5)).bounds(leftPos + 119, topPos + 46, 16, 14).build());

        temperatureBox = addRenderableWidget(new EditBox(font, leftPos + 62, topPos + 32, 52, 13, Component.translatable("screen.firmalife_greenhouse_patch.temperature_box")));
        temperatureBox.setValue(Integer.toString(menu.getRequestedTemperature()));
        temperatureBox.setFilter(value -> value.isEmpty() || "-".equals(value) || value.matches("-?\\d{0,4}"));
        temperatureBox.setResponder(value -> updateButtonState());
        setInitialFocus(temperatureBox);

        saveButton = addRenderableWidget(Button.builder(Component.translatable("screen.firmalife_greenhouse_patch.save"), button -> saveTemperature()).bounds(leftPos + 77, topPos + 46, 24, 14).build());
        updateButtonState();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY)
    {
        drawDefaultBackground(graphics);
        graphics.fill(leftPos + PANEL_X, topPos + PANEL_Y, leftPos + PANEL_X + PANEL_WIDTH, topPos + PANEL_Y + PANEL_HEIGHT, PANEL_COLOR);
        graphics.renderOutline(leftPos + PANEL_X, topPos + PANEL_Y, PANEL_WIDTH, PANEL_HEIGHT, PANEL_BORDER);
        graphics.blitSprite(SLOT_SPRITE, leftPos + ClimateStationTemperatureMenu.HEATING_SLOT_X - 1, topPos + ClimateStationTemperatureMenu.HEATING_SLOT_Y - 1, 18, 18);
        graphics.blitSprite(SLOT_SPRITE, leftPos + ClimateStationTemperatureMenu.COOLING_SLOT_X - 1, topPos + ClimateStationTemperatureMenu.COOLING_SLOT_Y - 1, 18, 18);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        final int leftCenter = ClimateStationTemperatureMenu.HEATING_SLOT_X + 8;
        final int rightCenter = ClimateStationTemperatureMenu.COOLING_SLOT_X + 8;
        final int middleCenter = imageWidth / 2;

        flgp$drawScaledCenteredLine(graphics, title, imageWidth / 2, 10, TITLE_SCALE, TEXT_COLOR);

        flgp$drawScaledCenteredLine(graphics, Component.translatable("screen.firmalife_greenhouse_patch.heating_slot"), leftCenter, 15, HEADER_SCALE, TEXT_COLOR);
        flgp$drawScaledCenteredLine(graphics, Component.translatable("screen.firmalife_greenhouse_patch.heating_hint"), leftCenter, 22, HINT_SCALE, TEXT_COLOR);
        flgp$drawScaledCenteredLine(graphics, Component.translatable("screen.firmalife_greenhouse_patch.target_temperature"), middleCenter, 19, HEADER_SCALE, TEXT_COLOR);
        flgp$drawScaledCenteredLine(graphics, Component.translatable("screen.firmalife_greenhouse_patch.cooling_slot"), rightCenter, 15, HEADER_SCALE, TEXT_COLOR);
        flgp$drawScaledCenteredLine(graphics, Component.translatable("screen.firmalife_greenhouse_patch.cooling_hint"), rightCenter, 22, HINT_SCALE, TEXT_COLOR);

        flgp$drawScaledCenteredLine(graphics, Component.translatable("screen.firmalife_greenhouse_patch.heating_range", menu.getHeatingControlRange()), leftCenter, 49, RESULT_SCALE, TEXT_COLOR);
        flgp$drawScaledCenteredLine(graphics, Component.translatable("screen.firmalife_greenhouse_patch.cooling_range", menu.getCoolingControlRange()), rightCenter, 49, RESULT_SCALE, TEXT_COLOR);

        if (!menu.isClimateStationActive())
        {
            flgp$drawScaledCenteredLine(graphics, Component.translatable("screen.firmalife_greenhouse_patch.inactive"), middleCenter, 67, LABEL_SCALE, 0x9F4040);
        }
        else if (menu.isCellarMode())
        {
            flgp$drawScaledCenteredLine(graphics, Component.translatable(menu.getStructureSummaryKey(), Component.translatable(menu.getStructureNameKey()), menu.getBaseControlRange()), middleCenter, 61, DETAIL_SCALE, TEXT_COLOR);
            flgp$drawScaledCenteredLine(graphics, Component.translatable("screen.firmalife_greenhouse_patch.cellar_preservation", flgp$formatFactor(menu.getCellarPreservationMultiplier()), menu.getCellarDecayPercent()), middleCenter, 67, DETAIL_SCALE, TEXT_COLOR);
            flgp$drawScaledCenteredLine(graphics, Component.translatable("screen.firmalife_greenhouse_patch.status_line", menu.getAmbientTemperature(), menu.getEffectiveTemperature()), middleCenter, 73, DETAIL_SCALE, TEXT_COLOR);
            flgp$drawScaledCenteredLine(graphics, Component.translatable("screen.firmalife_greenhouse_patch.allowed_range", menu.getMinAllowedTemperature(), menu.getMaxAllowedTemperature()), middleCenter, 79, DETAIL_SCALE, TEXT_COLOR);
        }
        else
        {
            flgp$drawScaledCenteredLine(graphics, Component.translatable(menu.getStructureSummaryKey(), Component.translatable(menu.getStructureNameKey()), menu.getBaseControlRange()), middleCenter, 61, LABEL_SCALE, TEXT_COLOR);
            flgp$drawScaledCenteredLine(graphics, Component.translatable("screen.firmalife_greenhouse_patch.status_line", menu.getAmbientTemperature(), menu.getEffectiveTemperature()), middleCenter, 69, LABEL_SCALE, TEXT_COLOR);
            flgp$drawScaledCenteredLine(graphics, Component.translatable("screen.firmalife_greenhouse_patch.allowed_range", menu.getMinAllowedTemperature(), menu.getMaxAllowedTemperature()), middleCenter, 76, LABEL_SCALE, TEXT_COLOR);
        }
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        updateButtonState();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
        {
            if (parseTemperature() != null)
            {
                saveTemperature();
                return true;
            }
        }
        if (temperatureBox != null && temperatureBox.keyPressed(keyCode, scanCode, modifiers))
        {
            updateButtonState();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers)
    {
        if (temperatureBox != null && temperatureBox.charTyped(codePoint, modifiers))
        {
            updateButtonState();
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY)
    {
        super.renderTooltip(graphics, mouseX, mouseY);

        if (RenderHelpers.isInside(mouseX, mouseY, leftPos + 12, topPos + 14, 42, 38))
        {
            graphics.renderTooltip(font, flgp$getTooltipText(flgp$getHeatingTooltip()), mouseX, mouseY);
            return;
        }
        if (RenderHelpers.isInside(mouseX, mouseY, leftPos + 122, topPos + 14, 42, 38))
        {
            graphics.renderTooltip(font, flgp$getTooltipText(flgp$getCoolingTooltip()), mouseX, mouseY);
            return;
        }
        if (RenderHelpers.isInside(mouseX, mouseY, leftPos + 50, topPos + 16, 76, 58))
        {
            graphics.renderTooltip(font, flgp$getTooltipText(flgp$getStatusTooltip()), mouseX, mouseY);
        }
    }

    private void adjustTemperature(int delta)
    {
        final Integer parsedTemperature = parseTemperature();
        final int currentTemperature = parsedTemperature != null ? parsedTemperature : menu.getRequestedTemperature();
        final int adjustedTemperature = menu.clampRequestedTemperature(currentTemperature + delta);

        if (temperatureBox != null)
        {
            temperatureBox.setValue(Integer.toString(adjustedTemperature));
        }

        sendTemperature(adjustedTemperature);
        updateButtonState();
    }

    private void saveTemperature()
    {
        final Integer parsedTemperature = parseTemperature();
        if (parsedTemperature == null)
        {
            return;
        }

        final int clampedTemperature = menu.clampRequestedTemperature(parsedTemperature);
        if (temperatureBox != null)
        {
            temperatureBox.setValue(Integer.toString(clampedTemperature));
        }

        sendTemperature(clampedTemperature);
        updateButtonState();
    }

    @Nullable
    private Integer parseTemperature()
    {
        if (temperatureBox == null)
        {
            return null;
        }

        final String value = temperatureBox.getValue().trim();
        if (value.isEmpty() || "-".equals(value))
        {
            return null;
        }

        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException ignored)
        {
            return null;
        }
    }

    private void sendTemperature(int temperature)
    {
        final CompoundTag tag = new CompoundTag();
        tag.putInt(ClimateStationTemperatureMenu.TEMPERATURE_NBT_KEY, temperature);
        PacketDistributor.sendToServer(new ScreenButtonPacket(ClimateStationTemperatureMenu.BUTTON_SET_TEMPERATURE, tag));
    }

    private void updateButtonState()
    {
        final Integer parsed = parseTemperature();
        final boolean valid = parsed != null;
        final boolean inRange = valid && menu.clampRequestedTemperature(parsed) == parsed;

        if (temperatureBox != null)
        {
            temperatureBox.setTextColor(!valid ? 0xFF8080 : inRange ? 0xE0E0E0 : 0xFFD080);
        }
        if (saveButton != null)
        {
            saveButton.active = valid;
        }
    }

    private String flgp$getHeatingFormula()
    {
        return "%d x %s x %d -> +%d\u00B0C".formatted(
            menu.getHeatingItemCount(),
            flgp$formatFactor(menu.getHeatingUnitFactor()),
            menu.getBaseControlRange(),
            menu.getHeatingControlRange()
        );
    }

    private String flgp$getCoolingFormula()
    {
        final String factor = menu.hasCoolingItem()
            ? flgp$formatFactor(menu.getCoolingUnitFactor())
            : Component.translatable("screen.firmalife_greenhouse_patch.factor_placeholder").getString();
        return "%d x %s x %d -> -%d\u00B0C".formatted(
            menu.getCoolingItemCount(),
            factor,
            menu.getBaseControlRange(),
            menu.getCoolingControlRange()
        );
    }

    private List<Component> flgp$getHeatingTooltip()
    {
        final List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("screen.firmalife_greenhouse_patch.heating_slot"));
        lines.add(Component.translatable("screen.firmalife_greenhouse_patch.tooltip.heating_hint"));
        lines.add(Component.translatable("screen.firmalife_greenhouse_patch.tooltip.heating_rule"));
        lines.add(Component.translatable("screen.firmalife_greenhouse_patch.tooltip.formula_result", flgp$getHeatingFormula()));
        return lines;
    }

    private List<Component> flgp$getCoolingTooltip()
    {
        final List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("screen.firmalife_greenhouse_patch.cooling_slot"));
        lines.add(Component.translatable("screen.firmalife_greenhouse_patch.tooltip.cooling_hint"));
        lines.add(Component.translatable("screen.firmalife_greenhouse_patch.tooltip.cooling_rule"));
        if (menu.hasCoolingItem())
        {
            lines.add(Component.translatable("screen.firmalife_greenhouse_patch.tooltip.cooling_current", Component.translatable(menu.getCoolingItemNameKey()), flgp$formatFactor(menu.getCoolingUnitFactor())));
        }
        lines.add(Component.translatable("screen.firmalife_greenhouse_patch.tooltip.formula_result", flgp$getCoolingFormula()));
        return lines;
    }

    private List<Component> flgp$getStatusTooltip()
    {
        final List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(menu.isCellarMode() ? "screen.firmalife_greenhouse_patch.tooltip.cellar_summary" : "screen.firmalife_greenhouse_patch.tooltip.greenhouse_summary", Component.translatable(menu.getStructureNameKey()), menu.getBaseControlRange()));
        lines.add(Component.translatable("screen.firmalife_greenhouse_patch.tooltip.status_ambient", menu.getAmbientTemperature()));
        lines.add(Component.translatable("screen.firmalife_greenhouse_patch.tooltip.status_effective", menu.getEffectiveTemperature()));
        if (menu.isCellarMode())
        {
            lines.add(Component.translatable("screen.firmalife_greenhouse_patch.tooltip.current_preservation", flgp$formatFactor(menu.getCellarPreservationMultiplier()), menu.getCellarDecayPercent()));
        }
        lines.add(Component.translatable("screen.firmalife_greenhouse_patch.tooltip.status_range", menu.getMinAllowedTemperature(), menu.getMaxAllowedTemperature()));
        lines.add(Component.translatable("screen.firmalife_greenhouse_patch.tooltip.status_rounding"));
        return lines;
    }

    private String flgp$formatFactor(float factor)
    {
        if (Math.abs(factor - Math.round(factor)) < 0.0001f)
        {
            return Integer.toString(Math.round(factor));
        }
        if (Math.abs(factor * 10f - Math.round(factor * 10f)) < 0.0001f)
        {
            return String.format(Locale.ROOT, "%.1f", factor);
        }
        return String.format(Locale.ROOT, "%.2f", factor);
    }

    private List<FormattedCharSequence> flgp$getTooltipText(List<Component> lines)
    {
        return lines.stream().map(Component::getVisualOrderText).toList();
    }

    private void flgp$drawScaledCenteredLine(GuiGraphics graphics, Component text, int centerX, int y, float scale, int color)
    {
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.drawString(font, text, Mth.floor(centerX / scale - font.width(text) / 2f), Mth.floor(y / scale), color, false);
        graphics.pose().popPose();
    }
}
