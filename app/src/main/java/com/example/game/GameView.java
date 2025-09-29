package com.example.game;

import android.content.Context;
import android.graphics.*;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.os.Handler;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameView extends SurfaceView implements Runnable {
    private Thread gameThread;
    private boolean isPlaying;
    private SurfaceHolder holder;
    private Paint paint;

    private Bitmap background;
    private Sprite character;

    private float charX, charY;
    private float speedX = 0, speedY = 0;

    private int screenWidth = 0, screenHeight = 0;
    private boolean initialized = false;

    private static final long STEP_NS = 16_666_667L;
    private long prevNs, accNs;

    private float bgX = 0;
    private float baseBgSpeed = 2;

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    // Entities
    private List<Projectile> projectiles = new ArrayList<>();
    private List<Explosion> explosions = new ArrayList<>();
    private List<LevelUpEffect> levelEffects = new ArrayList<>();
    private List<Missile> missiles = new ArrayList<>();
    private List<Laser> lasers = new ArrayList<>();
    private List<Item> items = new ArrayList<>();

    // Player system
    private GamePlayer player;
    private Paint levelTextPaint, xpBarBgPaint, xpBarFillPaint;

    // Player HP
    private int playerHp = 100;
    private int maxPlayerHp = 100;
    private boolean isGameOver = false;
    private RectF buttonRestart;

    // Sound
    private SoundPool soundPool;
    private int shootSoundId;
    private int explosionSoundId;

    // BGM
    private ExoPlayer exoPlayer;

    // Icons
    private Bitmap iconVolumeOn, iconVolumeOff;
    private Bitmap iconSfxOn, iconSfxOff;
    private RectF iconVolumeRect, iconSfxRect;
    private boolean isAllSoundOn = true;
    private boolean isSfxOn = true;

    // Joystick
    private Joystick joystick;
    private int joystickPointerId = -1;

    // Auto Fire
    private long lastShootTime = 0;
    private long shootCooldown = 300; // ms

    // Star power-up
    private Star star;

    // Defense
    private Shield shield = new Shield();
    private ArmorBuff armorBuff = new ArmorBuff();
    private Barrier barrier = new Barrier();

    // WaveManager
    private WaveManager waveManager;

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);

        background = BitmapFactory.decodeResource(getResources(), R.drawable.bg);
        character = new Sprite(context, R.drawable.character, 1, 8);

        player = new GamePlayer();
        levelTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        levelTextPaint.setColor(Color.WHITE);
        levelTextPaint.setTextSize(dp(20));
        levelTextPaint.setTextAlign(Paint.Align.LEFT);

        xpBarBgPaint = new Paint();
        xpBarBgPaint.setColor(Color.GRAY);
        xpBarFillPaint = new Paint();
        xpBarFillPaint.setColor(Color.rgb(0, 230, 118));

        soundPool = new SoundPool(5, android.media.AudioManager.STREAM_MUSIC, 0);
        shootSoundId = soundPool.load(context, R.raw.shoot, 1);
        explosionSoundId = soundPool.load(context, R.raw.explosion, 1);

        exoPlayer = new ExoPlayer.Builder(context).build();
        MediaItem music = MediaItem.fromUri("android.resource://" + context.getPackageName() + "/" + R.raw.bg_music);
        exoPlayer.setMediaItem(music);
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
        exoPlayer.prepare();
        exoPlayer.setVolume(1.0f);

        iconVolumeOn = BitmapFactory.decodeResource(getResources(), R.drawable.ic_volume_on);
        iconVolumeOff = BitmapFactory.decodeResource(getResources(), R.drawable.ic_volume_off);
        iconSfxOn = BitmapFactory.decodeResource(getResources(), R.drawable.ic_sfx_on);
        iconSfxOff = BitmapFactory.decodeResource(getResources(), R.drawable.ic_sfx_off);
        waveManager = new WaveManager(context, getWidth()>0?getWidth():1080, getHeight()>0?getHeight():1920);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w; screenHeight = h;

        if (background != null) {
            background = Bitmap.createScaledBitmap(background, screenWidth, screenHeight, true);
        }

        if (!initialized && character != null) {
            charX = (screenWidth - character.getFrameWidth()) / 2f;
            charY = screenHeight - character.getFrameHeight() - 30f;
            initialized = true;
            character.setRow(0);
            character.resetAnimation();
        }

        joystick = new Joystick(dp(100), screenHeight - dp(150), dp(80), dp(40));
        buttonRestart = new RectF(screenWidth/2f - dp(100), screenHeight/2f + dp(50),
                screenWidth/2f + dp(100), screenHeight/2f + dp(120));

        star = new Star(getContext(), screenWidth, screenHeight);

        iconVolumeRect = new RectF(20, 20, 20 + dp(50), 20 + dp(50));
        iconSfxRect = new RectF(screenWidth - dp(70), 20, screenWidth - 20, 20 + dp(50));

        // init wave system
        waveManager = new WaveManager(getContext(), screenWidth, screenHeight);
    }

    @Override
    public void run() {
        prevNs = System.nanoTime();
        while (isPlaying) {
            long now = System.nanoTime();
            accNs += (now - prevNs);
            prevNs = now;
            while (accNs >= STEP_NS) {
                update();
                accNs -= STEP_NS;
            }
            draw();
            try { Thread.sleep(1); } catch (InterruptedException ignored) {}
        }
    }

    private void update() {
        if (!initialized || isGameOver) return;

        bgX -= baseBgSpeed;
        if (bgX <= -screenWidth) bgX += screenWidth;

        // move player
        speedX = joystick.getActuatorX() * 5;
        speedY = joystick.getActuatorY() * 5;
        charX += speedX;
        charY += speedY;
        character.update();
        charX = Math.max(0, Math.min(charX, screenWidth - character.getFrameWidth()));
        charY = Math.max(0, Math.min(charY, screenHeight - character.getFrameHeight()));

        // Auto fire bullets
        long now = System.currentTimeMillis();
        if (now - lastShootTime > shootCooldown) {
            fireProjectile();
            lastShootTime = now;
        }

        // Projectiles update
        for (int i = projectiles.size()-1; i>=0; i--) {
            Projectile p = projectiles.get(i);
            p.update(); if (!p.isActive()) projectiles.remove(i);
        }

        // Wave update
        waveManager.update();

        // Explosions
        for (int i=explosions.size()-1; i>=0; i--) {
            explosions.get(i).update();
            if (explosions.get(i).isFinished()) explosions.remove(i);
        }

        // Level effects
        for (int i=levelEffects.size()-1; i>=0; i--) {
            levelEffects.get(i).update();
            if (levelEffects.get(i).isFinished()) levelEffects.remove(i);
        }

        // Items update + player eat item
        for (int i=items.size()-1; i>=0; i--){
            Item it=items.get(i);
            it.update();
            if(!it.isActive()) { items.remove(i); continue; }
            Rect cRect=new Rect((int)charX,(int)charY,
                    (int)charX+character.getFrameWidth(),
                    (int)charY+character.getFrameHeight());
            if(Rect.intersects(cRect, it.getRect())){
                activateItem(it.getType());
                items.remove(i);
            }
        }

        // Projectiles vs Enemies
        for (int i=projectiles.size()-1; i>=0; i--) {
            Projectile p = projectiles.get(i);
            boolean hit=false;
            for (Enemy e: waveManager.getActiveEnemies()) {
                if (Rect.intersects(p.getRect(), e.getRect()) && !e.isDead()) {
                    e.takeDamage(p.getDamage());
                    hit=true;
                    if (e.isDead()) {
                        explosions.add(new Explosion(getContext(), R.drawable.explosion_sheet, e.getX(), e.getY(), 9));
                        if (isAllSoundOn&&isSfxOn) soundPool.play(explosionSoundId,1,1,0,0,1);
                        if (player.addXP(e.getXPValue())) {
                            levelEffects.add(new LevelUpEffect(charX+character.getFrameWidth()/2, charY));
                        }
                        if(Math.random()<0.25){
                            items.add(new Item(new Random().nextInt(5), e.getX(), e.getY()));
                        }
                    }
                    break;
                }
            }
            if(hit) projectiles.remove(i);
        }

        // Missile
        for (int i=missiles.size()-1; i>=0; i--) {
            Missile m = missiles.get(i);
            Enemy nearest=null; float best=Float.MAX_VALUE;
            for (Enemy e: waveManager.getActiveEnemies()){
                if(e.isDead()) continue;
                float dx=e.getX()-m.getRect().centerX();
                float dy=e.getY()-m.getRect().centerY();
                float d=(float)Math.sqrt(dx*dx+dy*dy);
                if(d<best){nearest=e;best=d;}
            }
            m.update(nearest);
            if(nearest!=null && Rect.intersects(m.getRect(), nearest.getRect()) && !nearest.isDead()) {
                nearest.takeDamage(m.getDamage());
                explosions.add(new Explosion(getContext(), R.drawable.explosion_sheet, nearest.getX(), nearest.getY(), 9));
                if(isAllSoundOn&&isSfxOn) soundPool.play(explosionSoundId,1,1,0,0,1);
                m.deactivate();
                if(nearest.isDead()){
                    player.addXP(nearest.getXPValue());
                    if(Math.random()<0.25){
                        items.add(new Item(new Random().nextInt(5), nearest.getX(), nearest.getY()));
                    }
                }
            }
            if(!m.isActive()) missiles.remove(i);
        }

        // Laser
        for (int i=lasers.size()-1; i>=0; i--) {
            Laser l=lasers.get(i); l.update();
            if(!l.isActive()){ lasers.remove(i); continue; }
            for(Enemy e:waveManager.getActiveEnemies()) {
                if(!e.isDead() && l.hit(e)) {
                    e.takeDamage(l.getDamage());
                    explosions.add(new Explosion(getContext(), R.drawable.explosion_sheet, e.getX(), e.getY(), 9));
                    if(e.isDead()){
                        player.addXP(e.getXPValue());
                        if(Math.random()<0.25){
                            items.add(new Item(new Random().nextInt(5), e.getX(), e.getY()));
                        }
                    }
                }
            }
        }

        // Shield follow player
        if (shield.isActive()) {
            shield.update(charX+character.getFrameWidth()/2, charY+character.getFrameHeight()/2);
        }

        // Armor buff
        armorBuff.update();

        // Barrier collision
        if (barrier.isActive()) {
            Rect bRect = barrier.getRect();
            for (Enemy e: waveManager.getActiveEnemies()) {
                if(Rect.intersects(bRect, e.getRect()) && !e.isDead()){
                    e.takeDamage(999);
                    barrier.deactivate();
                    break;
                }
            }
        }

        // player vs enemy
        Rect cRect=new Rect((int)charX,(int)charY,(int)charX+character.getFrameWidth(),(int)charY+character.getFrameHeight());
        for (Enemy e: waveManager.getActiveEnemies()){
            if(!e.isDead() && Rect.intersects(cRect,e.getRect())){
                if(shield.isActive()){
                    shield.hit();
                } else {
                    int dmg=20;
                    dmg = armorBuff.reduceDamage(dmg);
                    playerHp-=dmg;
                }
                if(playerHp<=0){playerHp=0;isGameOver=true;isPlaying=false;}
                e.takeDamage(999);
            }

            // player vs enemy bullets
            for(EnemyBullet b: e.getBullets()){
                if(b.isActive() && Rect.intersects(cRect,b.getRect())){
                    b.active=false;
                    if(shield.isActive()) shield.hit();
                    else {
                        int dmg=10;
                        dmg = armorBuff.reduceDamage(dmg);
                        playerHp-=dmg;
                    }
                    if(playerHp<=0){playerHp=0;isGameOver=true;isPlaying=false;}
                }
            }
        }
    }

    private void draw() {
        if(!initialized)return;
        Canvas canvas=null;
        try{
            if(!holder.getSurface().isValid())return;
            canvas=holder.lockCanvas(); if(canvas==null)return;

            if(background!=null){
                canvas.drawBitmap(background,bgX,0,paint);
                canvas.drawBitmap(background,bgX+screenWidth,0,paint);
            }

            character.draw(canvas,(int)charX,(int)charY,paint);
            for(Projectile p:projectiles)p.draw(canvas,paint);
            for(Missile m:missiles)m.draw(canvas,paint);
            for(Laser l:lasers)l.draw(canvas,paint);
            for(Enemy e:waveManager.getActiveEnemies()) e.draw(canvas,paint);
            for(Explosion ex:explosions)ex.draw(canvas,paint);
            for(LevelUpEffect eff:levelEffects)eff.draw(canvas,paint);
            for(Item it:items) it.draw(canvas,paint);

            if (armorBuff.isActive()) {
                paint.setColor(Color.argb(120, 200, 0, 200));
                canvas.drawCircle(charX + character.getFrameWidth()/2f,
                        charY + character.getFrameHeight()/2f,
                        character.getFrameWidth(), paint);
            }
            if(star!=null)star.draw(canvas,paint);

            joystick.draw(canvas,paint);

            shield.draw(canvas,paint);
            barrier.draw(canvas,paint);

            drawSoundIcons(canvas);
            drawPlayerUI(canvas);

            if(isGameOver){
                paint.setColor(Color.argb(180,0,0,0));
                canvas.drawRect(0,0,screenWidth,screenHeight,paint);
                paint.setColor(Color.WHITE);
                paint.setTextSize(dp(40));
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("GAME OVER",screenWidth/2f,screenHeight/2f,paint);
                paint.setTextSize(dp(25));
                canvas.drawRoundRect(buttonRestart,20,20,paint);
                canvas.drawText("RESTART",buttonRestart.centerX(),buttonRestart.centerY()+10,paint);
            }
        } finally {
            if(canvas!=null) holder.unlockCanvasAndPost(canvas);
        }
    }
    // ==== Fire ====
    private void fireProjectile(){
        float projX=charX+character.getFrameWidth()/2f-6;
        float projY=charY-20;
        int dmg=player.getCurrentDamage();
        int count=player.getBulletCount();
        if(count==1) projectiles.add(new Projectile(projX,projY,dmg));
        else if(count==2){
            projectiles.add(new Projectile(projX-15,projY,dmg));
            projectiles.add(new Projectile(projX+15,projY,dmg));
        }
        else {
            projectiles.add(new Projectile(projX-30,projY,dmg));
            projectiles.add(new Projectile(projX,projY,dmg));
            projectiles.add(new Projectile(projX+30,projY,dmg));
        }
        if(isAllSoundOn&&isSfxOn) soundPool.play(shootSoundId,1,1,0,0,1);
    }

    private void fireMissile(){
        float mx=charX+character.getFrameWidth()/2f;
        float my=charY;
        missiles.add(new Missile(mx,my));
    }

    private void fireLaser(){
        float lx=charX+character.getFrameWidth()/2f;
        float ly=charY;
        lasers.add(new Laser(lx,ly,screenHeight));
    }

    // ==== Activated when eating item ====
    private void activateItem(int type){
        switch(type){
            case Item.TYPE_MISSILE: fireMissile(); break;
            case Item.TYPE_LASER: fireLaser(); break;
            case Item.TYPE_SHIELD:
                shield.activate(charX+character.getFrameWidth()/2,
                        charY+character.getFrameHeight()/2,
                        character.getFrameWidth());
                break;
            case Item.TYPE_ARMOR:
                armorBuff.activate();
                break;
            case Item.TYPE_BARRIER:
                barrier.activate(charX+character.getFrameWidth()/2, charY);
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if(!initialized) return true;
        int action=event.getActionMasked();
        int pointerIndex=event.getActionIndex();
        float x=event.getX(pointerIndex);
        float y=event.getY(pointerIndex);

        if(isGameOver && action==MotionEvent.ACTION_DOWN && buttonRestart.contains(x,y)){
            resetGame();
            return true;
        }

        switch(action){
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:{
                if(iconVolumeRect.contains(x,y)){
                    isAllSoundOn=!isAllSoundOn;
                    if(isAllSoundOn){
                        if(exoPlayer!=null&&!exoPlayer.isPlaying()) exoPlayer.play();
                    } else {
                        if(exoPlayer!=null&&exoPlayer.isPlaying()) exoPlayer.pause();
                    }
                    return true;
                }
                if(iconSfxRect.contains(x,y)){
                    isSfxOn=!isSfxOn;
                    return true;
                }
                if(joystick.isPressed(x,y)){
                    joystick.setPressed(true);
                    joystickPointerId=event.getPointerId(pointerIndex);
                    joystick.setActuator(x,y);
                }
                break;
            }
            case MotionEvent.ACTION_MOVE:{
                if(joystickPointerId!=-1){
                    int idx=event.findPointerIndex(joystickPointerId);
                    if(idx!=-1){
                        joystick.setActuator(event.getX(idx), event.getY(idx));
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:{
                if(event.getPointerId(pointerIndex)==joystickPointerId){
                    joystick.setPressed(false);
                    joystick.resetActuator();
                    joystickPointerId=-1;
                }
                break;
            }
        }
        return true;
    }

    private void drawSoundIcons(Canvas canvas) {
        Bitmap musicIcon = isAllSoundOn ? iconVolumeOn : iconVolumeOff;
        Rect musicDst = new Rect((int) iconVolumeRect.left,(int) iconVolumeRect.top,
                (int) iconVolumeRect.right,(int) iconVolumeRect.bottom);
        canvas.drawBitmap(musicIcon,null,musicDst,paint);

        Bitmap sfxIcon = isSfxOn ? iconSfxOn : iconSfxOff;
        Rect sfxDst = new Rect((int) iconSfxRect.left,(int) iconSfxRect.top,
                (int) iconSfxRect.right,(int) iconSfxRect.bottom);
        canvas.drawBitmap(sfxIcon,null,sfxDst,paint);
    }

    private void drawPlayerUI(Canvas canvas) {
        canvas.drawText("Wave: "+waveManager.getCurrentWave(), dp(20), dp(50), levelTextPaint);
        canvas.drawText("LV: "+player.getLevel(), dp(20), dp(80), levelTextPaint);

        if(player.isMaxLevel()){
            canvas.drawText("MAX LEVEL", dp(80), dp(80), levelTextPaint);
        } else {
            float xpBarWidth=dp(150), xpBarHeight=dp(10), xpBarX=dp(80), xpBarY=dp(75);
            canvas.drawRect(xpBarX,xpBarY,xpBarX+xpBarWidth, xpBarY+xpBarHeight,xpBarBgPaint);
            float progress=player.getXPProgress();
            canvas.drawRect(xpBarX,xpBarY,xpBarX+xpBarWidth*progress,xpBarY+xpBarHeight,xpBarFillPaint);
        }

        canvas.drawText("HP: "+playerHp, dp(20), dp(120), levelTextPaint);
        float hpBarWidth=dp(150), hpBarHeight=dp(12), hpBarX=dp(80), hpBarY=dp(110);
        paint.setColor(Color.RED);
        canvas.drawRect(hpBarX,hpBarY,hpBarX+hpBarWidth,hpBarY+hpBarHeight,paint);
        paint.setColor(Color.GREEN);
        float hpProg=(float)playerHp/maxPlayerHp;
        canvas.drawRect(hpBarX,hpBarY,hpBarX+hpBarWidth*hpProg,hpBarY+hpBarHeight,paint);
    }

    private void resetGame(){
        playerHp=maxPlayerHp;
        isGameOver=false;
        player=new GamePlayer();
        projectiles.clear();
        missiles.clear();
        lasers.clear();
        explosions.clear();
        levelEffects.clear();
        items.clear();
        star.respawn();
        waveManager = new WaveManager(getContext(), screenWidth, screenHeight);
        isPlaying=true;
        gameThread=new Thread(this);
        gameThread.start();
    }

    public void resume(){
        if(isGameOver) return;
        isPlaying=true;
        gameThread=new Thread(this);
        gameThread.start();
        if(exoPlayer!=null&&!exoPlayer.isPlaying()) exoPlayer.play();
    }

    public void pause(){
        isPlaying=false;
        try{ if(gameThread!=null) gameThread.join(); }
        catch(InterruptedException ignored){}
        if(exoPlayer!=null&&exoPlayer.isPlaying()) exoPlayer.pause();
    }

    public void release(){
        if(background!=null&&!background.isRecycled()){
            background.recycle();
            background=null;
        }
        if(character!=null){
            character.dispose();
            character=null;
        }
        if(soundPool!=null){
            soundPool.release();
            soundPool=null;
        }
        if(exoPlayer!=null){
            exoPlayer.release();
            exoPlayer=null;
        }
    }

    // =========== Joystick =============
    private class Joystick{
        private float centerX,centerY,baseRadius,hatRadius,
                actuatorX=0,actuatorY=0;
        private boolean isPressed=false;

        public Joystick(float cx,float cy,float br,float hr){
            centerX=cx; centerY=cy; baseRadius=br; hatRadius=hr;
        }

        public void draw(Canvas canvas,Paint paint){
            paint.setColor(Color.argb(100,200,200,200));
            canvas.drawCircle(centerX,centerY,baseRadius,paint);
            float innerX=centerX+actuatorX*baseRadius;
            float innerY=centerY+actuatorY*baseRadius;
            paint.setColor(Color.argb(200,100,200,255));
            canvas.drawCircle(innerX, innerY, hatRadius, paint);
        }

        public boolean isPressed(float tx,float ty){
            double d=Math.sqrt((tx-centerX)*(tx-centerX)+(ty-centerY)*(ty-centerY));
            return d<baseRadius;
        }

        public void setPressed(boolean p){isPressed=p;}

        public void setActuator(float tx,float ty){
            float dx=tx-centerX, dy=ty-centerY;
            float d=(float)Math.sqrt(dx*dx+dy*dy);
            if(d>baseRadius){
                dx=(dx/d)*baseRadius;
                dy=(dy/d)*baseRadius;
            }
            actuatorX=dx/baseRadius;
            actuatorY=dy/baseRadius;
        }

        public void resetActuator(){ actuatorX=0; actuatorY=0; }
        public float getActuatorX(){return actuatorX;}
        public float getActuatorY(){return actuatorY;}
    }
}