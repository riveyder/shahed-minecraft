package io.github.riveyder.shahed.client;

import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Responsive, wrapped pages; actual user key assignments are resolved whenever the screen opens. */
public final class FpvHelpScreen extends Screen {
    private final Screen parent;
    private final List<FormattedCharSequence> lines=new ArrayList<>();
    private int page,perPage,pages;
    public FpvHelpScreen(Screen parent){super(Component.translatable("help.shahed.title"));this.parent=parent;}
    @Override protected void init(){
        lines.clear();int w=Math.min(400,width-28);
        List<Component> sections=List.of(
                Component.translatable("help.shahed.obtain"),Component.translatable("help.shahed.bind"),
                Component.translatable("help.shahed.flight",FpvKeys.FORWARD.getTranslatedKeyMessage(),FpvKeys.BACKWARD.getTranslatedKeyMessage(),
                        FpvKeys.LEFT.getTranslatedKeyMessage(),FpvKeys.RIGHT.getTranslatedKeyMessage(),FpvKeys.UP.getTranslatedKeyMessage(),
                        FpvKeys.DOWN.getTranslatedKeyMessage(),FpvKeys.BRAKE.getTranslatedKeyMessage(),FpvKeys.EXIT.getTranslatedKeyMessage()),
                Component.translatable("help.shahed.camera",FpvKeys.SETTINGS.getTranslatedKeyMessage(),FpvKeys.HUD_MODE.getTranslatedKeyMessage(),
                        FpvKeys.YAW_LEFT.getTranslatedKeyMessage(),FpvKeys.YAW_RIGHT.getTranslatedKeyMessage(),FpvKeys.PITCH_UP.getTranslatedKeyMessage(),FpvKeys.PITCH_DOWN.getTranslatedKeyMessage()),
                Component.translatable("help.shahed.fleet",FpvKeys.FORWARD.getTranslatedKeyMessage(),FpvKeys.BACKWARD.getTranslatedKeyMessage(),
                        FpvKeys.LEFT.getTranslatedKeyMessage(),FpvKeys.RIGHT.getTranslatedKeyMessage(),FpvKeys.UP.getTranslatedKeyMessage(),
                        FpvKeys.DOWN.getTranslatedKeyMessage(),FpvKeys.BRAKE.getTranslatedKeyMessage()),
                Component.translatable("help.shahed.payload",FpvKeys.RELEASE_PAYLOAD.getTranslatedKeyMessage()),
                Component.translatable("help.shahed.fleet_safety"),Component.translatable("help.shahed.racing"),Component.translatable("help.shahed.challenges"),
                Component.translatable("race.command_help"),Component.translatable("race.editor_help"));
        for(var section:sections){lines.addAll(font.split(section,w));lines.add(FormattedCharSequence.EMPTY);}
        perPage=Math.max(1,(height-85)/11);pages=Math.max(1,(lines.size()+perPage-1)/perPage);page=Math.min(page,pages-1);
        var back=addRenderableWidget(Button.builder(Component.literal("‹"),b->{page--;rebuildWidgets();}).bounds(width/2-110,height-32,35,20).build());back.active=page>0;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(width/2-70,height-32,140,20).build());
        var next=addRenderableWidget(Button.builder(Component.literal("›"),b->{page++;rebuildWidgets();}).bounds(width/2+75,height-32,35,20).build());next.active=page+1<pages;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float t){
        super.extractRenderState(g,x,y,t);g.centeredText(font,title,width/2,10,0xFF9DFFC0);
        g.centeredText(font,"KRIVYE RUKI AEROSPACE · "+(page+1)+"/"+pages,width/2,25,0xFFB8DCC8);
        int left=(width-Math.min(400,width-28))/2;
        for(int i=page*perPage;i<Math.min(lines.size(),(page+1)*perPage);i++)g.text(font,lines.get(i),left,43+(i-page*perPage)*11,0xFFF0F5F3);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
}
