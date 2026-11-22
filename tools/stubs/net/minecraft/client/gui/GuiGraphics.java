package net.minecraft.client.gui;
import java.util.List; import net.minecraft.network.chat.Component;
public class GuiGraphics { public void fill(int a,int b,int c,int d,int col){}
 public int drawString(Font f,String s,int x,int y,int c,boolean sh){return 0;} public int drawString(Font f,Component s,int x,int y,int c,boolean sh){return 0;}
 public void drawCenteredString(Font f,String s,int x,int y,int c){} public void renderComponentTooltip(Font f,List<Component> l,int x,int y){} }
