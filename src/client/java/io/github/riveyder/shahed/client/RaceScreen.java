package io.github.riveyder.shahed.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Public course browser; admin editing stays in small, documented commands. */
public final class RaceScreen extends Screen {
    private final Screen parent;
    private int page;
    private String selected="";
    public RaceScreen(Screen parent){super(Component.translatable("race.aircraft",FpvController.aircraftCategoryLabel()));this.parent=parent;RaceClient.request(0,"");}
    public void refresh(){rebuildWidgets();}
    @Override protected void init(){
        var courses=RaceClient.courses();int perPage=Math.max(1,(height-132)/24),pages=Math.max(1,(courses.size()+perPage-1)/perPage);
        page=Math.min(page,pages-1);int w=Math.min(320,width-20),x=(width-w)/2;
        for(int i=page*perPage;i<Math.min(courses.size(),(page+1)*perPage);i++){
            var row=courses.get(i);int y=48+(i-page*perPage)*24;
            var b=Button.builder(Component.literal((row.id().equals(selected)?"> ":"")+row.id()+" · ")
                    .append(Component.translatable("race.kind."+row.kind())),button->{selected=row.id();rebuildWidgets();}).bounds(x,y,w,20).build();
            b.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("race.course_details",row.gates(),RaceClient.time(row.best()),RaceClient.time(row.record()))));addRenderableWidget(b);
        }
        int y=height-77,third=(w-4)/3;
        var join=addRenderableWidget(Button.builder(Component.translatable("race.join"),b->RaceClient.request(1,selected)).bounds(x,y,third,20).build());join.active=!selected.isEmpty();
        var preview=addRenderableWidget(Button.builder(Component.translatable("race.preview"),b->RaceClient.request(3,selected)).bounds(x+third+2,y,third,20).build());preview.active=!selected.isEmpty();
        addRenderableWidget(Button.builder(Component.translatable("race.cancel"),b->RaceClient.request(2,"")).bounds(x+2*(third+2),y,third,20).build());
        var prev=addRenderableWidget(Button.builder(Component.literal("‹"),b->{page--;rebuildWidgets();}).bounds(x,height-53,30,20).build());prev.active=page>0;
        var next=addRenderableWidget(Button.builder(Component.literal("›"),b->{page++;rebuildWidgets();}).bounds(x+w-30,height-53,30,20).build());next.active=page+1<pages;
        addRenderableWidget(Button.builder(Component.translatable("race.refresh"),b->RaceClient.request(0,"")).bounds(x+34,height-53,(w-72)/2,20).build());
        addRenderableWidget(Button.builder(Component.translatable("help.shahed.title"),b->minecraft.gui.setScreen(new FpvHelpScreen(this))).bounds(x+w/2+2,height-53,(w-72)/2,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->onClose()).bounds(x,height-29,w,20).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float t){
        super.extractRenderState(g,x,y,t);g.centeredText(font,title,width/2,10,0xFF9DFFC0);
        var notice=RaceClient.notice().getString();
        g.centeredText(font,font.plainSubstrByWidth(notice.isEmpty()?Component.translatable("race.training_hint").getString():notice,width-16),width/2,29,0xFFB8DCC8);
        if(RaceClient.courses().isEmpty())g.centeredText(font,Component.translatable("race.empty"),width/2,55,0xFFF0F5F3);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
}
