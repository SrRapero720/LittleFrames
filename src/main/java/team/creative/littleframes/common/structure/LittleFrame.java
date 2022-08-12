package team.creative.littleframes.common.structure;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager.CullFace;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import team.creative.creativecore.common.gui.creator.GuiCreator;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.box.AlignedBox;
import team.creative.creativecore.common.util.math.box.BoxCorner;
import team.creative.creativecore.common.util.math.box.BoxFace;
import team.creative.creativecore.common.util.math.vec.VectorUtils;
import team.creative.creativecore.common.util.mc.ColorUtils;
import team.creative.littleframes.LittleFrames;
import team.creative.littleframes.client.display.FrameDisplay;
import team.creative.littleframes.client.gui.GuiLittleFrame;
import team.creative.littleframes.client.texture.TextureCache;
import team.creative.littleframes.common.block.BECreativeFrame;
import team.creative.littleframes.common.packet.LittleFramePacket;
import team.creative.littletiles.common.block.little.tile.LittleTileContext;
import team.creative.littletiles.common.block.little.tile.parent.IStructureParentCollection;
import team.creative.littletiles.common.gui.handler.LittleStructureGuiCreator;
import team.creative.littletiles.common.structure.LittleStructure;
import team.creative.littletiles.common.structure.LittleStructureType;
import team.creative.littletiles.common.structure.directional.StructureDirectional;
import team.creative.littletiles.common.structure.relative.StructureRelative;

public class LittleFrame extends LittleStructure {
    
    public static final LittleStructureGuiCreator GUI = GuiCreator
            .register("little_frame", new LittleStructureGuiCreator((nbt, player, structure) -> new GuiLittleFrame((LittleFrame) structure)));
    
    @StructureDirectional(color = ColorUtils.CYAN)
    public StructureRelative frame;
    
    @StructureDirectional
    public Facing facing;
    
    @StructureDirectional
    public Vector3f topRight;
    
    private String url = "";
    public float brightness = 1;
    public float alpha = 1;
    
    public int renderDistance = 64;
    
    public FitMode fitMode = FitMode.CROP;
    
    public float volume = 1;
    public boolean loop = true;
    public int tick = 0;
    public boolean playing = true;
    
    @OnlyIn(Dist.CLIENT)
    public TextureCache cache;
    
    @OnlyIn(Dist.CLIENT)
    public FrameDisplay display;
    
    public LittleFrame(LittleStructureType type, IStructureParentCollection mainBlock) {
        super(type, mainBlock);
    }
    
    @OnlyIn(Dist.CLIENT)
    public boolean isURLEmpty() {
        return url.isEmpty();
    }
    
    @OnlyIn(Dist.CLIENT)
    public String getURL() {
        return BECreativeFrame.replaceVariables(url);
    }
    
    public String getRealURL() {
        return url;
    }
    
    public void setURL(String url) {
        this.url = url;
    }
    
    @OnlyIn(Dist.CLIENT)
    public FrameDisplay requestDisplay() {
        String url = getURL();
        if (cache == null || !cache.url.equals(url)) {
            cache = TextureCache.get(url);
            if (display != null)
                display.release();
            display = null;
        }
        if (display != null)
            return display;
        if (cache.ready())
            return display = cache.createDisplay(url, volume, loop);
        return null;
    }
    
    public void play() {
        playing = true;
        LittleFrames.NETWORK.sendToClient(new LittleFramePacket(getStructureLocation(), playing, tick), getLevel(), getPos());
    }
    
    public void pause() {
        playing = false;
        LittleFrames.NETWORK.sendToClient(new LittleFramePacket(getStructureLocation(), playing, tick), getLevel(), getPos());
    }
    
    public void stop() {
        playing = false;
        tick = 0;
        LittleFrames.NETWORK.sendToClient(new LittleFramePacket(getStructureLocation(), playing, tick), getLevel(), getPos());
    }
    
    @Override
    protected void loadExtra(CompoundTag nbt) {
        url = nbt.getString("url");
        if (nbt.contains("render"))
            renderDistance = nbt.getInt("render");
        else
            renderDistance = 64;
        if (nbt.contains("alpha"))
            alpha = nbt.getFloat("alpha");
        else
            alpha = 1;
        if (nbt.contains("brightness"))
            brightness = nbt.getFloat("brightness");
        else
            brightness = 1;
        
        volume = nbt.getFloat("volume");
        playing = nbt.getBoolean("playing");
        tick = nbt.getInt("tick");
        loop = nbt.getBoolean("loop");
        fitMode = FitMode.values()[nbt.getInt("fitMode")];
    }
    
    @Override
    protected void saveExtra(CompoundTag nbt) {
        nbt.putString("url", url);
        nbt.putInt("render", renderDistance);
        nbt.putFloat("alpha", alpha);
        nbt.putFloat("brightness", brightness);
        
        nbt.putFloat("volume", volume);
        nbt.putBoolean("playing", playing);
        nbt.putInt("tick", tick);
        nbt.putBoolean("loop", loop);
        nbt.putInt("fitMode", fitMode.ordinal());
    }
    
    @Override
    public boolean canInteract() {
        return true;
    }
    
    @Override
    public InteractionResult use(Level level, LittleTileContext context, BlockPos pos, Player player, BlockHitResult result) {
        GUI.open(player, this);
        return InteractionResult.SUCCESS;
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderTick(PoseStack pose, MultiBufferSource buffer, BlockPos pos, float partialTickTime) {
        if (isURLEmpty() || alpha == 0) {
            if (display != null)
                display.release();
            return;
        }
        
        FrameDisplay display = requestDisplay();
        if (display == null)
            return;
        
        display.prepare(getURL(), volume * Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.MASTER), playing, loop, tick);
        
        GlStateManager.enableBlend();
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GlStateManager.disableLighting();
        GlStateManager.color(brightness, brightness, brightness, alpha);
        int texture = display.texture();
        
        GlStateManager.cullFace(CullFace.BACK);
        GlStateManager.bindTexture(texture);
        
        GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        
        GlStateManager.pushMatrix();
        
        GlStateManager.translate(x, y, z);
        
        AlignedBox box = frame.getBox().getCube(frame.getContext());
        BoxFace face = BoxFace.get(facing);
        if (facing.getAxisDirection() == AxisDirection.POSITIVE)
            box.setMax(facing.getAxis(), box.getMin(facing.getAxis()) + 0.01F);
        else
            box.setMin(facing.getAxis(), box.getMax(facing.getAxis()) - 0.01F);
        Axis uAxis = face.getTexUAxis();
        Axis vAxis = face.getTexVAxis();
        if (fitMode == FitMode.CROP) {
            float width = box.getSize(uAxis);
            float height = box.getSize(vAxis);
            float videoRatio = display.getWidth() / (float) display.getHeight();
            float ratio = width / height;
            if (ratio > videoRatio)
                box.shrink(uAxis, width - height * videoRatio);
            else if (ratio < videoRatio)
                box.shrink(vAxis, height - width / videoRatio);
        }
        
        GlStateManager.enableRescaleNormal();
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.getBuffer();
        builder.begin(GL11.GL_POLYGON, DefaultVertexFormats.POSITION_TEX);
        for (BoxCorner corner : face.corners)
            builder.pos(box.getValueOfFacing(corner.x), box.getValueOfFacing(corner.y), box.getValueOfFacing(corner.z))
                    
                    .tex(corner.isFacingPositive(uAxis) != (VectorUtils.get(uAxis, topRight) > 0) ? 1 : 0, corner
                            .isFacingPositive(vAxis) != (VectorUtils.get(vAxis, topRight) > 0) ? 1 : 0)
                    .endVertex();
        tessellator.draw();
        
        GlStateManager.popMatrix();
        
        GlStateManager.cullFace(CullFace.BACK);
        
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public double getMaxRenderDistance() {
        return renderDistance;
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public AABB getRenderBoundingBox() {
        return frame.getBox().getBB(frame.getGrid());
    }
    
    @Override
    public void tick() {
        super.tick();
        if (playing)
            tick++;
    }
    
    @Override
    public void unload() {
        super.unload();
        if (getLevel().isClientSide && display != null)
            display.release();
    }
    
    public static enum FitMode {
        CROP,
        STRETCH;
    }
    
}
