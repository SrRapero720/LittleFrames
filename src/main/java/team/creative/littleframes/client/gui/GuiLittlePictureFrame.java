package team.creative.littleframes.client.gui;

import me.srrapero720.watermedia.api.image.ImageCache;
import me.srrapero720.watermedia.api.image.ImageFetch;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.network.chat.Component;
import team.creative.creativecore.common.gui.Align;
import team.creative.creativecore.common.gui.GuiLayer;
import team.creative.creativecore.common.gui.GuiParent;
import team.creative.creativecore.common.gui.VAlign;
import team.creative.creativecore.common.gui.controls.parent.GuiColumn;
import team.creative.creativecore.common.gui.controls.parent.GuiLabeledControl;
import team.creative.creativecore.common.gui.controls.parent.GuiRow;
import team.creative.creativecore.common.gui.controls.parent.GuiTable;
import team.creative.creativecore.common.gui.controls.simple.GuiButton;
import team.creative.creativecore.common.gui.controls.simple.GuiButtonIcon;
import team.creative.creativecore.common.gui.controls.simple.GuiCheckBox;
import team.creative.creativecore.common.gui.controls.simple.GuiDuration;
import team.creative.creativecore.common.gui.controls.simple.GuiLabel;
import team.creative.creativecore.common.gui.controls.simple.GuiSlider;
import team.creative.creativecore.common.gui.controls.simple.GuiStateButtonMapped;
import team.creative.creativecore.common.gui.controls.simple.GuiSteppedSlider;
import team.creative.creativecore.common.gui.controls.simple.GuiTextfield;
import team.creative.creativecore.common.gui.flow.GuiFlow;
import team.creative.creativecore.common.gui.style.Icon;
import team.creative.creativecore.common.gui.sync.GuiSyncLocal;
import team.creative.creativecore.common.util.mc.ColorUtils;
import team.creative.creativecore.common.util.text.TextBuilder;
import team.creative.creativecore.common.util.text.TextMapBuilder;
import team.creative.littleframes.LittleFrames;
import team.creative.littleframes.common.structure.LittlePictureFrame;
import team.creative.littleframes.common.structure.LittlePictureFrame.FitMode;

public class GuiLittlePictureFrame extends GuiLayer {
    
    public LittlePictureFrame frame;
    
    public GuiTextfield url;
    
    public final GuiSyncLocal<EndTag> PLAY = getSyncHolder().register("play", x -> frame.play());
    public final GuiSyncLocal<EndTag> PAUSE = getSyncHolder().register("pause", x -> frame.pause());
    public final GuiSyncLocal<EndTag> STOP = getSyncHolder().register("stop", x -> frame.stop());
    
    public final GuiSyncLocal<CompoundTag> SET_DATA = getSyncHolder().register("set_data", nbt -> {
        String url = nbt.getString("url");
        if (LittleFrames.CONFIG.canUse(getPlayer(), url)) {
            frame.setURL(url);
            frame.renderDistance = Math.min(LittleFrames.CONFIG.maxRenderDistance, nbt.getInt("render"));
            frame.fitMode = LittlePictureFrame.FitMode.values()[nbt.getInt("fit")];
            frame.loop = nbt.getBoolean("loop");
            frame.volume = nbt.getFloat("volume");
            frame.minDistance = nbt.getFloat("min");
            frame.maxDistance = nbt.getFloat("max");
            frame.alpha = nbt.getFloat("transparency");
            frame.brightness = nbt.getFloat("brightness");
            frame.refreshCounter = frame.refreshInterval = nbt.getInt("refresh");
        }
        
        frame.updateStructure();
    });
    
    public GuiLittlePictureFrame(LittlePictureFrame frame) {
        super("little_frame", 200, 180);
        this.frame = frame;
    }
    
    @Override
    public void create() {
        GuiButton save = new GuiButton("save", x -> {
            CompoundTag nbt = new CompoundTag();
            GuiTextfield url = get("url");
            GuiSteppedSlider renderDistance = get("distance");
            
            GuiStateButtonMapped<FitMode> fit = get("fit");
            
            GuiSlider transparency = get("transparency");
            GuiSlider brightness = get("brightness");
            
            GuiCheckBox loop = get("loop");
            GuiSlider volume = get("volume");
            GuiSteppedSlider min = get("range_min");
            GuiSteppedSlider max = get("range_max");
            
            GuiCheckBox autoRefresh = get("autoRefresh");
            GuiDuration duration = get("duration");
            
            nbt.putInt("fit", fit.getState());
            
            nbt.putInt("render", (int) renderDistance.getValue());
            
            nbt.putFloat("transparency", (float) transparency.getValue());
            nbt.putFloat("brightness", (float) brightness.getValue());
            
            nbt.putBoolean("loop", loop.value);
            nbt.putFloat("volume", (float) volume.getValue());
            nbt.putFloat("min", min.getIntValue());
            nbt.putFloat("max", max.getIntValue());
            
            nbt.putString("url", url.getText());
            
            nbt.putInt("refresh", autoRefresh.value ? duration.getDuration() : -1);
            
            nbt.putInt("type", 0);
            SET_DATA.send(nbt);
        });
        
        save.setTranslate("gui.save");
        
        align = Align.STRETCH;
        flow = GuiFlow.STACK_Y;
        
        url = new GuiUrlTextfield(save, "url", frame.getRealURL());
        url.setMaxStringLength(512);
        add(url);
        GuiLabel error = new GuiLabel("error").setDefaultColor(ColorUtils.RED);
        if (frame.isClient() && frame.cache != null) {
            if (frame.cache.getStatus().equals(ImageCache.Status.FAILED)) {
                Exception e = frame.cache.getException();
                if (frame.cache.isVideo()) {
                    if (!LittleFrames.CONFIG.useVLC)
                        error.setTitle(Component.literal("Image not found"));
                } else {
                    if (e instanceof ImageFetch.GifDecodingException)
                        error.setTranslate("download.exception.gif");
                    else if (e.getMessage().startsWith("Server returned HTTP response code: 403"))
                        error.setTranslate("download.exception.forbidden");
                    else if (e.getMessage().startsWith("Server returned HTTP response code: 404"))
                        error.setTranslate("download.exception.notfound");
                    else
                        error.setTranslate("download.exception.invalid");
                }
            }
        }
        add(error);
        GuiStateButtonMapped<FitMode> button = new GuiStateButtonMapped<FitMode>("fit", new TextMapBuilder<FitMode>().addComponent(FitMode.values(), x -> Component.translatable(
            "gui.little_frame.fitmode." + x.name())));
        button.select(frame.fitMode);
        add(button);
        
        GuiTable table = new GuiTable();
        add(table);
        GuiColumn left;
        GuiColumn right;
        
        table.addRow(new GuiRow(left = new GuiColumn(), right = new GuiColumn()));
        left.add(new GuiLabel("t_label").setTitle(Component.translatable("gui.creative_frame.transparency").append(":")));
        right.add(new GuiSlider("transparency", frame.alpha, 0, 1).setExpandableX());
        
        table.addRow(new GuiRow(left = new GuiColumn(), right = new GuiColumn()));
        left.add(new GuiLabel("b_label").setTitle(Component.translatable("gui.creative_frame.brightness").append(":")));
        right.add(new GuiSlider("brightness", frame.brightness, 0, 1).setExpandableX());
        
        table.addRow(new GuiRow(left = new GuiColumn(), right = new GuiColumn()));
        left.add(new GuiLabel("d_label").setTitle(Component.translatable("gui.creative_frame.distance").append(":")));
        right.add(new GuiSteppedSlider("distance", frame.renderDistance, 5, 1024).setExpandableX());
        
        GuiParent play = new GuiParent(GuiFlow.STACK_X);
        add(play);
        
        play.add(new GuiButtonIcon("play", Icon.PLAY, x -> PLAY.send(EndTag.INSTANCE)));
        play.add(new GuiButtonIcon("pause", Icon.PAUSE, x -> PAUSE.send(EndTag.INSTANCE)));
        play.add(new GuiButtonIcon("stop", Icon.STOP, x -> STOP.send(EndTag.INSTANCE)));
        
        add(new GuiCheckBox("loop", frame.loop).setTranslate("gui.creative_frame.loop"));
        add(new GuiLabeledControl("gui.creative_frame.volume", new GuiSlider("volume", frame.volume, 0, 1).setExpandableX()));
        
        GuiParent range = new GuiParent();
        add(range);
        range.add(new GuiLabel("range_label").setTranslate("gui.creative_frame.range"));
        range.add(new GuiSteppedSlider("range_min", (int) frame.minDistance, 0, 512).setExpandableX());
        range.add(new GuiSteppedSlider("range_max", (int) frame.maxDistance, 0, 512).setExpandableX());
        
        GuiParent refresh = new GuiParent();
        refresh.spacing = 10;
        add(refresh.setVAlign(VAlign.CENTER));
        refresh.add(new GuiCheckBox("autoRefresh", frame.refreshInterval > 0).setTranslate("gui.creative_frame.autoReload"));
        refresh.add(new GuiDuration("duration", frame.refreshInterval, false, true, true, true));
        
        GuiParent bottom = new GuiParent(GuiFlow.STACK_X);
        bottom.align = Align.RIGHT;
        add(bottom);
        save.setEnabled(LittleFrames.CONFIG.canUse(getPlayer(), url.getText()));
        bottom.add(save);
        bottom.add(new GuiButton("reload", x -> {
            if (Screen.hasShiftDown())
                ImageCache.reloadAll();
            else if (frame.cache != null)
                frame.cache.reload();
        }).setTranslate("gui.creative_frame.reload").setTooltip(new TextBuilder().translate("gui.creative_frame.reloadtooltip").build()));
    }
    
}