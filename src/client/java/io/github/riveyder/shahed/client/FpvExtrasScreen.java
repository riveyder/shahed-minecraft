package io.github.riveyder.shahed.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class FpvExtrasScreen extends Screen {
    private final Screen parent;
    private final FpvSettings settings;
    public FpvExtrasScreen(Screen parent,FpvSettings settings) {
        super(Component.literal("Дополнительные параметры"));this.parent=parent;this.settings=settings;
    }
    @Override protected void init() {
        int x=width/2-120,y=Math.max(30,height/2-50);
        addRenderableWidget(Button.builder(label("Ночное видение",settings.nightVision),b->{
            settings.nightVision=!settings.nightVision;b.setMessage(label("Ночное видение",settings.nightVision));
        }).bounds(x,y,240,20).build());
        addRenderableWidget(Button.builder(label("Камера режиссёра в повторе",settings.directorCamera),b->{
            settings.directorCamera=!settings.directorCamera;b.setMessage(label("Камера режиссёра в повторе",settings.directorCamera));
        }).bounds(x,y+26,240,20).build());
        addRenderableWidget(Button.builder(label("Юмор производителя",settings.humor),b->{
            settings.humor=!settings.humor;b.setMessage(label("Юмор производителя",settings.humor));
        }).bounds(x,y+52,240,20).build());
        addRenderableWidget(Button.builder(Component.literal("Готово"),b->onClose()).bounds(x,y+84,240,20).build());
    }
    private Component label(String text,boolean enabled) {return Component.literal(text+": "+(enabled?"Вкл":"Выкл"));}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float t) {
        super.extractRenderState(g,x,y,t);g.centeredText(font,title,width/2,15,0xFFFFFFFF);
    }
    @Override public boolean isPauseScreen() {return false;}
    @Override public void onClose() {settings.save();minecraft.gui.setScreen(parent);}
    @Override public void removed() {settings.save();}
}
