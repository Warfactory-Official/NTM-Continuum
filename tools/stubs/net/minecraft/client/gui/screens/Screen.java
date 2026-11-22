package net.minecraft.client.gui.screens;
public class Screen { protected int width,height; protected net.minecraft.client.gui.Font font; protected net.minecraft.network.chat.Component title;
 protected Screen(net.minecraft.network.chat.Component t){} public void render(net.minecraft.client.gui.GuiGraphics g,int a,int b,float c){} public void tick(){} public void onClose(){}
 public boolean isPauseScreen(){return true;} public boolean mouseClicked(double a,double b,int c){return false;} public boolean mouseDragged(double a,double b,int c,double d,double e){return false;}
 public boolean mouseReleased(double a,double b,int c){return false;} public boolean mouseScrolled(double a,double b,double c,double d){return false;} }
