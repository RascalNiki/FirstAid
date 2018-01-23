package ichttt.mods.firstaid.client;

import ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel;
import ichttt.mods.firstaid.client.gui.GuiHealthScreen;
import ichttt.mods.firstaid.client.tutorial.GuiTutorial;
import ichttt.mods.firstaid.client.util.EventCalendar;
import ichttt.mods.firstaid.common.FirstAidConfig;
import ichttt.mods.firstaid.common.damagesystem.capability.PlayerDataManager;
import ichttt.mods.firstaid.common.items.FirstAidItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientEventHandler {
    private static int id;

    @SubscribeEvent
    public static void clientTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (EventCalendar.isGuiFun()) {
            GuiHealthScreen.BED_ITEMSTACK.setItemDamage(id);
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.world != null && mc.world.getWorldTime() % 3 == 0) id++;
            if (id > 15) id = 0;
        }
    }

    @SubscribeEvent
    public static void onKeyPress(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        AbstractPlayerDamageModel damageModel = PlayerDataManager.getDamageModel(mc.player);
        if (ClientProxy.showWounds.isPressed()) {
            if (!damageModel.hasTutorial) {
                damageModel.hasTutorial = true;
                PlayerDataManager.tutorialDone.add(mc.player.getName());
                Minecraft.getMinecraft().displayGuiScreen(new GuiTutorial());
            }
            else {
                mc.displayGuiScreen(new GuiHealthScreen(damageModel));
            }
        }
    }

    @SubscribeEvent
    public static void preRender(RenderGameOverlayEvent.Pre event) {
        RenderGameOverlayEvent.ElementType type = event.getType();
        if (type == RenderGameOverlayEvent.ElementType.HEALTH && !FirstAidConfig.overlay.showVanillaHealthBar) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void renderOverlay(RenderGameOverlayEvent.Post event) {
        RenderGameOverlayEvent.ElementType type = event.getType();
        if (type == RenderGameOverlayEvent.ElementType.ALL || (type == RenderGameOverlayEvent.ElementType.TEXT && FirstAidConfig.overlay.position == 2)) {
            GuiIngameForge.renderHealth = FirstAidConfig.overlay.showVanillaHealthBar;
            HUDHandler.renderOverlay(event.getResolution());
        }
    }
}
