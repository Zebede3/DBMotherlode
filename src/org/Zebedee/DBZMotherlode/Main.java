package org.Zebedee.DBZMotherlode;

import org.dreambot.api.Client;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.SkillTracker;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.walking.pathfinding.impl.obstacle.impl.DestructableObstacle;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.script.event.impl.ExperienceEvent;
import org.dreambot.api.script.impl.TaskScript;
import org.dreambot.api.script.listener.ExperienceListener;
import org.dreambot.api.utilities.Timer;

import java.awt.*;

@ScriptManifest(name = "Zeb's Motherlode", author = "Zebedee", description = "Motherlode Mine", version = 1.0, category = Category.MINING)
public class Main extends TaskScript implements ExperienceListener {

    public static Bot bot;
    public static int totalGoldenNuggets;

    static int expGained;
    static Timer t;

    private AntiBan antiBan;

    public static int targetMiningLevel = 0;

    @Override
    public void onStart(){

        log("Starting Zeb's Motherlode Miner");

        log("Starting up Zen Anti-ban");
        antiBan = new AntiBan(this);

        log("Setting client to use WindMouse algorithm");
        Client.getInstance().setMouseMovementAlgorithm(new WindMouse());
        if(Client.getInstance().getMouseMovementAlgorithm() instanceof WindMouse ){
            log("Success!");
        }else{
            log("Could not load WindMouse");
        }

        if(!JSON.fileExists()){
            JSON.writeJson();
        }

        // Add rockfall in MLM to the path finder
        log("Adding obstacles to web walker");
        Walking.getAStarPathFinder().addObstacle(new DestructableObstacle("Rockfall", "Mine", null,null, null));

        log("Starting timer");
        t = new Timer();
        log("Resetting XP count for this session");
        expGained = 0;
        log("Starting skill tracker");
        SkillTracker.start();

        log("Initializing new BOT instance");
        bot = new Bot();

        log("Adding Anti-ban skills to check");
        antiBan.setStatsToCheck(Skill.MINING);

        log("Adding task nodes");
        addNodes(
                new MotherlodeMine(this.antiBan)
        );

    }

    @Override
    public void onExit(){
        log("Stopping Zeb's Motherlode");
    }

    @Override
    public void onPaint(Graphics g){
        if(t != null){
            g.drawString("Time elapsed: " + t.formatTime(), 30,30);
        }
        if(getLastTaskNode() != null){
            g.drawString("Current task: " + getLastTaskNode().toString(), 30,45);
        }
        g.drawString("Total XP: " + Integer.toString(expGained), 30, 60);
        g.drawString("XP/Hour: " + SkillTracker.getGainedExperiencePerHour(Skill.MINING), 30, 75);
        g.drawString("Mining Level: " + Skills.getRealLevel(Skill.MINING), 30,90);
        g.drawString("Anti-ban Status: " + (antiBan.getStatus().equals("") ? "Inactive" : antiBan.getStatus()), 30,105);
    }

    @Override
    public void onGained(ExperienceEvent e){
        if(e.getSkill().equals(Skill.MINING)){
            expGained += e.getChange();
        }
    }

    public static String bestPickaxe(){
        int miningLevel = Skills.getRealLevel(Skill.MINING);

        if(miningLevel >= 21 && miningLevel < 31){
            return "Mithril pickaxe";
        }

        if(miningLevel >= 31 && miningLevel < 41){
            return "Adamant pickaxe";
        }

        if(miningLevel >= 41){
            return "Rune pickaxe";
        }

        return null;
    }
}
