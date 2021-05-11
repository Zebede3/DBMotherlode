package org.Zebedee.DBZMotherlode;

import org.dreambot.api.Client;
import org.dreambot.api.data.GameState;
import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.RSLoginResponse;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.Shop;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.bank.BankLocation;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.filter.Filter;
import org.dreambot.api.methods.grandexchange.GrandExchange;
import org.dreambot.api.methods.input.Camera;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.login.LoginUtility;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Map;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.settings.PlayerSettings;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.world.World;
import org.dreambot.api.methods.world.Worlds;
import org.dreambot.api.methods.worldhopper.WorldHopper;
import org.dreambot.api.script.TaskNode;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.items.Item;

import java.util.List;

public class MotherlodeMine extends TaskNode {

    final Area dwarfMine = new Area(3054, 9780, 3064, 9763, 0);
    final Area falador = new Area(2937, 3390, 3066, 3329, 0);
    final Area house = new Area(3058, 3380, 3062, 3375, 0);
    final Area motherlodeMine = new Area(3714, 5693, 3774, 5634);
    final Area bankArea = new Area(3757, 5667, 3760, 5664);
    final static Area depositArea = new Area(3747, 5674, 3750, 5671);
    final Area collectionArea = new Area(3747, 5660, 3750, 5658);
    final Area strutsArea = new Area(3740, 5670, 3742, 5661);
    final Area lowerArea = new Area(3741, 5673, 3751, 5659);

    /*
    Motherlode mine pay-dirt mining areas
     */
    final Area southWestMiningArea = new Area(3713, 5645, 3726, 5633);

    final Area westMiningArea = new Area(
            new Tile[]{
                    new Tile(3715, 5671, 0),
                    new Tile(3715, 5663, 0),
                    new Tile(3722, 5662, 0),
                    new Tile(3722, 5665, 0),
                    new Tile(3721, 5666, 0),
                    new Tile(3722, 5667, 0),
                    new Tile(3722, 5669, 0),
                    new Tile(3723, 5670, 0),
                    new Tile(3723, 5673, 0),
                    new Tile(3718, 5673, 0)
            }
    );

    Area southEastMiningArea = new Area(3763, 5647, 3774, 5634);


    Area areaToMine = null;

    Player local;


    // Predicates
    private final Filter<GameObject> ORE_VEIN = v -> v.getName().equals("Ore vein") && areaToMine.contains(v.getTile()) && Map.canReach(v.getTile(), true) && v.distance() < 10;
    private final Filter<Item> DO_NOT_BANK_ITEMS = h -> h.getName().equals("Hammer") || h.getName().equals(Main.bestPickaxe()) || h.getName().contains("Prospector");
    final Filter<Item> ROW = i -> i != null && i.getName().contains("Ring of wealth (");


    // 382,4,2
    static int payDirtRewardNumber;

    int miningArea = Main.bot.miningArea;


    // Scene objects & Npcs
    GameObject bankChest;
    GameObject oreVein;
    GameObject hopper;
    GameObject sack;

    /*
    Struts
     */
    List<GameObject> struts;
    GameObject brokenStrut;


    // Tiles
    final Tile crateTile = new Tile(3752, 5664);
    final Tile entrance = new Tile(3728, 5689);

    int mineAreaIndex;

    World randWorld;

    Camera camera;

    boolean shouldStop = false;

    int r;

    final Filter<Player> HOP = p -> p != null && areaToMine.contains(p) && !p.getName().equals(Players.localPlayer().getName());
    final Filter<Item> REWARDS = i -> i != null &&
            i.getName().equals("Coal") || i.getName().equals("Golden nugget") || i.getName().equals("Mithril ore") || i.getName().equals("Gold ore");
    final Filter<Item> PROSPECTOR = i -> i != null && i.getName().contains("Prospector");
    private AntiBan antiBan;

    GameObject target = null;

    public MotherlodeMine(AntiBan antiBan){
        this.antiBan = antiBan;
    }


    @Override
    public String toString() {
        return "Motherlode Mine";
    }

    @Override
    public boolean accept() {
        return Skills.getRealLevel(Skill.MINING) >= 30;
    }

    @Override
    public int execute() {


        checkBan();

        if (shouldStop || Skills.getRealLevel(Skill.MINING) >= 73) {
            return -1;
        }

        if(Camera.getZoom() > 323){
            Camera.setZoom(Calculations.random(180,323));
            sleep(500,1500);
        }

        // Get the local player
        local = Players.localPlayer();

        // Get the bots mining area
        if (areaToMine == null) {
            log("Setting Mining area");
            areaToMine = getMiningArea();
        }

        // Anti-ban stuff
       // antiban();
        int r = Calculations.random(0,10000);
        log(r);
        if(r > 9500){
            antiBan.doRandom();
        }
        if(antiBan.doRandom()){
            log("Script-specific anti-ban flag triggered");
        }

        // Get the current amount of pay-dirt reward
        payDirtRewardNumber = PlayerSettings.getBitValue(5558);

        if (!Inventory.contains(Main.bestPickaxe()) && !Equipment.contains(Main.bestPickaxe())) {
            getBestPickaxe();

        } else {
            if (!motherlodeMine.contains(local)) {
                walkToMotherlodeMine();
            } else {

                if(shouldBuyHelmet() || shouldBuyJacket() || shouldBuyLegs() || shouldBuyBoots()){

                    buyProspectorGear();

                }else{

                    if(Inventory.contains(PROSPECTOR)){
                        equipProspectorGear();
                    }


                    // Get hammer for struts
                    if (!Inventory.contains("Hammer")) {
                        getHammer();
                    }

                    // Fix a strut
                    if (!Inventory.contains("Pay-dirt") && GameObjects.all("Broken strut").size() == 2 && lowerArea.contains(local) && Inventory.contains("Hammer")) {
                        fixStruts();
                    }

                    // Deposit pay-dirt
                    if (Inventory.contains("Pay-dirt") && Inventory.isFull()) {
                        deposit();
                    }

                    // Collect rewards
                    if (!Inventory.isFull() && payDirtRewardNumber > 0 && GameObjects.all("Broken strut").size() < 2) {
                        collect();
                    }

                    // Mine pay-dirt
                    if (!Inventory.isFull() && PlayerSettings.getBitValue(5558) == 0 && !Inventory.contains("Golden nugget") && Inventory.contains("Hammer")) {
                        mine();
                    }

                    // Bank the rewards
                    if (hasRewards() && !Inventory.contains("Pay-dirt")) {
                        bank();
                    }
                }


            }


        }

        return Calculations.random(300, 600);
    }

    private void checkBan(){
        if(!Client.getGameState().equals(GameState.LOGGED_IN)){
            RSLoginResponse response = LoginUtility.login();
            if(response.equals(RSLoginResponse.ADDRESS_BLOCKED) || response.equals(RSLoginResponse.NOT_ELIGIBLE) || response.equals(RSLoginResponse.ACCOUNT_LOCKED)){
                log("Account has been banned.");
                shouldStop = true;
            }
        }
    }

    private void buyProspectorGear(){
        if(!Shop.isOpen()){
            log("Opening shop");
            if(!bankArea.contains(local)){
                Walking.walk(bankArea.getNearestTile(local));
                sleep(500,1500);
            }else{
                NPCs.closest("Prospector Percy").interact("Trade");
                sleepUntil(Shop::isOpen, 5000);
            }
        }else{
            log("Shop is open");
            sleep(500,1000);
            if(shouldBuyHelmet()){
                Shop.purchase("Prospector helmet", 1);
            }
            if(shouldBuyJacket()){
                Shop.purchase("Prospector jacket", 1);
            }
            if(shouldBuyLegs()){
                Shop.purchase("Prospector legs", 1);
            }
            if(shouldBuyBoots()){
                Shop.purchase("Prospector boots", 1);
            }
            sleep(500,1500);
            Shop.close();
        }
    }

    private void equipProspectorGear(){
        if(!Tabs.isOpen(Tab.INVENTORY)){
            Tabs.open(Tab.INVENTORY);
            sleep(500,1500);
        }else{
            sleep(100,400);
            Inventory.get(PROSPECTOR).interact();
            sleep(500,1500);
        }
    }

    private void antiban() {
        // Camrea stuff
        if (Camera.getZoom() > 272) {
            log("Adjusting camera");
            Camera.setZoom(Calculations.random(181, 272));
        }

        r = Calculations.random(1000);
        if (r > 950) {
            log("Adjusting camera: " + r);
            Camera.mouseRotateTo(Calculations.random(0, 2029), Calculations.random(224, 383));
        }

        if (!local.isOnScreen()) {
            log("Turning camera to player");
            Camera.mouseRotateToEntity(local);
        }

        // Take occasional AFK breaks
        if (Calculations.random(0, 1000) > 990) {
            log("Breaking...");
            sleep(3000, 25000);
        }

    }

    private void getBestPickaxe() {
        log("Getting a better pickaxe");
        if (Bank.openClosest()) {
            sleep(500, 1500);
            Bank.depositAllExcept(DO_NOT_BANK_ITEMS);
            sleep(500, 1500);
            if (Bank.contains(Main.bestPickaxe())) {
                log("Found new pickaxe");
                Bank.withdraw(Main.bestPickaxe());
                sleep(500, 1500);
                Bank.close();
            } else {
                log("Could not find better pickaxe, stopping script");
                Bank.close();
                sleep(100, 300);
                shouldStop = true;
            }
        }
    }

    private void mine() {

        if (!areaToMine.contains(local)) {
            log("Walking to mining spot");
            Walking.walk(areaToMine.getCenter().getRandomizedTile(4));
            sleep(200, 2000);
        } else {
            if (Players.closest(HOP) != null) {
                log("Hopping worlds");
                hopWorlds();
            } else {
                if (!Tabs.isOpen(Tab.INVENTORY)) {
                    Tabs.open(Tab.INVENTORY);
                }
                if (sleepUntil(() -> local.isAnimating(), Calculations.random(2000, 3000))) {
                    log("Player is mining");
                    sleep(1000, 2000);
                } else {
                    log("Looking for ore");
                    GameObject ore = GameObjects.closest(ORE_VEIN);
                    if (ore == null && (areaToMine.equals(westMiningArea) || areaToMine.equals(southEastMiningArea))) {
                        GameObjects.closest("Rockfall").interact();
                    }
                    if (ore != null) {
                        log("Found ore");
                        if (!ore.isOnScreen()) {
                            Camera.mouseRotateToEntity(ore);
                            sleep(200, 400);
                        } else {
                            if (ore.interact()) {
                                sleep(500, 1000);
                                sleepUntil(() -> !local.isMoving(), 4000);
                            }

                        }
                    }
                }
            }
        }
    }

    private void deposit() {
        hopper = GameObjects.closest("Hopper");
        log(hopper.distance());
        if (hopper == null || !Map.canReach(hopper.getTile(), true) || hopper.distance(local) > 8 || !hopper.isOnScreen()) {
            log("Walking to Hopper");
            Walking.walk(depositArea.getNearestTile(local).getRandomizedTile(2));
            sleep(Calculations.random(400, 2000));
        } else {
            if (hopper.interact()) {
                log("Depositing Pay-dirt");
                sleepUntil(() -> !Inventory.contains("Pay-dirt"), 12000);
                sleep(500, 1900);
                if (GameObjects.all("Broken strut").size() < 2) {
                    log("Walking to collection point");
                    Walking.walk(collectionArea.getRandomTile());
                    sleepUntil(() -> GameObjects.all("Broken strut").size() == 2 || payDirtRewardNumber > 0, 10000);
                }
            }
        }
    }

    private void collect() {
        sack = GameObjects.closest("Sack");

        if (sack == null || !Map.canReach(sack.getTile(), true) || sack.distance() > 10 || !sack.isOnScreen()) {
            log("Walking to Collection Sack");
            Walking.walk(collectionArea.getNearestTile(local));
            sleep(200, 2300);
        } else {
            if (sack.interact()) {
                log("Collecting rewards");
                sleepUntil(() -> hasRewards(), 8000);
                sleep(200, 900);
            }
        }
    }

    private void fixStruts() {
        if (!Inventory.contains("Hammer")) {
            getHammer();
        } else {
            log("Fixing struts");
            brokenStrut = GameObjects.closest("Broken strut");

            if (brokenStrut == null || !Map.canReach(brokenStrut.getTile(), true)) {
                log("Walking to Water wheel area");
                Walking.walk(strutsArea.getNearestTile(local));
                sleep(300, 2000);
            } else {
                if (brokenStrut.interact("Hammer")) {
                    log("Fixing Broken strut");
                    sleepUntil(() -> GameObjects.all("Broken strut").size() < 2, 8000);
                    sleep(4000, 8000);
                }
            }
        }

    }

    private void buyCoalBag() {
        /*
        if not in pete area
            walk to pete area
        else
            open up shop
            purchase bag
         */
        NPCs.closest("Prospector Percy").interact("Trade");
        if (sleepUntil(Shop::isOpen, 15000)) {
            sleep(500, 1000);
            if (Shop.contains("Coal bag")) {
                Shop.get("Coal bag").interact("Buy 1");
                sleep(500, 1500);
            }
            sleep(500, 1000);
            Shop.close();
        }
    }

    private void bank() {
        bankChest = GameObjects.closest("Bank chest");

        if (bankChest == null || !Map.canReach(bankChest.getTile(), true) || bankChest.distance() > 8) {
            log("Walking to Bank Chest");
            Walking.walk(bankArea.getNearestTile(local));
            sleep(200, 2000);
        } else {
            if (bankChest.interact("Use")) {
                sleepUntil(() -> Bank.isOpen(), 10000);
                sleep(200, 1000);
                Bank.depositAllExcept(DO_NOT_BANK_ITEMS);
                sleep(300, 1200);
                if(shouldBuyHelmet() || shouldBuyJacket() || shouldBuyLegs() || shouldBuyBoots()){
                    Bank.withdrawAll("Golden nugget");
                }
                sleep(500,1500);

                Bank.close();
            }
            sleep(500, 1000);
        }
    }

    private boolean shouldBuyHelmet(){
        return (Bank.count("Golden nugget") >= 40 || Inventory.count("Golden nugget") >= 40) && !Equipment.contains("Prospector helmet") && !Inventory.contains("Prospector helmet");
    }

    private boolean shouldBuyJacket(){
        return (Bank.count("Golden nugget") >= 60 || Inventory.count("Golden nugget") >= 60) && !Equipment.contains("Prospector jacket") && !Inventory.contains("Prospector jacket") && (Equipment.contains("Prospector helmet") || Inventory.contains("Prospector helmet"));
    }

    private boolean shouldBuyLegs(){
        return (Bank.count("Golden nugget") >= 50 || Inventory.count("Golden nugget") >= 50) && (Equipment.contains("Prospector jacket") || Inventory.contains("Prospector jacket")) && !Inventory.contains("Prospector legs") && !Equipment.contains("Prospector legs");
    }

    private boolean shouldBuyBoots(){
        return (Bank.count("Golden nugget") >= 30 || Inventory.count("Golden nugget") >= 30) && (Equipment.contains("Prospector legs") || Inventory.contains("Prospector legs")) && !Inventory.contains("Prospector legs") && !Equipment.contains("Prospector legs");
    }


    private void getHammer() {

        if (!crateTile.getArea(4).contains(local)) {
            log("Getting a hammer");
            Walking.walk(crateTile.getArea(4).getRandomTile());
        } else {
            if (Inventory.isFull()) {
                Inventory.get(i -> !i.getName().contains("pickaxe") || !i.getName().contains("Coins")).interact("Drop");
                sleep(1200, 2000);
            }
            GameObjects.getTopObjectOnTile(crateTile).interact("Search");
            sleep(1500, 3000);

            sleepUntil(() -> Inventory.contains("Hammer"), 6000);
        }
        sleep(900, 1500);
    }


    private Area getMiningArea() {
        switch (miningArea) {
            case (1):
                return westMiningArea;
            case (2):
                return southWestMiningArea;
            case (3):
                return southEastMiningArea;
        }
        return southWestMiningArea;
    }

    private boolean hasRewards() {
        return Inventory.contains("Golden nugget") || Inventory.contains("Coal") || Inventory.contains("Gold ore") || Inventory.contains("Mithril ore") || Inventory.contains("Adamantite ore");
    }

    private void hopWorlds() {
        log("Hopping world");
        randWorld = Worlds.getRandomWorld();

        if (randWorld.isDeadmanMode() || randWorld.isF2P() || randWorld.isPVP() || randWorld.isHighRisk() || randWorld.isTournamentWorld() || randWorld.getMinimumLevel() > 100) {
            randWorld = Worlds.getRandomWorld();

        } else if (randWorld.isMembers() && randWorld.isNormal()) {
            log("Hopping to " + randWorld.getWorld());
            WorldHopper.hopWorld(randWorld);
            sleep(10000, 15000);
        }
    }

    private void walkToMotherlodeMine() {
        log("Walking to Motherlode Mine");
        if (!dwarfMine.contains(local)) {
            if (!falador.contains(local)) {
                Walking.walk(falador.getCenter());
                sleep(900, 1400);
            } else {
                if (!house.contains(local)) {
                    Walking.walk(house.getCenter());
                    sleep(900, 1500);
                } else {
                    sleep(1500, 3000);
                    GameObjects.closest("Staircase").interact("Climb-down");
                    sleepUntil(() -> dwarfMine.contains(local), 6000);
                }
            }
        } else {
            log("Entering Motherlode Mine");
            GameObjects.closest("Cave").interact("Enter");
            sleepUntil(() -> motherlodeMine.contains(local), 10000);
        }
    }


}
