package org.Zebedee.DBZMotherlode;

import org.dreambot.api.methods.interactive.Players;

import java.util.List;
import org.json.simple.*;

public class Bot {

    String botName;
    int miningArea;

    public Bot(){
        this.botName = Players.localPlayer().getName();
        this.miningArea = JSON.getMiningArea();
    }
}
