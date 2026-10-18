package io.github.riveyder.shahed.client;

import io.github.riveyder.shahed.control.DroneConfig;
import io.github.riveyder.shahed.network.*;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

/** Live personal preferences and explicit, revision-checked server policy changes. */
public final class FpvSettingsScreen extends Screen {
    private static final String[] CATEGORIES={"Взрывы","Полёт","Камера / HUD","Звук","Повтор","Мультиплеер","Последствия","Графика"};
    private final Screen parent;
    private final FpvSettings settings;
    private final List<IntFunction<AbstractWidget>> rows=new ArrayList<>();
    private int category,page;
    private double[] draft;
    private boolean admin;
    private boolean dirty;
    private String status="Ожидание настроек сервера…";
    private String area="Не назначена";
    public FpvSettingsScreen(Screen parent,FpvSettings settings) {
        super(Component.literal("KRIVYE RUKI AEROSPACE"));this.parent=parent;this.settings=settings;
        adopt(ServerSettingsClient.state());ServerSettingsClient.read();
    }
    private void adopt(SettingsStatePayload p) {
        var s=p==null?DroneConfig.DEFAULTS:p.settings();
        draft=new double[]{s.destroyBlocks()?1:0,s.explosionPower(),s.entityDamage()?1:0,s.damageScale(),
                s.particles()?1:0,s.sound()?1:0,s.maxBlocks(),s.stabilization()?1:0,s.dragMultiplier(),s.activeLimit(),s.spectators()?1:0,s.telemetryInterval(),s.realFire()?1:0,s.fireSpread()?1:0,s.effectScale(),s.audioRange()};
        if(p!=null) {admin=p.admin();status=p.status();area=p.area();}dirty=false;
    }
    private DroneConfig.Settings policy() {return new DroneConfig.Settings(draft[0]>0,(float)draft[1],draft[2]>0,(float)draft[3],
            draft[4]>0,draft[5]>0,(int)draft[6],draft[7]>0,(float)draft[8],(int)draft[9],draft[10]>0,(int)draft[11],draft[12]>0,draft[13]>0,(float)draft[14],(int)draft[15]).sanitized();}
    public void receive(SettingsStatePayload p) {
        adopt(p);rebuildWidgets();
        if(!p.nonce().equals(SettingsRequestPayload.NONE)) minecraft.gui.setScreen(new ConfirmScreen(confirmed->{
            minecraft.gui.setScreen(this);
            if(confirmed) ServerSettingsClient.request(p.confirmationAction(),policy(),p.nonce());
        },Component.literal(p.confirmationAction()==6?"Применить разрушительный режим?":"Выполнить TEST EXPLOSION?"),Component.literal(p.confirmationAction()==6
                ?p.status()+" Последующие аварии будут разрушать местность."
                :"Точка: "+area+". Мощность "+p.settings().explosionPower()
                +"; блоки: "+(p.settings().destroyBlocks()?"разрушение до "+p.settings().maxBlocks():"сохранить")
                +"; урон: "+(p.settings().entityDamage()?"включён":"выключен")+". Отойдите от точки. Действие необратимо.")));
    }
    @Override protected void init() {
        rows.clear();int tabWidth=Math.min(100,(width-16)/4),start=width/2-tabWidth*2;
        for(int i=0;i<CATEGORIES.length;i++) {int selected=i;var tab=addRenderableWidget(Button.builder(Component.literal(CATEGORIES[i]),b->{
            category=selected;page=0;rebuildWidgets();}).bounds(start+(i%4)*tabWidth,32+(i/4)*22,tabWidth-2,20).build());tab.active=i!=category;}
        switch(category) {
            case 0 -> {
                serverSlider("Интенсивность разрушения",1,1,32,false);serverToggle("Разрушение блоков",0);
                serverToggle("Урон сущностям",2);serverSlider("Множитель урона",3,0,1,false);
                serverToggle("Частицы взрыва",4);serverToggle("Звук взрыва",5);serverSlider("Лимит блоков",6,0,16384,true);
                for(int i=0;i<3;i++) {int preset=i;action("Набор "+new String[]{"SAFE","NORMAL","CHAOS"}[i],()->{
                    var s=policy().preset(preset);var old=ServerSettingsClient.state();
                    adopt(new SettingsStatePayload(old.revision(),s,admin,"Набор выбран; нажмите «Применить»",SettingsRequestPayload.NONE,area));
                    dirty=true;rebuildWidgets();},admin);}
                action("Назначить точку теста здесь",()->send(2),admin);
                action("TEST EXPLOSION — подтверждение…",()->send(3),admin&&!dirty);
                serverSlider("Дальность звука, блоки",15,64,2048,true);
                serverSlider("Базовый масштаб эффекта",14,1,4,false);
                slider("Личный масштаб эффекта",settings.explosionVfxScale,.5,3,false,v->settings.explosionVfxScale=v,true);
                slider("Громкость взрыва",settings.explosionVolume,0,1,false,v->settings.explosionVolume=v,true);
            }
            case 1 -> {
                slider("Чувствительность мыши",settings.sensitivity,.1,3,false,v->settings.sensitivity=v,true);
                toggle("Инверсия тангажа",()->settings.invertPitch,v->settings.invertPitch=v,true);
                serverToggle("Автовыравнивание крена",7);serverSlider("Сопротивление воздуха",8,.5,2,false);
                action("Клавиши управления…",()->minecraft.gui.setScreen(new KeyBindsScreen(this,minecraft.options)),true);
            }
            case 2 -> {
                action("HUD: "+(settings.minimalHud?"MINIMAL":"FULL"),()->{settings.minimalHud=!settings.minimalHud;rebuildWidgets();},true);
                action("Цвет: "+new String[]{"Зелёный","Белый","Красный"}[settings.hudColor],()->{settings.hudColor=(settings.hudColor+1)%3;rebuildWidgets();},true);
                slider("Поле зрения",settings.cameraFov,50,110,true,v->settings.cameraFov=v,true);
                toggle("Наклон камеры и переходы",()->settings.cameraEffects,v->settings.cameraEffects=v,true);
                slider("Сглаживание камеры, с",settings.smoothingSeconds,0,.5,false,v->settings.smoothingSeconds=v,true);
                toggle("Ночное видение FPV",()->settings.nightVision,v->settings.nightVision=v,true);
            }
            case 3 -> {
                slider("Двигатель",settings.engineVolume,0,1,false,v->settings.engineVolume=v,true);
                slider("Ветер",settings.windVolume,0,1,false,v->settings.windVolume=v,true);
                slider("Интерфейс SHAHED",settings.interfaceVolume,0,1,false,v->settings.interfaceVolume=v,true);
                slider("Взрыв",settings.explosionVolume,0,1,false,v->settings.explosionVolume=v,true);
                slider("Дальний гул",settings.distantRumbleVolume,0,1,false,v->settings.distantRumbleVolume=v,true);
                toggle("Эхо взрыва",()->settings.explosionEcho,v->settings.explosionEcho=v,true);
            }
            case 4 -> {
                toggle("Запрос повтора",()->settings.replayEnabled,v->settings.replayEnabled=v,true);
                action("Скорость: "+(settings.replaySlow?"0.25×":"1×"),()->{settings.replaySlow=!settings.replaySlow;rebuildWidgets();},true);
                action("Камера: "+new String[]{"FOLLOW","SIDE","CRASH SITE"}[settings.replayCamera],()->{settings.replayCamera=(settings.replayCamera+1)%3;rebuildWidgets();},true);
                toggle("Автоповтор аварии",()->settings.replayAuto,v->settings.replayAuto=v,true);
                toggle("Орбита режиссёра",()->settings.directorCamera,v->settings.directorCamera=v,true);
                toggle("Юмор производителя",()->settings.humor,v->settings.humor=v,true);
            }
            case 5 -> {
                serverSlider("Активных пилотов",9,1,64,true);serverToggle("Разрешить наблюдателей",10);
                serverSlider("Телеметрия: интервал, тики",11,1,20,true);
                action("Перечитать настройки сервера",ServerSettingsClient::read,true);
            }
            case 6 -> {
                slider("Длительность дыма, с",settings.smokeSeconds,5,60,true,v->settings.smokeSeconds=v,true);
                slider("Плотность дыма",settings.smokeDensity,0,3,false,v->settings.smokeDensity=v,true);
                slider("Обломки и угли",settings.debrisDensity,0,2,false,v->settings.debrisDensity=v,true);
                serverToggle("Настоящий огонь",12);serverToggle("Распространение огня",13);
            }
            case 7 -> {
                action("Качество: "+new String[]{"Низкое","Среднее","Высокое","Экстремальное"}[settings.explosionQuality],()->{
                    settings.explosionQuality=(settings.explosionQuality+1)%4;rebuildWidgets();},true);
                slider("Бюджет частиц",settings.particleBudget,256,2048,true,v->settings.particleBudget=(int)v,true);
                slider("Тряска камеры",settings.cameraShake,0,1,false,v->settings.cameraShake=v,true);
                slider("Личный масштаб эффекта",settings.explosionVfxScale,.5,3,false,v->settings.explosionVfxScale=v,true);
            }
        }
        int perPage=Math.max(1,(height-153)/24),pages=Math.max(1,(rows.size()+perPage-1)/perPage);
        page=Math.min(page,pages-1);
        for(int i=page*perPage;i<Math.min(rows.size(),(page+1)*perPage);i++) addRenderableWidget(rows.get(i).apply(84+(i-page*perPage)*24));
        int bottom=height-59;
        var back=addRenderableWidget(Button.builder(Component.literal("‹"),b->{page--;rebuildWidgets();}).bounds(width/2-120,bottom,35,20).build());back.active=page>0;
        var apply=addRenderableWidget(Button.builder(Component.literal(admin?"Применить серверные":"Только администратор"),b->send(1)).bounds(width/2-81,bottom,162,20).build());
        apply.active=admin&&dirty;
        var next=addRenderableWidget(Button.builder(Component.literal("›"),b->{page++;rebuildWidgets();}).bounds(width/2+85,bottom,35,20).build());next.active=page+1<pages;
        addRenderableWidget(Button.builder(Component.literal("Готово · "+(page+1)+"/"+pages),b->onClose()).bounds(width/2-120,height-35,240,20).build());
    }
    private void send(int action) {ServerSettingsClient.request(action,policy(),SettingsRequestPayload.NONE);}
    private void serverToggle(String name,int index) {toggle(name+" [сервер]",()->draft[index]>0,v->{draft[index]=v?1:0;dirty=true;},admin);}
    private void serverSlider(String name,int index,double min,double max,boolean integer) {
        slider(name+" [сервер]",draft[index],min,max,integer,v->{draft[index]=v;dirty=true;},admin);
    }
    private void toggle(String name,BooleanSupplier get,Consumer<Boolean> set,boolean enabled) {
        action(name+": "+(get.getAsBoolean()?"Вкл":"Выкл"),()->{set.accept(!get.getAsBoolean());rebuildWidgets();},enabled);
    }
    private void action(String name,Runnable action,boolean enabled) {rows.add(y->{var b=Button.builder(Component.literal(name),button->{action.run();settings.save();})
            .bounds(width/2-120,y,240,20).build();b.active=enabled;b.setTooltip(Tooltip.create(Component.literal(name)));return b;});}
    private void slider(String name,double current,double min,double max,boolean integer,DoubleConsumer setter,boolean enabled) {
        rows.add(y->{var s=new AbstractSliderButton(width/2-120,y,240,20,Component.empty(),(current-min)/(max-min)) {
            {updateMessage();setTooltip(Tooltip.create(Component.literal(name)));}
            private double selected() {double v=min+value*(max-min);return integer?Math.round(v):v;}
            @Override protected void updateMessage() {setMessage(Component.literal(name+": "+String.format(Locale.ROOT,integer?"%.0f":"%.2f",selected())));}
            @Override protected void applyValue() {setter.accept(selected());}
        };s.active=enabled;return s;});
    }
    @Override public void tick() {
        // Sliders do not rebuild while dragging. Activate apply/test widgets on the following tick.
        for(var w:children()) if(w instanceof Button b) {
            if(b.getMessage().getString().equals("Применить серверные")) b.active=admin&&dirty;
            if(b.getMessage().getString().startsWith("TEST EXPLOSION")) b.active=admin&&!dirty;
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float t) {
        super.extractRenderState(g,x,y,t);g.centeredText(font,title,width/2,10,0xFF9DFFC0);
        g.centeredText(font,Component.literal(font.plainSubstrByWidth(status,width-12)),width/2,height-72,0xFFB8DCC8);
    }
    @Override public boolean isPauseScreen() {return false;}
    @Override public void onClose() {settings.save();minecraft.gui.setScreen(parent);}
    @Override public void removed() {settings.save();}
}
