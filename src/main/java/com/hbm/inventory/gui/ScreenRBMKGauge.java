package com.hbm.inventory.gui;

import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKGauge;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Screwdriver-opened RBMK gauge configuration panel; it is not a container menu. */
public final class ScreenRBMKGauge extends CompatScreen {
    private static final Identifier TEX = Ids.of("hbm", "textures/gui/machine/gui_rbmk_gauge.png");
    private final BlockEntityRBMKGauge gauge;
    private final EditBox[] color = new EditBox[BlockEntityRBMKGauge.GAUGES];
    private final EditBox[] label = new EditBox[BlockEntityRBMKGauge.GAUGES];
    private final EditBox[] rtty = new EditBox[BlockEntityRBMKGauge.GAUGES];
    private final EditBox[] min = new EditBox[BlockEntityRBMKGauge.GAUGES];
    private final EditBox[] max = new EditBox[BlockEntityRBMKGauge.GAUGES];
    private final boolean[] active = new boolean[BlockEntityRBMKGauge.GAUGES], polling = new boolean[BlockEntityRBMKGauge.GAUGES];
    private int left, top;
    public ScreenRBMKGauge(BlockEntityRBMKGauge gauge) { super(Component.translatable("container.rbmkGauge")); this.gauge = gauge; }
    @Override protected void init() { left=(width-256)/2; top=(height-204)/2; for(int i=0;i<BlockEntityRBMKGauge.GAUGES;i++){var u=gauge.gauges[i]; String hex=Integer.toHexString(u.color); while(hex.length()<6)hex="0"+hex; color[i]=field(31,59+i*36,64,hex,6);label[i]=field(179,59+i*36,64,u.label,15);rtty[i]=field(31,77+i*36,64,u.rtty,10);min[i]=field(125,77+i*36,44,""+u.min,32);max[i]=field(199,77+i*36,44,""+u.max,32);active[i]=u.active;polling[i]=u.polling;}}
    private EditBox field(int x,int y,int w,String value,int length){EditBox b=new EditBox(font,left+x,top+y,w,14,Component.empty());b.setBordered(false);b.setTextColor(0xFF00FF00);b.setTextColorUneditable(0xFF00FF00);b.setMaxLength(length);b.setValue(value==null?"":value);addRenderableWidget(b);return b;}
    @Override protected void extractBackgroundCompat(GuiGraphicsExtractor g,int x,int y,float p){GuiDraw.blit(g,TEX,left,top,0,0,256,204,256,256);for(int i=0;i<active.length;i++){if(active[i])GuiDraw.blit(g,TEX,left+111,top+54+i*36,18,204,16,16,256,256);if(polling[i])GuiDraw.blit(g,TEX,left+128,top+53+i*36,0,204,18,18,256,256);}}
    @Override protected void extractForegroundCompat(GuiGraphicsExtractor g,int x,int y,float p){GuiDraw.text(g,font,title,left+(256-font.width(title))/2,top+6,0xFF404040,false);}
    @Override protected boolean mouseClickedCompat(MouseInput e,boolean d){for(int i=0;i<active.length;i++){if(over(e,111,54+i*36,16,16)){active[i]=!active[i];return true;}if(over(e,128,53+i*36,18,18)){polling[i]=!polling[i];return true;}}if(over(e,209,17,18,18)){save();return true;}return false;}
    private boolean over(MouseInput e,int x,int y,int w,int h){return e.x()>=left+x&&e.x()<left+x+w&&e.y()>=top+y&&e.y()<top+y+h;}
    private void save(){CompoundTag n=new CompoundTag();byte a=0,p=0;for(int i=0;i<active.length;i++){if(active[i])a|=(byte)(1<<i);if(polling[i])p|=(byte)(1<<i);try{n.putInt("color"+i,Integer.parseInt(color[i].getValue(),16));}catch(Exception ignored){}n.putString("label"+i,label[i].getValue());n.putString("rtty"+i,rtty[i].getValue());try{n.putInt("min"+i,Integer.parseInt(min[i].getValue()));}catch(Exception ignored){}try{n.putInt("max"+i,Integer.parseInt(max[i].getValue()));}catch(Exception ignored){}}n.putByte("active",a);n.putByte("polling",p);GolemNetwork.sendToServer(ControlEventPayload.CODEC,new ControlEventPayload(gauge.getBlockPos(),n));}
    @Override public boolean isPauseScreen(){return false;}
}
