package io.github.riveyder.shahed.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Only opened in response to the server's explicit destruction reason. */
public final class SignalLostScreen extends Screen {
    private final long opened=System.nanoTime();
    public SignalLostScreen() { super(Component.literal("SIGNAL LOST")); }
    @Override protected void init() {
        var replay=addRenderableWidget(Button.builder(Component.literal("REPLAY"),button->playReplay())
                .bounds(width/2-104,height/2+24,100,20).build());
        replay.active=canReplay();
        var slow=addRenderableWidget(Button.builder(Component.literal("REPLAY 0.25x"),button->ReplayController.request(true))
                .bounds(width/2-104,height/2+48,208,20).build());
        slow.active=canReplay();
        addRenderableWidget(Button.builder(Component.literal("EXIT"),button->onClose())
                .bounds(width/2+4,height/2+24,100,20).build());
    }
    private boolean canReplay() { return ReplayController.available(); }
    private void playReplay() { ReplayController.request(false); }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta) {
        graphics.fill(0,0,width,height,0xFF050908);
        long elapsed=(System.nanoTime()-opened)/1_000_000;
        int shift=(int)(elapsed/40%19);
        for(int y=shift;y<height;y+=19) graphics.fill(0,y,width,y+2,0x303BA68A);
        for(int i=0;i<7;i++) {
            int y=(int)((elapsed/13+i*71)%Math.max(1,height));
            graphics.fill(0,y,width,y+1,0x557CA99A);
        }
        graphics.centeredText(font,title,width/2,height/2-32,0xFFE4FFF2);
        graphics.centeredText(font,Component.literal("DRONE DESTROYED"),width/2,height/2-16,0xFF85B7A2);
        graphics.centeredText(font,Component.literal(ReplayController.blackBox()),width/2,height/2+2,0xFFB7D5C7);
        if(FpvController.settings().humor) graphics.centeredText(font,Component.literal("Гарантия аннулирована. КРИВЫЕ РУКИ AEROSPACE"),
                width/2,height-22,0xFF82948C);
        super.extractRenderState(graphics,mouseX,mouseY,delta);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() {
        minecraft.gui.setScreen(null);
        FpvController.beginReturnFade();
    }
}
